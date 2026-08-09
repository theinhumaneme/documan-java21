# Documan API — v1

Generated from [`openapi.json`](openapi.json), which is itself generated from the
controller signatures. Do not edit by hand: run `scripts/generate_api_md.py`.

## Base URL

- `/` — Same origin as the caller
- `http://localhost:8080` — Local development

Every path below is relative to that, and already includes the `/api/v1` prefix.

## Authentication

A Clerk session token, sent as `Authorization: Bearer <token>`.

The service verifies it against Clerk's published signing keys and issues nothing itself, so there is no login endpoint here. A browser gets one from the Clerk SDK; anything else needs one minted for it.

Ask for the JWT template the deployment expects (`documan` by default) rather than the bare session token — the default carries no email, and without one the service cannot provision a row for a reader it has not seen before. Templates default to a 60 second lifetime, which is short for anything scripted.

```http
GET /api/v1/user/me HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Reading needs no token.** Every `GET` is public except those under `/user`, which
return the directory rather than material. Everything that changes something needs one.

## Roles

`regular` < `maintainer` < `moderator` < `admin`, and every check asks *at least this
rank* — so an administrator is implicitly a moderator too. A maintainer's rights are
scoped to granted departments, years and semesters rather than to the library as a whole,
which is why some rules below name a resource rather than a rank.

## Conventions

- **Identifiers are query parameters**, not path segments: `?subjectId=12`.
- **Collections return a `PageResponse`** — `content`, `page`, `size`, `totalElements`,
  `totalPages`, `first`, `last` — except the reference lookups, which return plain arrays.
- **Paging** accepts `page`, `size` and `sort`. Sort by a non-unique column and pages can
  overlap or skip; `sort=id,asc` is the safe choice when walking a whole collection.
- **Errors are RFC 9457 problem documents** (`application/problem+json`).

### Status codes

| Code | Meaning |
| --- | --- |
| `400` | Validation failed, or a rule spanning several fields was broken |
| `401` | No token, or one that does not verify |
| `403` | A valid token belonging to somebody not allowed to do this |
| `404` | No such row |
| `409` | Conflict — a duplicate, or a concurrent modification |
| `502` | Object storage failed |
| `503` | Search is unavailable; carries `Retry-After` |

## Endpoints

### Reference data

| Method | Path | Required parameters | Who may call it |
| --- | --- | --- | --- |
| `GET` | `/api/v1/department/all` | — | public |
| `GET` | `/api/v1/role` | `roleId` | public |
| `GET` | `/api/v1/role/all` | — | public |
| `PUT` | `/api/v1/role/demote` | `userId`, `roleId` | administrators only. |
| `PUT` | `/api/v1/role/promote` | `userId`, `roleId` | administrators only. |
| `GET` | `/api/v1/role/user` | `userId` | public |
| `GET` | `/api/v1/semester/all` | — | public |
| `GET` | `/api/v1/year/all` | — | public |

### Library

| Method | Path | Required parameters | Who may call it |
| --- | --- | --- | --- |
| `DELETE` | `/api/v1/file` | `objectUID` | moderators and above, or a maintainer whose grant covers the file's subject. |
| `POST` | `/api/v1/file` | `folderId` | moderators and above, or a maintainer whose grant covers the folder's subject. |
| `GET` | `/api/v1/file/download` | `fileId` | public |
| `GET` | `/api/v1/file/folder` | `folderId` | public |
| `PATCH` | `/api/v1/file/move` | — | requires the right to both ends of the move — every file's current subject and the destination folder's. |
| `PATCH` | `/api/v1/file/rename` | `fileId` | moderators and above, or a maintainer whose grant covers the file's subject. |
| `GET` | `/api/v1/file/subject` | `subjectId` | public |
| `DELETE` | `/api/v1/folder` | `folderId` | moderators and above, or a maintainer whose grant covers the folder's subject. |
| `GET` | `/api/v1/folder` | `subjectId` | public |
| `POST` | `/api/v1/folder` | `subjectId` | moderators and above, or a maintainer whose grant covers this subject's department, year and semester. |
| `PUT` | `/api/v1/folder` | `folderId` | moderators and above, or a maintainer whose grant covers the folder's subject. |
| `DELETE` | `/api/v1/subject` | `subjectId` | moderators and above, or a maintainer whose grant covers this subject's department, year and semester. |
| `GET` | `/api/v1/subject` | `subjectId` | public |
| `POST` | `/api/v1/subject` | — | moderators and above, or a maintainer whose grant covers the department, year and semester in the request body. |
| `PUT` | `/api/v1/subject` | `subjectId` | requires the right to both addresses, because an update may move the subject — where it sits now, and where the body says it should go. |
| `GET` | `/api/v1/subject/all` | — | public |
| `GET` | `/api/v1/subject/semester` | `departmentId`, `yearId`, `semesterId` | public |

### Search

| Method | Path | Required parameters | Who may call it |
| --- | --- | --- | --- |
| `GET` | `/api/v1/search/files` | — | public |

### Blog

| Method | Path | Required parameters | Who may call it |
| --- | --- | --- | --- |
| `DELETE` | `/api/v1/comment` | `commentId` | the comment's author, or any moderator. |
| `GET` | `/api/v1/comment` | `commentId` | public |
| `POST` | `/api/v1/comment` | `userId`, `postId` | the token's owner, who must have commenting granted. |
| `PUT` | `/api/v1/comment` | `commentId` | the comment's author, or any moderator. |
| `GET` | `/api/v1/comment/all` | — | public |
| `GET` | `/api/v1/comment/post` | `postId` | public |
| `GET` | `/api/v1/comment/user` | `userId` | public |
| `POST` | `/api/v1/comment/vote` | `voteType`, `commentId`, `userId` | the token's owner only — `userId` may not name anybody else. |
| `POST` | `/api/v1/comment/vote/remove` | `voteType`, `commentId`, `userId` | the token's owner only — `userId` may not name anybody else. |
| `DELETE` | `/api/v1/post` | `postId` | the post's author, or any moderator. |
| `GET` | `/api/v1/post` | `postId` | public |
| `POST` | `/api/v1/post` | `userId` | the token's owner, who must have posting granted. Setting `announcement` additionally requires an administrator. |
| `PUT` | `/api/v1/post` | `postId` | the post's author, or any moderator. Changing `announcement` requires an administrator; resubmitting it unchanged does not. |
| `GET` | `/api/v1/post/all` | — | public |
| `POST` | `/api/v1/post/favourite` | `postId`, `userId` | the token's owner only — `userId` may not name anybody else. |
| `POST` | `/api/v1/post/favourite/remove` | `postId`, `userId` | the token's owner only — `userId` may not name anybody else. |
| `GET` | `/api/v1/post/user` | `userId` | public |
| `POST` | `/api/v1/post/vote` | `voteType`, `postId`, `userId` | the token's owner only — `userId` may not name anybody else. |
| `POST` | `/api/v1/post/vote/remove` | `voteType`, `postId`, `userId` | the token's owner only — `userId` may not name anybody else. |

### Accounts

| Method | Path | Required parameters | Who may call it |
| --- | --- | --- | --- |
| `DELETE` | `/api/v1/user` | `userId` | administrators, and not yourself. |
| `GET` | `/api/v1/user` | `userId` | yourself, or any moderator. |
| `POST` | `/api/v1/user` | — | administrators only. |
| `PUT` | `/api/v1/user` | `userId` | yourself, or any administrator. |
| `GET` | `/api/v1/user/all` | — | moderators and administrators. |
| `GET` | `/api/v1/user/comments` | `userId` | any signed-in reader. |
| `GET` | `/api/v1/user/favourites/files` | `userId` | yourself, or any moderator. |
| `POST` | `/api/v1/user/favourites/files` | `fileId`, `userId` | the token's owner only — `userId` may not name anybody else. |
| `POST` | `/api/v1/user/favourites/files/remove` | `fileId`, `userId` | the token's owner only — `userId` may not name anybody else. |
| `GET` | `/api/v1/user/favourites/posts` | `userId` | yourself, or any moderator. |
| `GET` | `/api/v1/user/me` | — | any signed-in reader. |
| `PUT` | `/api/v1/user/me/profile` | — | any signed-in reader. |
| `PUT` | `/api/v1/user/permissions` | `userId` | moderators and administrators. |
| `GET` | `/api/v1/user/posts` | `userId` | any signed-in reader. |
| `GET` | `/api/v1/user/subjects` | `userId` | any signed-in reader. |
| `GET` | `/api/v1/user/username` | `username` | moderators and administrators. |
| `GET` | `/api/v1/user/votes/comments` | `userId`, `voteType` | yourself, or any moderator. |
| `GET` | `/api/v1/user/votes/posts` | `userId`, `voteType` | yourself, or any moderator. |

### Maintainer scopes

| Method | Path | Required parameters | Who may call it |
| --- | --- | --- | --- |
| `DELETE` | `/api/v1/maintainer-scope` | `scopeId` | administrators only. |
| `GET` | `/api/v1/maintainer-scope` | `userId` | yourself, or any moderator. |
| `POST` | `/api/v1/maintainer-scope` | — | administrators only. |
| `GET` | `/api/v1/maintainer-scope/may-edit` | `userId`, `departmentId` | yourself, or any moderator. |

---

67 operations, 46 of which require a token. 21 are public reads.

