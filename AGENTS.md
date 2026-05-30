# AGENTS.md

<!-- SPECKIT START -->
**Completed Feature**:
`001-between-isnull-operators` — [Spec](./specs/001-between-isnull-operators/spec.md) | [Plan](./specs/001-between-isnull-operators/plan.md) | [Research](./specs/001-between-isnull-operators/research.md) | [Data Model](./specs/001-between-isnull-operators/data-model.md) | [Quickstart](./specs/001-between-isnull-operators/quickstart.md)
<!-- SPECKIT END -->

## Architecture

### Multi-Module Maven Project

| Module                           | Layer              | Purpose                                                                                                                                                                              |
|----------------------------------|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `speedy-commons`                 | Shared Library     | Interfaces, enums, `SpeedyValue` types, `SpeedyQuery`/`Condition` model, annotations, metadata builders, serializers, validation rules. Zero dependencies beyond Spring Boot parent. |
| `speedy-core`                    | Core Engine        | Handler chain, JOOQ-based query execution, URI/JSON parsing, response serialization, event/validation processing, `SpeedyApiController`.                                             |
| `antlr-parser`                   | Parser (Legacy)    | ANTLR4 grammar for a URL DSL. Compiled but runtime URI parsing uses `SpeedyUriContext` instead.                                                                                      |
| `speedy-jpa-impl`                | JPA Bridge         | `JpaMetaModelProcessorV2` scans `EntityManagerFactory` to build the `MetaModel` from `@Entity` classes.                                                                              |
| `speedy-static-impl`             | Static Bridge      | `FileMetaModelProcessor` builds `MetaModel` from a JSON file.                                                                                                                        |
| `spring-boot-starter-speedy-api` | Auto-Configuration | `SpeedyApiAutoConfiguration` — conditionally creates `SpeedyFactory` and `SpeedyOpenApiCustomizer` beans. Entry point: `META-INF/spring/...AutoConfiguration.imports`.               |
| `speedy-java-client`             | Client SDK         | Fluent Java client (`SpeedyApi`) with typed request builders for GET, query, create, update, delete.                                                                                 |
| `speedy-test-app`                | Integration Tests  | Full Spring Boot app with 19 JPA entities, sample config, event handlers, validators.                                                                                                |
| `jacoco-aggregate`               | Coverage           | Aggregates JaCoCo reports across all modules.                                                                                                                                        |

### Request Processing Flow

All requests enter through `SpeedyApiController` (`/speedy/v1/**`) and are delegated to `SpeedyFactory.processReqV2()`,
which runs the **handler chain** (Chain of Responsibility):

```
HeadHandler -> RequestParserHandler -> EntityCaptureHandler
  -> SwitchHandler -> [ GetHandler | QueryHandler | CreateHandler | UpdateHandler | DeleteHandler ]
    -> SpeedyResponseWriterHandler -> TailHandler
```

**Handler responsibilities:**

| Handler                       | What it does                                                                                                                                                           |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `HeadHandler`                 | Entry; passthrough decorator.                                                                                                                                          |
| `RequestParserHandler`        | Reads `HttpMethod`, request URI, and JSON body from `HttpServletRequest`.                                                                                              |
| `EntityCaptureHandler`        | Parses URI via `SpeedyUriContext` to resolve `EntityMetadata` from `MetaModel`.                                                                                        |
| `SwitchHandler`               | Routes by HTTP method + URI suffix (`$query`, `$create`, `$update`, `$delete`). Checks action permissions via `EntityMetadata.is{Read/Create/Update/Delete}Allowed()`. |
| `GetHandler`                  | Parses URI query params into `SpeedyQuery`, executes `executeMany()`, sets `JSONSerializerV2`.                                                                         |
| `QueryHandler`                | Parses POST JSON body (`$from`, `$where`, `$orderBy`, `$page`, `$expand`, `$select`) into `SpeedyQuery`, supports count queries.                                       |
| `CreateHandler`               | Parses JSON array body, fires PRE/POST_INSERT events, validates, bulk creates.                                                                                         |
| `UpdateHandler`               | Parses PK + fields from JSON body, fires PRE/POST_UPDATE events, validates, updates.                                                                                   |
| `DeleteHandler`               | Parses JSON array of PKs, fires PRE/POST_DELETE events, validates, bulk deletes.                                                                                       |
| `SpeedyResponseWriterHandler` | Invokes `IResponseSerializerV2.write()` to serialize the response.                                                                                                     |
| `TailHandler`                 | Terminates the chain (no-op).                                                                                                                                          |

Each handler holds a `final Handler next` reference and calls `next.process(context)` to pass control. The chain is
assembled in `SpeedyFactory.createHandlerChain()`.

### RequestContext (Mutable State Object)

`RequestContext` flows through the entire chain carrying:

- **Immutable (set at construction):** `ISpeedyConfiguration`, `SpeedyDialect`, `MetaModel`, `HttpServletRequest`,
  `HttpServletResponse`, `EventProcessor`, `ValidationProcessor`
- **Mutable (set by handlers):** `EntityMetadata`, `QueryProcessor`, `SpeedyQuery`, `IResponseSerializerV2`,
  `requestUri`, `httpMethod`, `body` (JsonNode)

### SPI / Pluggability

Users integrate by implementing **`ISpeedyConfiguration`**:

- `metaModelProcessor()` — provide `JpaMetaModelProcessorV2` or `FileMetaModelProcessor`
- `register(ISpeedyRegistry)` — register `ISpeedyEventHandler` (lifecycle) and `ISpeedyCustomValidation` (validators)
- `dataSourcePerReq()` — DataSource per request (supports multi-tenancy)
- `getDialect()` — `SpeedyDialect` enum (H2, PostgreSQL, MySQL, etc.)

### SQL Generation

- **`JooqQueryBuilder`** translates the `SpeedyQuery` condition tree into JOOQ `Condition` objects with automatic JOINs
  for associations.
- **`JooqPkQueryBuilder`** — primary key lookups
- **`SpeedyInsertQuery`** — batch inserts (PostgreSQL temp table workaround for generated keys)
- **`SpeedyUpdateQuery`** — single-entity updates
- **`SpeedyDeleteQuery`** — batch deletes
- **`JooqSqlToSpeedy`** — maps JOOQ `Record` results back to `SpeedyEntity`

### REST API Endpoints

Base path: `/speedy/v1/{EntityName}`

| Method        | URI                 | Purpose                                                                                      |
|---------------|---------------------|----------------------------------------------------------------------------------------------|
| `GET`         | `/{Entity}`         | List entities with URL query params                                                          |
| `GET`         | `/$metadata`        | Returns full metamodel as JSON                                                               |
| `POST`        | `/{Entity}/$query`  | Advanced query with JSON body (`$from`, `$where`, `$orderBy`, `$page`, `$expand`, `$select`) |
| `POST`        | `/{Entity}/$create` | Bulk create from JSON array                                                                  |
| `PUT`/`PATCH` | `/{Entity}/$update` | Update single entity by PK in body                                                           |
| `DELETE`      | `/{Entity}/$delete` | Bulk delete by PK array                                                                      |

### Error Handling

Exception hierarchy: `SpeedyHttpException` (checked) with subclasses `BadRequestException` (400), `NotFoundException` (
404), `InternalServerError` (500), and `SpeedyHttpRuntimeException` (unchecked variant).

`SpeedyFactory.processReqV2()` catches `SpeedyHttpException` → `Exception` → `Throwable` at the top level.
`ExceptionUtils` writes JSON error responses (`{"status":..., "message":..., "timestamp":...}`) and maps persistence
exceptions to HTTP status codes.

Validation errors from `DefaultFieldValidator` are accumulated and thrown as a single `BadRequestException`.

### Annotations

All in `speedy-commons/.../annotations/`:

- `@SpeedyEvent` — marks event handler methods (`value` = entity name, `eventType` = array of `SpeedyEventType`)
- `@SpeedyValidator` — marks custom validator methods (`entity` = entity name, `requests` = array of
  `SpeedyValidationRequestType`)
- `@SpeedyAction` — gates CRUD operations per entity/field (`ActionType[]`: READ, CREATE, UPDATE, DELETE, ALL)
- `@SpeedyControllerAdvice` — marker for classes that contain `@SpeedyExceptionHandler` methods; scanned by
  `AdviceExceptionMapper`
- `@SpeedyExceptionHandler` — method-level, maps `Throwable` types to an HTTP `status` and optional custom message (
  `value` = exception classes, `status` = HTTP code, default 500)
- `@SpeedyIgnore` — excludes entity/field from metamodel
- `@SpeedyType` — overrides column type in metamodel
- `@SpeedySensitive` — marks entity/field as blocked from `$` field references
- Validation annotations: `@SpeedyMin`, `@SpeedyMax`, `@SpeedyLength`, `@SpeedyRegex`, `@SpeedyEmail`, `@SpeedyUrl`,
  `@SpeedyFuture`, `@SpeedyPast`, `@SpeedyDateWithFormat`, `@SpeedyDateRange`, `@SpeedyNotBlank`, `@SpeedyPositive`,
  `@SpeedyNegative`, `@SpeedyDigits`, `@SpeedyDecimalMax`, `@SpeedyDecimalMin`, `@SpeedyPositiveOrZero`,
  `@SpeedyNegativeOrZero`

---

## Documentation

User-facing documentation lives in [`docs/`](docs/README.md). Keep docs updated when adding new features.

---

## Design Patterns

### Chain of Responsibility

The 12 handlers form a linear chain where each handler processes what it cares about and passes control via
`next.process(context)`. The chain is wired in `SpeedyFactory.createHandlerChain()`. Individual handlers are unaware of
their position in the chain.

### Builder Pattern

- **`MetaModelBuilder`** / `EntityBuilder` / `FieldBuilder` / `KeyFieldBuilder` — fluent API for constructing the
  metamodel. Entry point: `MetadataBuilder.builder()`.
- **`ResponseContextBuilder`** — builds `IResponseContext` for response serialization.
- **`SpeedyClient`** (Java client) — fluent builders for GET/query/create/update/delete requests.

### Strategy Pattern

- **`QueryProcessor`** interface — single implementation `JooqQueryProcessorImpl`, swappable for other DB backends.
- **`MetaModelProcessor`** interface — `JpaMetaModelProcessorV2` (JPA scan) or `FileMetaModelProcessor` (JSON file).
- **`IResponseSerializerV2`** interface — `JSONSerializerV2` (entity list), `JSONCountSerializerV2` (count only),
  field-level predicate for key-only serialization.
- **`Converter`** interface — type conversion between `SpeedyValue` and DB types.
- **`FieldRule`** interface — 25 composable validation rule implementations.

### SPI / Plugin Pattern

- **`ISpeedyConfiguration`** — primary SPI users must implement.
- **`ISpeedyEventHandler`** and **`ISpeedyCustomValidation`** — marker interfaces; methods discovered via `@SpeedyEvent`
  and `@SpeedyValidator`.

### Observer / Event-Driven Pattern

`EventProcessor` scans handler beans for `@SpeedyEvent` methods, builds a
`Map<SpeedyEventType, MultiValueMap<String, EventHandlerMetadata>>` registry. Events: PRE_INSERT, POST_INSERT,
PRE_UPDATE, POST_UPDATE, PRE_DELETE, POST_DELETE. Entities are auto-converted between `SpeedyEntity` and Java POJOs for
handler method parameters.

### Template Method Pattern

`DefaultFieldValidator.validate()` enforces required fields first, then delegates optional field validation to a
composable list of `FieldRule` objects.

### Value Object

`SpeedyValue` hierarchy (`SpeedyText`, `SpeedyInt`, `SpeedyDouble`, `SpeedyDate`, `SpeedyDateTime`, `SpeedyBoolean`,
`SpeedyEnum`, `SpeedyNull`, `SpeedyCollection`, `SpeedyEntity`) provides a uniform internal type representation
decoupled from the database.

---

## Test Strategy

### Frameworks

- **JUnit Jupiter 5** (via `spring-boot-starter-test`)
- **MockMvc** (`@AutoConfigureMockMvc`) for HTTP integration testing
- **Hamcrest** matchers (`Matchers.hasSize()`, `jsonPath`)
- **JaCoCo** Maven plugin 0.8.13 for code coverage
- **Grafana k6** (`k6-load-test/`) for load testing
- **Python** (`db-tests/run-test.py`) for external DB test automation

### Module-Level Test Organization

| Module               | Location                                                                          | Test Type                                                                          |
|----------------------|-----------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| `speedy-commons`     | `src/test/java/.../validation/`, `metadata/`, `mappings/`, `io/`                  | Unit tests for value types, metadata builders, serializer/deserializer round-trips |
| `speedy-core`        | `src/test/java/.../query/jooq/`, `parser/`, `helpers/`, `deserializer/`, `utils/` | Unit tests for query builders, URI parsing, data conversion                        |
| `speedy-jpa-impl`    | `src/test/java/.../processors/`, `util/`                                          | Unit tests for JPA metamodel processor                                             |
| `speedy-static-impl` | `src/test/java/.../file/impl/`                                                    | Unit tests for file-based metamodel                                                |
| `speedy-java-client` | `src/test/java/.../QueryTest.java`                                                | Unit tests for client builders                                                     |
| `antlr-parser`       | `src/test/java/.../AntlrRequestListenerTest.java`                                 | Unit tests for ANTLR grammar                                                       |
| `speedy-test-app`    | `src/test/java/.../url/`, `query/`, `entity/`, `client/`, `validation/`           | **Integration tests** (`@SpringBootTest` + H2)                                     |

### Integration Test Structure (`speedy-test-app`)

- Annotated with `@SpringBootTest(webEnvironment = MOCK)` + `@AutoConfigureMockMvc`
- H2 in-memory database (`jdbc:h2:mem:bootapp;DB_CLOSE_DELAY=-1`)
- Tests organized by functional area:
    - **URL-based CRUD**: `url/SpeedyGetTest.java`, `SpeedyPostTest.java`, `SpeedyPutTest.java`, `SpeedyDeleteTest.java`
    - **Query DSL**: `query/SpeedyV2*Test.java` (where clauses, paging, ordering, expands, FK traversal, contains,
      negatives)
    - **Validation**: `validation/CategoryValidationIT.java`, `ProductDefaultValidationIT.java`,
      `DateValidationIT.java`, `WebsiteUrlValidationIT.java`
    - **Client**: `client/SpeedyApiTest.java`, `CompanyEventTest.java`, `EventExceptionTest.java`,
      `CompanyEnumTest.java`, `TaskEnumTest.java`
    - **Entity**: `entity/PkUuidTestTest.java` (UUID PK handling)

## Coding Conventions

### Debug Logging

Always pass the exception object as the last argument to SLF4J log methods to print the full stack trace. Never call
`.getMessage()` or pass exception message strings alone — that hides the root cause location.

**Bad:**

```java
log.info("Entity #{} failed: {}",i, e.getMessage());
```

**Good:**

```java
log.info("Entity #{} failed",i, e);
```

### CI/CD

- **`main.yml`**: PR checks on `main` — Ubuntu, JDK 17, `mvn clean install`
- **`release.yml`**: Release on `release` branch — signs with GPG, deploys to Maven Central via `central-portal`
  profile, bumps version, tags `v<version>`
- **`verify.yml`**: Pre-release verification — validates GPG key, tests Central Portal connectivity, compiles
