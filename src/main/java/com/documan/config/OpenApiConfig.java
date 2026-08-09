// MIT License
//
// Copyright (C) 2024 Kalyan Mudumby / @theinhumaneme
//
// Permission is granted to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the software.
package com.documan.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

/**
 * Document-level metadata for the generated OpenAPI spec.
 *
 * <p>Only the parts springdoc cannot infer live here: a title, a version, a licence and the servers
 * the document is valid against. Paths, parameters and every request and response schema are read
 * off the controller signatures at runtime, which is the whole point — a spec assembled by hand
 * drifts the first time someone changes a return type and forgets to update it.
 *
 * <p>That guarantee is only as good as the signatures. A handler returning {@code
 * ResponseEntity<?>} documents nothing, because a wildcard has no schema, and springdoc will emit
 * an empty response body for it without complaining. The concrete DTO return types on the
 * controllers are therefore load bearing: they <em>are</em> the documentation, and both the
 * committed {@code openapi.json} and the TypeScript the frontend compiles against inherit whatever
 * accuracy they have.
 *
 * <p>The version is the API's contract version — the {@code v1} in {@code /api/v1} — not the Maven
 * artifact version. A patch release does not change the contract, so tying the two together would
 * churn the committed spec on every build for no reader benefit.
 */
@Configuration
public class OpenApiConfig {

  /** The name the security scheme is registered and referenced under. */
  private static final String BEARER = "bearerAuth";

  @Bean
  public OpenAPI documanOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Documan API")
                .description(
                    """
                    Departments, years, semesters, subjects and the files filed under them, \
                    plus the student blog.

                    Two conventions run through everything. Identifiers are query parameters \
                    rather than path segments — `?subjectId=12`, not `/subject/12`. And \
                    collection endpoints return a `PageResponse` envelope, while the three \
                    reference lookups (`/department/all`, `/year/all`, `/semester/all`) return \
                    plain arrays, because a fixed handful of rows is not worth paging.

                    Errors are RFC 9457 problem documents, produced centrally by \
                    `GlobalExceptionHandler`.

                    This document is generated from the controller signatures and is not edited \
                    by hand. `./extract-openapi-json.sh` regenerates the committed copy.\
                    """)
                .version("v1")
                .license(
                    new License()
                        .name("MIT")
                        .url("https://github.com/theinhumaneme/documan-java21/blob/main/LICENSE")))
        .servers(
            List.of(
                new Server().url("/").description("Same origin as the caller"),
                new Server().url("http://localhost:8080").description("Local development")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            """
                            A Clerk session token, sent as `Authorization: Bearer <token>`.

                            The service verifies it against Clerk's published signing keys and \
                            issues nothing itself, so there is no login endpoint here. A browser \
                            gets one from the Clerk SDK; anything else needs one minted for it.

                            Ask for the JWT template the deployment expects (`documan` by \
                            default) rather than the bare session token — the default carries no \
                            email, and without one the service cannot provision a row for a \
                            reader it has not seen before. Templates default to a 60 second \
                            lifetime, which is short for anything scripted.\
                            """)));
  }

  /**
   * Writes each endpoint's authorisation rule into the document, from the annotation itself.
   *
   * <p>springdoc reads signatures, and {@code @PreAuthorize} is not part of one — so without this
   * the generated spec described 67 operations and mentioned authentication nowhere at all. A reader
   * could not tell that promoting a user needs an administrator, or that uploading a file needs a
   * maintainer whose grant covers the folder. That is most of what somebody integrating needs to
   * know, and it was the one thing missing.
   *
   * <p>Derived rather than written down. A prose table beside the code would be wrong the first time
   * a rule changed and nobody remembered the table; this reads the annotation that is actually
   * enforced, so the two cannot disagree. The expression is emitted verbatim underneath the
   * translation for the same reason — the summary is a courtesy, the expression is the truth.
   */
  @Bean
  public OperationCustomizer documentAuthorisation() {
    return (operation, handlerMethod) -> {
      PreAuthorize rule = handlerMethod.getMethodAnnotation(PreAuthorize.class);
      boolean readOnly = isRead(operation, handlerMethod);

      if (rule == null && readOnly && !closedByChain(handlerMethod)) {
        // Public: a GET with no rule, outside the paths the chain closes wholesale. Nothing to say
        // and no credential to advertise.
        return operation;
      }

      operation.addSecurityItem(new SecurityRequirement().addList(BEARER));
      String note =
          rule != null
              ? "**Authorisation:** " + explain(rule.value()) + "\n\n`" + rule.value() + "`"
              : "**Authorisation:** any signed-in reader.";
      operation.setDescription(
          operation.getDescription() == null || operation.getDescription().isBlank()
              ? note
              : operation.getDescription() + "\n\n" + note);
      return operation;
    };
  }

  private static boolean isRead(Operation operation, HandlerMethod handlerMethod) {
    return handlerMethod.hasMethodAnnotation(
        org.springframework.web.bind.annotation.GetMapping.class);
  }

  /**
   * Whether the filter chain requires a token regardless of what the method says.
   *
   * <p>Authorisation is not only the annotations. {@code WebSecurityConfig} closes every method under
   * {@code /api/v1/user/**} because those responses are the directory — names, addresses, each
   * reader's own votes and favourites — rather than material. A reader of the generated document has
   * no way to know that, and without this the spec described {@code GET /user/me} as public, which is
   * nonsense on its face and was caught by handing the document to someone with no other context.
   *
   * <p>Matched on the controller rather than the path because that is what a {@code HandlerMethod}
   * knows, and {@link com.documan.controllers.UserController} is exactly the {@code /api/v1/user}
   * mapping. If the chain's rule and this one ever drift, the spec is the thing that goes wrong —
   * hence the cross-reference in both directions.
   */
  private static boolean closedByChain(HandlerMethod handlerMethod) {
    return handlerMethod.getBeanType().equals(com.documan.controllers.UserController.class);
  }

  /**
   * The common expressions in prose. Anything unrecognised falls through to the expression itself,
   * which is worse to read and still correct — the failure mode of a new rule is a spec that is
   * terse, not a spec that lies.
   */
  private static String explain(String expression) {
    return switch (expression) {
      case "@permissions.isAdmin()" -> "administrators only.";
      case "@permissions.isModerator()" -> "moderators and administrators.";
      case "@permissions.isSelf(#userId)" ->
          "the token's owner only — `userId` may not name anybody else.";
      case "@permissions.isSelfOrAdmin(#userId)" -> "yourself, or any administrator.";
      case "@permissions.isSelfOrModerator(#userId)" -> "yourself, or any moderator.";
      case "@permissions.mayEditSubject(#subjectId)" ->
          "moderators and above, or a maintainer whose grant covers this subject's"
              + " department, year and semester.";
      case "@permissions.mayEditFolder(#folderId)" ->
          "moderators and above, or a maintainer whose grant covers the folder's subject.";
      case "@permissions.mayEditFile(#fileId)", "@permissions.mayEditObject(#objectUID)" ->
          "moderators and above, or a maintainer whose grant covers the file's subject.";
      case "@permissions.mayMoveFiles(#request.fileIds(), #request.folderId())" ->
          "requires the right to both ends of the move — every file's current subject and the"
              + " destination folder's.";
      case "@permissions.mayEditPost(#postId)" -> "the post's author, or any moderator.";
      case "@permissions.mayEditComment(#commentId)" -> "the comment's author, or any moderator.";
      case "@permissions.mayEditAddress(#request.departmentId(), #request.yearId(),"
              + " #request.semesterId())" ->
          "moderators and above, or a maintainer whose grant covers the department, year and"
              + " semester in the request body.";
      case "@permissions.mayEditSubject(#subjectId) and @permissions.mayEditAddress("
              + "#request.departmentId(), #request.yearId(), #request.semesterId())" ->
          "requires the right to both addresses, because an update may move the subject — where"
              + " it sits now, and where the body says it should go.";
      case "@permissions.isSelf(#userId) and @permissions.mayPost()"
              + " and (!#request.announcement() or @permissions.mayAnnounce())" ->
          "the token's owner, who must have posting granted. Setting `announcement` additionally"
              + " requires an administrator.";
      case "@permissions.isSelf(#userId) and @permissions.mayComment()" ->
          "the token's owner, who must have commenting granted.";
      case "@permissions.mayEditPost(#postId)"
              + " and @permissions.mayAnnounceOn(#postId, #request.announcement())" ->
          "the post's author, or any moderator. Changing `announcement` requires an administrator;"
              + " resubmitting it unchanged does not.";
      case "@permissions.isAdmin() and !@permissions.isSelf(#userId)" ->
          "administrators, and not yourself.";
      case "@permissions.mayPost()" -> "any signed-in reader with posting granted.";
      case "@permissions.mayComment()" -> "any signed-in reader with commenting granted.";
      default -> expression;
    };
  }
}
