-- The four roles.
--
-- Insertion order is not privilege order and nothing may read it as one. `maintainer` ranks between
-- `regular` and `moderator` but is appended last, because these ids come from an identity column and
-- renumbering them would rewrite every documan_user.role_id in every existing database. Rank lives
-- in RoleName, keyed by the name below; see that enum for what each role is for.
INSERT INTO role(name)
VALUES ('regular');
INSERT INTO role(name)
VALUES ('moderator');
INSERT INTO role(name)
VALUES ('admin');
INSERT INTO role(name)
VALUES ('maintainer');
