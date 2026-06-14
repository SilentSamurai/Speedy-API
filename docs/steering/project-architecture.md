---
inclusion: auto
---

# Speedy-API — Project Architecture & Development Guide

## Overview

Speedy-API is a Java 17 / Spring Boot 3.3 framework that auto-generates REST APIs from JPA entities.
It reads JPA metadata at startup, builds an internal `MetaModel`, and exposes a full CRUD + query API
under `/speedy/v1/` without requiring per-entity controllers or repositories.

Version: 3.1.4  
License: Apache 2.0  
Published to: Maven Central via Sonatype Central Portal

---

## Module Dependency Graph

```
speedy-commons          (shared interfaces, models, enums, annotations, exceptions)
    ├── speedy-jpa-impl         (JPA → MetaModel bridge)
    ├── speedy-java-client      (Java client SDK, MockMvc test support)
    ├── speedy-static-impl      (file-based MetaModel for non-JPA use)
    ├── speedy-jooq-query-processor        (jOOQ query execution)
    └── speedy-core             (core engine: handlers, serialization, OpenAPI)
            └── spring-boot-starter-speedy-api  (auto-configuration starter)
                    └── speedy-test-app         (integration test application)

antlr-parser            (ANTLR4 grammar for URL query DSL — standalone, used by antlr tests)
jacoco-aggregate        (code coverage aggregation)
```

---

## Core Abstractions (speedy-commons)

These interfaces define the contract that all implementations must follow:

- `MetaModel` — registry of all entity metadata; lookup by entity name
- `EntityMetadata` — describes one entity: fields, keys, associations, allowed actions (READ/CREATE/UPDATE/DELETE/ALL)
- `FieldMetadata` — describes one field: column type, value type, nullability, associations, enum config, validation
  rules
- `KeyFieldMetadata` — extends FieldMetadata for primary key fields
- `SpeedyValue` — polymorphic value type (text, int, double, boolean, date, datetime, time, zoned datetime, enum,
  object, collection, null)
- `SpeedyEntity` — a map of field name → SpeedyValue, tied to an EntityMetadata
- `SpeedyQuery` — query model: from, where (BooleanCondition tree), orderBy, pageInfo, expand, select
- `QueryProcessor` — executes queries: `executeMany`, `executeCount`, `exists`, `create`, `update`, `delete`
- `ISpeedyConfiguration` — user-provided config: MetaModelProcessor, DataSource, SpeedyDialect, event/validator
  registration
- `ISpeedyRegistry` — registration point for `ISpeedyEventHandler` and `ISpeedyCustomValidation`

### Annotations

| Annotation         | Target       | Purpose                                                              |
|--------------------|--------------|----------------------------------------------------------------------|
| `@SpeedyAction`    | Entity class | Restrict allowed CRUD operations (READ, CREATE, UPDATE, DELETE, ALL) |
| `@SpeedyIgnore`    | Entity/Field | Exclude from MetaModel                                               |
| `@SpeedyType`      | Field        | Override the inferred ColumnType                                     |
| `@SpeedyEvent`     | Method       | Register lifecycle event handler (PRE/POST INSERT/UPDATE/DELETE)     |
| `@SpeedyValidator` | Method       | Register custom validation for CREATE/UPDATE/DELETE                  |

### Value Types

`ValueType` enum:
`TEXT, INT, FLOAT, BOOL, DATE, DATE_TIME, TIME, ZONED_DATE_TIME, ENUM, ENUM_ORD, OBJECT, COLLECTION, NULL`

### Database Dialects

`SpeedyDialect` enum mirrors jOOQ's `SQLDialect`. Common values: `H2`, `POSTGRES`, `MYSQL`, `ORACLE`, `SQLSERVER`,
`SQLITE`, `MARIADB`.

---

## Request Processing Pipeline (speedy-core)

All requests hit `SpeedyApiController` which delegates to `SpeedyFactory.processReqV2()`.
Processing is orchestrated in phases via **multiple sub-chains** (each a `List<Handler>` iterated
sequentially in `SpeedyEngineImpl.run()`). The operation dispatch switch lives in `processReqV2()`.

```
SpeedyFactory.processReqV2()
├── 1. engine.prepare(ctx)              — create QueryProcessor
├── 2. engine.parseUri(ctx)             — uriChain: HeadHandler → UriParserHandler → TailHandler
├── 3. engine.parseHeaders(ctx)         — headerChain: HeadHandler → RequestParserHandler → TailHandler
├── 4. engine.resolveOperation(ctx)     — operationChain: HeadHandler → OperationResolverHandler → TailHandler
├── 5. engine.selectSerializer(ctx)     — serializerSelectionChain: HeadHandler → SerializerSelectionHandler → TailHandler
├── 6. engine.selectBodyParser(ctx)     — parserSelectionChain: HeadHandler → ParserSelectionHandler → TailHandler
├── 7. switch (type) {                  — SINGLE dispatch: write ops parse their body then run the op; read ops just run
│      GET_LIST  → engine.get(ctx)                                  —   getChain: HeadHandler → PermissionCheckHandler → GetHandler → TailHandler
│      QUERY     → engine.parseQueryBody(ctx); engine.query(ctx)    —   queryBodyChain: …→ QueryBodyParserHandler →…; queryChain: …→ QueryHandler →…
│      CREATE    → engine.parseCreateBody(ctx); engine.create(ctx)  —   createBodyChain: …→ CreateBodyParserHandler →…; createChain: …→ CreateHandler →…
│      UPDATE    → engine.parseUpdateBody(ctx); engine.update(ctx)  —   updateBodyChain: …→ UpdateBodyParserHandler →…; updateChain: …→ UpdateHandler →…
│      DELETE    → engine.parseDeleteBody(ctx); engine.delete(ctx)  —   deleteBodyChain: …→ DeleteBodyParserHandler →…; deleteChain: …→ DeleteHandler →…
│      METADATA  → engine.metadata(ctx)                             —   metadataChain: HeadHandler → MetadataHandler → TailHandler
│    }
└── 8. serializer.write(resp, response) — write response directly (not via a handler)
```

### Key Design Decisions

- Sub-chains are immutable after construction — no runtime modification
- `RequestContext` is the mutable state bag that flows through the chain
- `QueryProcessor` is cached per unique `DataSource` returned by `dataSourcePerReq()`. For single-tenant apps this means
  one `QueryProcessor` (and one JOOQ `DSLContext`) for the application lifetime. Multi-tenant deployments automatically
  get a cached instance per tenant's DataSource.
- jOOQ is used for SQL generation and execution (not JPA/Hibernate at runtime), implemented in
  `speedy-jooq-query-processor`
- JPA/Hibernate is only used at startup for metadata introspection via `EntityManagerFactory`

---

## URL Query DSL

GET requests use a custom URL syntax parsed by `SpeedyUriContext`:

```
/speedy/v1/{Entity}                              — get all
/speedy/v1/{Entity}?field=value                  — filter by field
/speedy/v1/{Entity}?$orderBy=name&$pageSize=10   — ordering + pagination
```

POST `$query` requests use a JSON body parsed by `JsonQueryParser`:

```json
{
  "from": "Product",
  "where": { "name": "Widget" },
  "orderBy": [{ "field": "name", "desc": false }],
  "pageSize": 10,
  "expand": ["category"],
  "select": ["name", "price"]
}
```

The ANTLR grammar (`Speedy.g4`) defines a richer URL syntax used by the `antlr-parser` module:

```
/Customer(id='1',name='jolly')
/Customer(amount>0,amount<100)
/Customer(amount<>[1,2,3])
/Customer(id='1')?orderBy=['name','id']&orderByDesc='obc'
```

Operators: `=`, `==`, `!=`, `<`, `>`, `<=`, `>=`, `<>` (IN), `<!>` (NOT IN)  
Boolean connectors: `,` (AND), `&` (AND), `|` (OR)

---

## Event System

Events are annotation-driven. Implement `ISpeedyEventHandler`, annotate methods with `@SpeedyEvent`:

```java
@SpeedyEvent(value = "Product", eventType = {SpeedyEventType.PRE_INSERT})
public void onProductInsert(Product product) { ... }
```

Event types: `PRE_INSERT`, `PRE_UPDATE`, `PRE_DELETE`, `POST_INSERT`, `POST_UPDATE`, `POST_DELETE`

Handler methods receive either a `SpeedyEntity` or a typed Java entity (auto-converted via `SpeedySerializer`/
`SpeedyDeserializer`). Modifications to the entity in PRE events are persisted. Throwing `SpeedyHttpException` from a
handler propagates the error to the client.

Register handlers via `ISpeedyConfiguration.register(registry)`:

```java
registry.registerEventHandler(myEventHandler);
```

---

## Validation System

Custom validators implement `ISpeedyCustomValidation` and annotate methods with `@SpeedyValidator`:

```java
@SpeedyValidator(entity = "Category", requests = SpeedyValidationRequestType.CREATE)
public boolean validateCategory(Category category) {
    return category.getName() != null && !category.getName().isEmpty();
}
```

- Return `boolean` — `false` throws `BadRequestException`
- Or throw `SpeedyHttpException` directly for custom error responses
- Validators can accept `SpeedyEntity` or typed Java entity (auto-converted)
- If no custom validator is registered, `DefaultFieldValidator` runs (checks nullability, required fields, field-level
  rules from annotations)

Register via: `registry.registerValidator(myValidator);`

---

## MetaModel Implementations

### JPA (speedy-jpa-impl)

`JpaMetaModelProcessorV2` reads `EntityManagerFactory` metadata at startup. Supports:

- All JPA entity annotations
- Composite keys
- Associations (ManyToOne, OneToMany, etc.)
- Jakarta Validation annotations for field rules

### Static/File-based (speedy-static-impl)

Reads entity metadata from JSON files. Useful for non-JPA data sources.

---

## Spring Boot Integration

Add `spring-boot-starter-speedy-api` dependency. The auto-configuration:

1. Detects an `ISpeedyConfiguration` bean
2. Creates `SpeedyFactory` (builds MetaModel, event processor, validation processor, handler chain)
3. Creates `SpeedyOpenApiCustomizer` for OpenAPI/Swagger doc generation
4. Registers `SpeedyApiController` at `/speedy/v1/**`

User must provide:

```java
@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {
    @Override public MetaModelProcessor metaModelProcessor() { ... }
    @Override public void register(ISpeedyRegistry registry) { ... }
    @Override public DataSource dataSourcePerReq() { ... }
    @Override public SpeedyDialect getDialect() { ... }
}
```

---

## Build & Test

```bash
# Full build + test
mvn clean install

# Compile only (skip tests)
mvn clean compile -DskipTests

# Run specific module tests
mvn test -pl speedy-test-app
mvn test -pl antlr-parser
```

- Java 17 required
- Tests use H2 in-memory database
- Test app generates OpenAPI client code via `openapi-generator-maven-plugin`
- JaCoCo coverage reports generated per module + aggregated in `jacoco-aggregate`

---

## CI/CD Pipelines

| Workflow      | Trigger           | Action                                                          |
|---------------|-------------------|-----------------------------------------------------------------|
| `main.yml`    | PR to `main`      | `mvn clean install`                                             |
| `verify.yml`  | PR to `release`   | Verify Maven Central creds, GPG key, build, OSSRH connection    |
| `release.yml` | Push to `release` | Auto bump patch version, GPG sign, deploy to Maven Central, tag |

---

## Coding Conventions

- Package root: `com.github.silent.samurai.speedy`
- Lombok used for `@Getter`, `@Setter`, `@Slf4j`, `@Getter` on classes
- All exceptions extend `SpeedyHttpException` (carries HTTP status code)
- Handler pattern: implement `Handler` interface, accept `SpeedyContext`, no `next` reference (iteration is external
  in `SpeedyEngineImpl.run()`)
- New entity types/fields: add to JPA entities, MetaModel picks them up automatically
- New CRUD behavior: add/modify handlers in the relevant sub-chain (wired in `SpeedyEngineImpl` constructor)
- New query operators: extend `ConditionOperator` enum + `ConditionFactory` + `speedy-jooq-query-processor` query
  builder
- Serialization: `JSONResponseSerializer` for response output, `JsonNode2SpeedyValue` for input parsing
