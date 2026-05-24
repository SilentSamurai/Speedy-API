---
inclusion: manual
---

# Speedy-API — Issues & Improvement Opportunities

Audit based on codebase analysis as of v3.1.4.

---

## Bugs

### 1. `$pageNo` URL parameter is ignored — calls `addPageSize()` instead of `addPageNo()`

File: `speedy-core/.../parser/SpeedyUriContext.java`, method `extractPageInfo()`

```java
// BUG: should be speedyQuery.addPageNo(pageNo)
speedyQuery.addPageSize(pageNo);
```

The `$pageNo` query parameter is parsed correctly but then passed to `addPageSize()` instead of `addPageNo()`. This means `?$pageNo=2` overwrites the page size rather than setting the page number. Pagination via URL is effectively broken for anything beyond page 0.

### 2. `GetHandler` parses the URI twice

`EntityCaptureHandler` already parses the URI via `SpeedyUriContext` to extract the entity metadata, but `GetHandler` creates a second `SpeedyUriContext` and parses the same URI again. This is redundant work and means any query parameters parsed in `EntityCaptureHandler` are discarded — the `SpeedyQuery` from the first parse is never stored on the `RequestContext`.

### 3. `GetHandler` force-expands all associations

```java
Set<String> allAssociations = speedyQuery.getFrom()
    .getAssociatedFields().stream().map(...)
    .collect(Collectors.toSet());
speedyImpl.setExpand(allAssociations);
```

Every GET request automatically expands all associations regardless of what the client requested. For entities with many relationships, this causes unnecessary joins and data transfer. The user's `$expand` parameter (if any) is overwritten.

### 4. `UpdateHandler` wraps all exceptions as `InternalServerError`

```java
} catch (Exception e) {
    throw new InternalServerError("Update failed", e);
}
```

If a `BadRequestException` is thrown during event processing or validation, it gets wrapped in a 500 `InternalServerError` instead of propagating the original 400 status. The `DeleteHandler` correctly handles this with a separate `catch (SpeedyHttpException e) { throw e; }` block, but `UpdateHandler` does not.

### 5. `SpeedyQueryHandler` is a no-op

`SpeedyQueryHandler` exists in the handlers package but does nothing — it just calls `next.process(context)`. It's not used in the handler chain built by `SpeedyFactory`. Dead code.

---

## Security Concerns

### 6. Authentication/authorization is the consuming server's responsibility

By design, Speedy-API is a library — authentication and authorization are handled by the consuming application via middleware (e.g., Spring Security filters) that runs before requests reach `/speedy/v1/**`. The `@SpeedyAction` annotation provides static per-entity CRUD gating in `SwitchHandler`, but per-user access control is outside the library's scope.

**Recommendation:** Document this clearly in the getting-started guide with a Spring Security example showing how to secure Speedy endpoints.

### 7. `/$metadata` endpoint exposes full schema publicly — HIGH

`SpeedyApiController.metadata()` at `/$metadata` is publicly accessible with no auth and returns the complete MetaModel: all entity names, field names, types, nullability, associations, key structure, and whether keys are auto-generated. While DB column names are commented out in `MetaModelSerializer`, the exposed information is sufficient to map the entire data model.

**Recommendation:** Make `/$metadata` opt-in or secured by default.

### 8. Error responses may leak internal details

`ExceptionUtils.writeException()` returns `e.getLocalizedMessage()` directly to the client. For wrapped exceptions (e.g., jOOQ `DataException`), this can expose SQL column names, table names, and constraint details. The `processReqV2` catch for generic `Exception` doesn't write any body at all — just sets the status code, leaving the client with no structured error response.

### 9. No rate limiting or request size limits — HIGH

There's no max body size check in `RequestParserHandler` — it reads the full request body into a `JsonNode` on every request (including GETs). A malicious client could send a very large JSON array to `$create` or `$delete` endpoints, potentially causing OOM.

The `$pageSize` parameter has no upper bound — `addPageSize()` only checks `pageSize > 0`. A client can request `?$pageSize=999999999` and dump the entire table.

**Recommendation:** Add a configurable max page size enforced in `SpeedyQueryImpl.addPageSize()`. Add a max request body size via Spring config or a custom filter.

### 10. `GetHandler` auto-expands all associations — data leakage risk

Every GET request automatically expands ALL associations regardless of what the client requested (see bug #3). Beyond the performance impact, this is a data exposure risk: if entity A has a FK to a sensitive entity B (e.g., User → Role, Order → Customer), a simple GET on A returns B's data. Combined with the lack of auth (#6), any anonymous client can traverse the entire data graph.

**Recommendation:** Make expansion opt-in on GET requests. Only expand associations explicitly requested via `$expand`.

### 11. No field-level access control beyond `@SpeedyIgnore`

The only mechanism to hide fields is `@SpeedyIgnore` (compile-time exclusion from MetaModel). There's no runtime field-level visibility based on user role, request context, or any other dynamic criteria. `$select` controls which fields appear in the response, but any user can select any field.

### 12. CI/CD uses outdated `actions/checkout@v2`

All 3 GitHub workflows (`main.yml`, `release.yml`, `verify.yml`) use `actions/checkout@v2`, which runs on Node.js 12 (EOL). Should be updated to `@v4`.

### 13. `.gitignore` incomplete — missing common sensitive file patterns

The `.gitignore` only covers IDE and build artifacts. The following patterns are missing and should be added:

```
.env
.env.*
*.key
secrets/
keys/
application-secret*
*secret*.yml
*secret*.properties
```

The `keys/` directory exists (currently empty) but is not listed — if GPG keys, certificates, or other secrets are accidentally placed there, they will be committed. Same risk applies to `.env` files and any `*secret*` config files.

### 14. `release.yml` uses `git add .` — risk of committing untracked files

The release workflow does `git add .` before committing version bumps. If any untracked files exist in the CI workspace, they'll be committed to the release branch.

### 15. Spring Boot 3.3.0 not on latest patch

Spring Boot 3.3.0 was released May 2024. Multiple patch releases have been published since with security fixes. Should update to the latest 3.3.x.

### 16. SQL injection risk is well-mitigated — INFO

jOOQ DSL is used throughout with `DSL.value()` for parameterized values. Table/column names come from MetaModel (startup-time JPA introspection), not user input. Field names from user input are validated against the MetaModel via `entityMetadata.getField()` which throws `NotFoundException`. No raw SQL string construction found.

### 17. PostgreSQL JDBC driver pinned to vulnerable version 42.2.25 — CRITICAL

File: `speedy-test-app/pom.xml:77-81`

The `<version>42.2.25</version>` is explicitly pinned in the test app, overriding the Spring Boot BOM-managed version (42.7.3). This version is affected by:

| CVE | CVSS | Details |
|-----|------|---------|
| CVE-2024-1597 | 8.5 | SQL injection via JDBC URL — fixed in 42.7.2 |
| CVE-2022-31197 | 7.5 | SQL injection via Timestamp — fixed in 42.4.1 |
| CVE-2022-21724 | 8.1 | Improper input validation — fixed in 42.3.3 |

**Recommendation:** Remove the explicit `<version>` tag so the Spring Boot parent BOM manages it at 42.7.3+.

### 18. LIKE wildcard `_` not escaped in `$matches` operator — HIGH

File: `speedy-core/.../query/jooq/JooqQueryBuilder.java:82-86`

```java
String rawValue = speedyValue.asText()
    .replaceAll("%", "\\\\%")
    .replaceAll("\\*", "%");
return path.like(DSL.value(rawValue)).escape('\\');
```

The code escapes `%` and `*` but not the SQL single-character wildcard `_`. An attacker searching for a literal underscore (e.g., product code `PART_123`) would also match `PARTX123`, `PARTY123`, etc. Since jOOQ uses bind parameters this is not classic SQLi, but it causes **data leakage** — queries return more records than intended.

**Recommendation:** Add `.replaceAll("_", "\\\\_")` before the LIKE call.

### 19. `$orderBy` accepts arbitrary field names without metadata validation — HIGH

File: `speedy-core/.../parser/SpeedyUriContext.java:184-207`

The `$orderBy` and `$orderByDesc` URL parameters take comma-separated field names from user input and pass them directly to `speedyQuery.orderByAsc(field)` / `speedyQuery.orderByDesc(field)`. No metamodel lookup is performed. While jOOQ quotes these as identifiers (safe from classic SQLi), an attacker could:

- Reference columns from other tables via dot notation (`otherTable.secretColumn`)
- Reference non-existent columns causing database errors (info disclosure)
- Reference system columns that jOOQ may not fully sanitize

**Recommendation:** Validate each order-by field against the entity's metadata before adding to the query.

### 20. `$`-prefixed value expressions bypass type compatibility checks — HIGH

File: `speedy-core/.../parser/SpeedyUriContext.java:38-46`

The `$` prefix in filter values allows field-to-field comparisons, e.g., `?password=$email` compares the `password` field's value against the `email` field. The referenced field is validated to exist in the entity model, but **no type compatibility check** is performed between the source and target fields. A user can compare any two fields regardless of type, including sensitive fields like password hashes, salary, or internal flags.

**Recommendation:** Add type compatibility validation when resolving `$`-prefixed value expressions. Restrict which fields can be referenced in field-to-field comparisons.

### 21. GPG signing key passed unsafely in CI workflows — HIGH

Files: `.github/workflows/verify.yml:67`, `.github/workflows/release.yml:39`

```bash
cat <(echo -e "${{ secrets.GPG_SIGNING_KEY }}") | gpg --batch --import
```

Piping the full private GPG signing key through `echo -e` exposes the key material to process listing on the runner. While GitHub masks secrets in logs, the key can be visible via `/proc/` or other process introspection tools during execution.

**Recommendation:** Write the key to a file from a GitHub Actions environment variable instead. Use `gpg --import` with a file input.

### 22. H2 web console enabled with empty password — HIGH

Files:
- `speedy-test-app/.../application.properties:3,5,7`
- `speedy-test-app/.../application-mysql.properties:3`

The H2 console is enabled (`spring.h2.console.enabled=true`) with an empty datasource password and `DB_CLOSE_DELAY=-1`. An attacker who reaches `/h2-console` can execute arbitrary SQL against the in-memory database. Additionally, `spring.h2.console.enabled=true` is set in the **MySQL profile** (`application-mysql.properties`), which is a misconfiguration — the MySQL profile doesn't use H2 as its datasource.

**Recommendation:** Disable H2 console in all production profiles. For the MySQL profile, remove the `spring.h2.console.enabled` property entirely.

### 23. Catch-all `/**` request mapping is overly broad — MEDIUM

File: `speedy-core/.../controllers/SpeedyApiController.java:43`

```java
@RequestMapping(value = {"*", "/**", "*/*"})
```

The catch-all handler accepts any HTTP method on any sub-path under `/speedy/v1`. The `"/**"` matches multiple path segments and `"*/*"` matches arbitrary nested paths. This could shadow other routes or intercept unintended requests.

**Recommendation:** Restrict to specific HTTP methods and path patterns. Document which paths are valid.

### 24. Reflection-based `Method.invoke()` in validation and event processors — MEDIUM

Files:
- `speedy-core/.../validation/ValidationProcessor.java:102`
- `speedy-core/.../events/EventProcessor.java:113,116`

Both processors call `method.invoke(instance, param)` with parameters that include user-supplied entity data (converted via `SpeedySerializer.toJavaEntity()`). While the methods and instances are pre-registered from `ISpeedyCustomValidation` / `ISpeedyEventHandler` implementations (Spring beans, not controllable via HTTP), the pattern is fragile: an exception in the invoked method or a maliciously registered bean could lead to unexpected behavior or arbitrary code execution.

**Recommendation:** Validate handler method signatures at registration time. Use a security manager or sandbox if users can register custom handlers.

### 25. No input size limits on query strings — MEDIUM

File: `speedy-core/.../parser/SpeedyUriContext.java:84-113,148-181`

Query parameters, filter values, and page sizes are not validated for maximum length or count. An attacker could:
- Send extremely long query strings causing parsing overhead and memory pressure
- Send a large number of filter parameters in a single request
- Request `$pageSize` values up to `Integer.MAX_VALUE` (the only check is `pageSize > 0`)

There is also no max body size check in `RequestParserHandler` — it reads the full request body into a `JsonNode` (see issue #9).

**Recommendation:** Add configurable max query string length, max page size, and max filter parameter count.

---

## Architecture & Design

### 26. `QueryProcessor` created per request but `DSLContext` is not lightweight

`CreateQueryProcessorHandler` creates a new `JooqQueryProcessorImpl` (and thus a new `DSLContext`) for every request. While `DSLContext` is relatively cheap, the pattern of `dataSourcePerReq()` suggests the intent was to support per-request DataSource switching (e.g., multi-tenancy). However, in practice most apps return the same DataSource every time, making this overhead unnecessary. Consider caching the `QueryProcessor` when the DataSource hasn't changed.

### 27. `EntityCaptureHandler` parses URI but discards the `SpeedyQuery`

The handler parses the URI to extract entity metadata but doesn't store the resulting `SpeedyQuery` on the `RequestContext`. Downstream handlers (`GetHandler`, `QueryHandler`) then re-parse the URI or body independently. Storing the parsed query on the context would eliminate duplicate parsing.

### 28. No transaction boundary across batch operations

`CreateHandler.processPhysical()` calls `queryProcessor.create(jsonBody)` which inserts all entities, but if a `POST_INSERT` event fails for entity N, entities 1..N-1 are already committed. Similarly, `DeleteHandler` validates and fires `PRE_DELETE` for all keys, then deletes all, then fires `POST_DELETE` — but there's no rollback if `POST_DELETE` fails. The jOOQ operations use individual transactions per entity (in `SpeedyInsertQuery`/`SpeedyUpdateQuery`), not a batch transaction.

### 29. Tight coupling to jOOQ in `speedy-core`

`speedy-core` directly depends on jOOQ and contains the `JooqQueryProcessorImpl`. The `QueryProcessor` interface exists in `speedy-commons`, which is good, but the only implementation lives in `speedy-core` rather than in a separate `speedy-jooq-impl` module. This makes it impossible to swap the query engine without modifying `speedy-core`.

### 30. `SwitchHandler` routes by URL string matching, not structured routing

```java
if (requestURI.contains("$query")) { ... }
else if (requestURI.contains("$create")) { ... }
```

Using `String.contains()` on the URI is fragile. An entity named `$queryHelper` or a field value containing `$create` in the URL could trigger incorrect routing. A more robust approach would be to extract the action from a well-defined path segment.

---

## Performance

### 31. Default page size is 5 — too small for most use cases

`SpeedyConstant.defaultPageSize = 5` means every query without explicit `$pageSize` returns only 5 rows. This is unusually small and will cause excessive round-trips for most real-world usage. Consider raising to 20-50 or making it configurable via `ISpeedyConfiguration`.

### 32. `exists()` check before create/update/delete adds extra round-trip

`CreateHandler` calls `queryProcessor.exists(pk)` before inserting, `UpdateHandler` calls it before updating, and `DeleteHandler` calls it before deleting. Each of these is a separate SQL query. For batch creates with N entities, that's N extra SELECT queries. Consider using database-level constraints (unique/FK) and handling the resulting exceptions instead.

### 33. N+1 query pattern in `JooqQueryProcessorImpl.create()`

After batch insert, the code loops through each entity and does a `findByPrimaryKey()` SELECT to return the saved state. For N entities, that's N additional queries. A single `SELECT ... WHERE id IN (...)` would be more efficient.

### 34. `GetHandler` auto-expanding all associations triggers extra joins/queries

As noted in bug #3, every GET auto-expands all associations. For an entity with 5 foreign keys, this means 5 additional JOINs on every single GET request, even when the client only needs the base entity fields.

---

## Code Quality

### 35. Inconsistent exception handling across handlers

- `CreateHandler`: catches `SpeedyHttpException` separately, re-throws it, wraps others in `InternalServerError`
- `UpdateHandler`: catches only `Exception`, wraps everything in `InternalServerError` (bug #4)
- `DeleteHandler`: catches `SpeedyHttpException` separately, re-throws it, wraps others in `InternalServerError`
- `GetHandler`: no try-catch at all — exceptions propagate to `SpeedyFactory`

There should be a consistent pattern, ideally handled once in a single error-handling handler at the top of the chain.

### 36. `RequestContext` is a mutable god object

`RequestContext` carries everything: config, dialect, metamodel, HTTP request/response, entity metadata, query processor, speedy query, response serializer, URI, method, body. It's set up incrementally by different handlers. This makes it hard to reason about what state is available at each point in the chain. Consider splitting into immutable input context and a mutable processing state.

### 37. Commented-out code and TODOs in production paths

- `SpeedyFactory`: `// private final QueryProcessor queryProcessor;` commented out
- `SpeedyUriContext.processFilters()`: `// TODO check this condition`
- `JSONSerializerV2`: `// basePayload.put("totalPageCount", totalPageCount);` commented out
- `SpeedyEventType`: `// IN_PLACE_OF_INSERT`, `IN_PLACE_OF_UPDATE`, `IN_PLACE_OF_DELETE` commented out
- `CreateHandler`: `// TODO: remove this may b not good to throw exception right after insert`

### 38. `spring-boot-starter-speedy-open-api` module listed in workspace but not in parent POM

The directory `spring-boot-starter-speedy-open-api` exists in the workspace but is not included in the parent POM's `<modules>` list. It's unclear if this is intentional (deprecated module) or an oversight.

---

## Missing Features / Improvements

### 39. No support for PATCH semantics (partial update)

`SwitchHandler` treats PUT and PATCH identically — both route to `UpdateHandler`. True PATCH semantics (only update supplied fields, leave others unchanged) vs PUT (replace entire entity) are not distinguished.

### 40. No response pagination metadata

`JSONSerializerV2` returns `pageIndex` and `pageSize` (which is actually the result count, not the requested page size) but no `totalCount` or `totalPages`. The `totalPageCount` field is commented out. Clients have no way to know how many pages exist without a separate `$query` with `select: ["count"]`.

### 41. No support for field-level select on GET requests

The `$query` endpoint supports `select` to choose specific fields, but GET requests via URL have no `$select` parameter. All fields (plus all associations per bug #3) are always returned.

### 42. No bulk update support

`$create` and `$delete` accept arrays for batch operations, but `$update` only handles a single entity. There's no bulk update endpoint.

### 43. No ETag / conditional request support

No `If-Match`, `If-None-Match`, or `ETag` headers are generated or checked. This means no optimistic concurrency control at the HTTP level and no cache validation support.

### 44. Configurable default page size

The default page size (5) is hardcoded in `SpeedyConstant`. It should be configurable via `ISpeedyConfiguration` or Spring properties (`speedy.api.defaultPageSize`).
