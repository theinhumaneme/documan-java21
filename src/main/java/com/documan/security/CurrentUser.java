// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.security;

import com.documan.dao.RoleDao;
import com.documan.dao.UserDao;
import com.documan.entity.Role;
import com.documan.entity.RoleName;
import com.documan.entity.User;
import com.documan.exception.ResourceNotFoundException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Who is making this request — the one place a token becomes a row.
 *
 * <p>Clerk owns identity but not the data hanging off it: a post has an author, a vote has a voter,
 * and both are foreign keys into {@code documan_user}. So a row has to exist for every person who
 * signs in, and the only moment we learn about them is the first request carrying their token.
 * Hence provisioning here rather than a synchronisation job — there is no list to synchronise from
 * without going back to Clerk's API for it, and a job would provision everyone who ever registered
 * to serve the few who actually read anything.
 *
 * <p>This replaces {@code ?userId=}. While that parameter still exists a caller can name any user
 * they like, so the two disagree by design until it is removed; nothing should read both.
 */
@Component
public class CurrentUser {

  private static final Logger log = LoggerFactory.getLogger(CurrentUser.class);

  /** New accounts always start on the lowest-privilege role, as self-registration did. */
  private static final RoleName DEFAULT_ROLE = RoleName.REGULAR;

  private final UserDao userDao;
  private final RoleDao roleDao;
  private final UserProvisioner provisioner;

  public CurrentUser(UserDao userDao, RoleDao roleDao, UserProvisioner provisioner) {
    this.userDao = userDao;
    this.roleDao = roleDao;
    this.provisioner = provisioner;
  }

  /** The validated token, or empty for an anonymous request — every GET may be one. */
  public Optional<Jwt> token() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwt) {
      return Optional.of(jwt.getToken());
    }
    return Optional.empty();
  }

  /** The signed-in reader's row, provisioning it if this is their first request. */
  @Transactional
  public Optional<User> find() {
    return token().map(this::resolve);
  }

  /**
   * The signed-in reader's row, or a 401.
   *
   * <p>For endpoints that cannot mean anything anonymously. The filter chain already rejects an
   * unauthenticated write, so reaching the throw means either a GET that needs an identity or a
   * rule here and in the chain that have drifted apart.
   */
  @Transactional
  public User require() {
    return find()
        .orElseThrow(
            () ->
                new AuthenticationCredentialsNotFoundException(
                    "This request needs a signed-in user"));
  }

  /**
   * The id the service layer still takes. Goes when {@code ?userId=} does.
   *
   * <p>Annotated in its own right because a call from here to {@link #require()} does not pass back
   * through the proxy, so this method's caller is what has to open the transaction that
   * provisioning writes in.
   */
  @Transactional
  public Integer requireId() {
    return require().getId();
  }

  /**
   * Token to row, in three steps: the row we already linked, the row that was theirs before the
   * provider, or a new one.
   *
   * <p>The middle step is what stops the migration losing everything. The database's existing
   * accounts were created by the old sign-up form and know nothing about the provider; matched on
   * email they are claimed by their owner's first token, keeping their posts, votes and role.
   * Without it a seeded administrator would sign in and find themselves a brand-new regular reader.
   *
   * <p>That step used to insist the row had no {@code external_id}, which locked out anyone whose
   * Clerk account was deleted and recreated. They keep their address but arrive with a new {@code
   * sub}: the first lookup misses, the email match was skipped because the row was already linked
   * to the old subject, and provisioning then collided with the unique index on email. The result
   * was a permanent 401-then-409 reported as "the request violates a database constraint", which
   * says nothing a reader or an administrator could act on. An address is proof enough of who they
   * are — the provider verified it before issuing the token — so the row is rebound to the subject
   * now presenting it. See {@link #rebind}.
   *
   * <p><b>The insert races, and has to.</b> A signed-in page load fires several authenticated
   * requests at once — {@code /user/me}, the reader's votes, their favourites — and registration
   * adds {@code /user/me/profile} on top. For someone signing in for the first time every one of
   * those arrives with no row yet, so every one of them looks, finds nothing, and inserts. One wins
   * the unique index on username and the rest fail.
   *
   * <p>This was not theoretical: a fresh database turned the first registration into four
   * violations of {@code idx_user_username}, and the surviving row was id 2 because id 1 had been
   * consumed by a loser. Checking harder before inserting cannot fix it — any gap between the look
   * and the insert is the race. So the insert is allowed to fail, and losing is treated as what it
   * actually means: somebody else has just created the row, go and read it.
   */
  private User resolve(Jwt jwt) {
    String subject = subject(jwt);
    Optional<User> linked = userDao.findByExternalId(subject);
    if (linked.isPresent()) {
      return linked.get();
    }

    String email = email(jwt);
    Optional<User> existing = Optional.ofNullable(email).flatMap(userDao::findByEmail);
    if (existing.isPresent()) {
      return rebind(existing.get(), subject);
    }

    try {
      return provisioner.insert(provision(jwt, subject, email));
    } catch (DataIntegrityViolationException race) {
      // Lost. The winner's row is committed by now, so this is a plain re-read rather than a retry
      // — there is nothing to do again. If it is somehow absent the constraint was about something
      // other than this person, and rethrowing says so rather than inventing an answer.
      return userDao.findByExternalId(subject).orElseThrow(() -> race);
    }
  }

  /**
   * Point a row at the subject now claiming its address.
   *
   * <p>Covers two cases with one rule. A row that predates the provider has no subject and is being
   * linked for the first time; a row whose subject differs belongs to someone whose provider account
   * was replaced. Both are the same person by the only evidence available — a verified address the
   * provider vouched for when it issued this token — and both want the same outcome: keep the row,
   * with its posts, votes, permissions and role, and let them in.
   *
   * <p>What this trusts is the identity provider's verification of the address, so it is only as
   * strong as that. It is the same trust the migration path already placed in it, and the same one
   * the account exists on: an installation whose provider hands out unverified addresses has a
   * bigger problem than this method. The rebind is logged because it is the one moment a row changes
   * hands, and an unexpected one is worth being able to find afterwards.
   */
  private User rebind(User user, String subject) {
    String previous = user.getExternalId();
    if (subject.equals(previous)) {
      return user;
    }
    if (previous != null) {
      log.warn(
          "Rebinding user {} ({}) from subject {} to {} — same verified address, new provider"
              + " account",
          user.getId(),
          user.getEmail(),
          previous,
          subject);
    }
    user.setExternalId(subject);
    return userDao.save(user);
  }

  private User provision(Jwt jwt, String subject, String email) {
    User user = new User();
    user.setExternalId(subject);
    user.setUsername(availableUsername(email, subject));
    user.setEmail(email != null ? email : subject + "@invalid.local");
    user.setFirstName(claimOr(jwt, "given_name", firstWordOfName(jwt, true)));
    user.setLastName(claimOr(jwt, "family_name", firstWordOfName(jwt, false)));
    // Their address is real — the sign-up flow proved it before issuing a token. That is all
    // `verified` ever meant here, and it is not a judgement about the person.
    user.setVerified(true);
    // Reading, and nothing else, until a moderator says otherwise.
    //
    // These were true, on the reasoning that being inside the university's directory was itself the
    // gate and nobody should arrive mute. Self-registration removes that gate: anyone who can
    // receive email can now hold an account, so a default of "may post" would mean the student blog
    // is writable by anyone who signs up, with moderation only ever after the fact.
    //
    // The interface already explains this — "A moderator has not granted you posting yet" — and the
    // admin console grants both in one click, so the cost is a wait rather than a dead end.
    user.setCanPost(false);
    user.setCanComment(false);
    // Set by the registration form, through /user/me/profile, once a session exists.
    user.setAcceptedTermsOfService(false);
    user.setRole(defaultRole());
    // Department, year and semester stay null until they file themselves — see User.
    return user;
  }

  /**
   * A username nobody else holds, derived from the reader's address.
   *
   * <p>The local part is what someone would recognise beside their own posts. Uniqueness is not
   * something the identity provider can promise here — this database predates it, and two people at
   * different providers can both be {@code alice} — so the whole address is the first fallback and
   * the subject, which cannot collide, is the last.
   */
  private String availableUsername(String email, String subject) {
    if (StringUtils.hasText(email)) {
      int at = email.indexOf('@');
      String local = at > 0 ? email.substring(0, at) : email;
      if (StringUtils.hasText(local) && !userDao.existsByUsername(local)) {
        return local;
      }
      if (!userDao.existsByUsername(email)) {
        return email;
      }
    }
    return subject;
  }

  /**
   * Clerk's {@code sub} — its immutable id for the person, and the only claim safe to key a row on.
   * An address is something they can change; the subject is not.
   */
  private static String subject(Jwt jwt) {
    return jwt.getSubject();
  }

  /**
   * The reader's address, which the JWT template is what puts there.
   *
   * <p>Clerk's default session token carries no email at all, so a null here almost always means
   * the template named in the client's `clerk.ts` is missing or does not emit {@code email} — not
   * that the reader has no address.
   */
  private static String email(Jwt jwt) {
    String email = jwt.getClaimAsString("email");
    return StringUtils.hasText(email) ? email : null;
  }

  private static String claimOr(Jwt jwt, String claim, String fallback) {
    String value = jwt.getClaimAsString(claim);
    return StringUtils.hasText(value) ? value : fallback;
  }

  /** {@code name} is a display name — "Kalyan Mudumby". Split when the parts are not sent. */
  private static String firstWordOfName(Jwt jwt, boolean first) {
    String name = jwt.getClaimAsString("name");
    if (!StringUtils.hasText(name)) {
      return first ? "Unknown" : "User";
    }
    String[] parts = name.trim().split("\\s+", 2);
    if (first) {
      return parts[0];
    }
    return parts.length > 1 ? parts[1] : "";
  }

  private Role defaultRole() {
    return roleDao
        .findByName(DEFAULT_ROLE.value())
        .orElseThrow(
            () -> new ResourceNotFoundException("Role '%s'".formatted(DEFAULT_ROLE.value())));
  }
}
