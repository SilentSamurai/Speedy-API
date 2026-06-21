# Speedy-API

**Instant REST CRUD APIs for all your JPA entities — without writing a single line of controller code.**

Speedy-API is a Spring Boot library that exposes every `@Entity` in your application as a fully-featured REST resource. Annotate your entities, drop in a small configuration class, and Speedy gives you list, query, create, update, delete, metadata, paging, ordering, field selection, and relationship expansion out of the box — all backed by jOOQ-generated SQL.

![main workflow](https://github.com/SilentSamurai/Speedy-API/actions/workflows/main.yml/badge.svg)
![release workflow](https://github.com/SilentSamurai/Speedy-API/actions/workflows/release.yml/badge.svg)

---

## Why Speedy?

- **Zero boilerplate** — no controllers, no DTOs, no repositories. Your JPA entities *are* the API.
- **Rich query DSL** — a JSON query language with comparison operators, logical `$and`/`$or`, field-to-field references, pattern matching, paging, ordering, and `$select` projections.
- **Relationship expansion** — pull in related entities to any depth with dot-notation `$expand` paths.
- **jOOQ-powered SQL** — queries compile to efficient SQL with automatic JOINs; supports H2, PostgreSQL, MySQL, and more.
- **Extensible** — lifecycle event handlers, custom validators, a rich set of validation annotations, and per-entity/per-field CRUD gating.
- **OpenAPI integration** — endpoints are described via springdoc for Swagger UI.
- **Typed Java client** — a fluent SDK for calling Speedy backends from Java (production via `RestTemplate`, tests via `MockMvc`).

---

## Getting Started

Speedy works with Spring Boot 2.1 and above.

### 1. Add the dependencies

```xml
<!-- Spring Boot auto-configuration for Speedy -->
<dependency>
    <groupId>com.github.silentsamurai</groupId>
    <artifactId>spring-boot-starter-speedy-api</artifactId>
    <version>3.1.4</version>
</dependency>

<!-- JPA metamodel processor: builds the Speedy model from your @Entity classes -->
<dependency>
    <groupId>com.github.silentsamurai</groupId>
    <artifactId>speedy-jpa-metamodel-processor</artifactId>
    <version>3.1.4</version>
</dependency>
```

### 2. Provide a configuration

Speedy activates only when an `ISpeedyConfiguration` bean is present. It needs an entity manager, a metamodel processor, a data source, and a SQL dialect:

```java
@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    public SpeedyConfig(EntityManagerFactory entityManagerFactory, DataSource dataSource) {
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
    }

    @Override
    public MetaModelProcessor metaModelProcessor() {
        return new JpaMetaModelProcessor(this, entityManagerFactory);
    }

    @Override
    public void register(ISpeedyRegistry registry) {
        // register event handlers and custom validators here
    }

    @Override
    public DataSource dataSourcePerReq() {
        return dataSource;   // return a tenant-specific DataSource for multi-tenancy
    }

    @Override
    public SpeedyDialect getDialect() {
        return SpeedyDialect.H2;
    }
}
```

### 3. Annotate your entities

Plain JPA entities are all Speedy needs:

```java
@Setter
@Getter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    protected UUID id;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "email", nullable = false, length = 250)
    private String email;

    @Column(name = "type", nullable = false, length = 512)
    private String type;

    @SpeedyAction(ActionType.READ)   // read-only field: never written via the API
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

That's it — `User` is now a live REST resource.

> **Note:** Speedy-API is a library, not a security framework. Authentication and per-user authorization belong to your application (e.g. Spring Security filters in front of `/speedy/v1/**`). `@SpeedyAction` provides *static* per-entity/per-field CRUD gating, not per-user rules.

---

## REST API

All endpoints live under `/speedy/v1`.

| Method        | URI                       | Purpose                                                                      |
|---------------|---------------------------|------------------------------------------------------------------------------|
| `GET`         | `/{Entity}`               | List entities, filtered/paged/ordered/expanded via URL query params          |
| `GET`         | `/$metadata`              | Returns the full metamodel as JSON                                           |
| `POST`        | `/{Entity}/$query`        | Advanced query with a JSON body                                             |
| `POST`        | `/{Entity}/$create`       | Bulk create from a JSON array                                               |
| `PUT`/`PATCH` | `/{Entity}/$update`       | Update a single entity by primary key                                       |
| `DELETE`      | `/{Entity}/$delete`       | Bulk delete by primary-key array                                           |

### Example: query with the JSON DSL

```http
POST /speedy/v1/User/$query
Content-Type: application/json
Accept: application/json

{
    "$where": {
        "type": "ADMIN",
        "loginCount": { "$gt": 0 }
    },
    "$select": ["id", "name", "email"],
    "$orderBy": { "createdAt": "DESC" },
    "$page": { "$index": 0, "$size": 20 }
}
```

The query DSL supports comparison operators (`$eq`, `$ne`, `$lt`, `$gt`, `$lte`, `$gte`, `$in`, `$nin`, `$matches`), logical operators (`$and`, `$or`), field-to-field references (`"salePrice": { "$lt": "$regularPrice" }`), multi-level `$expand` paths, `$select` projections, and `$select: ["$count"]` count queries. See the [Query docs](docs/query-operation.md) for the full reference.

---

## Java Client

Call a Speedy backend from Java with a fluent, type-safe client:

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

SpeedyClient<SpeedyResponse> client =
        SpeedyClient.restTemplate(new RestTemplate(), "http://localhost:8080");

// Create
client.create("Category")
      .addField("name", "Electronics")
      .execute();

// Query
SpeedyQuery query = SpeedyQuery.from("Category")
        .where(condition("name", eq("Electronics")))
        .build();
SpeedyResponse categories = client.query(query).execute();
```

Add it with:

```xml
<dependency>
    <groupId>com.github.silentsamurai</groupId>
    <artifactId>speedy-java-client</artifactId>
    <version>3.1.4</version>
</dependency>
```

A `MockMvc` transport (`SpeedyClient.mockMvc(mockMvc)`) makes it easy to drive Speedy endpoints from integration tests. See the [Java Client docs](docs/java-client.md).

---

## Modules

Speedy is a multi-module Maven project:

| Module                              | Purpose                                                                                   |
|-------------------------------------|-------------------------------------------------------------------------------------------|
| `speedy-commons`                    | Shared interfaces, `SpeedyValue` types, query/condition model, annotations, validation    |
| `speedy-core`                       | Request-processing engine: handler chain, URI/JSON parsing, serialization, `SpeedyApiController` |
| `speedy-jpa-metamodel-processor`    | Builds the metamodel by scanning JPA `@Entity` classes                                     |
| `speedy-static-metamodel-processor` | Builds the metamodel from a JSON file                                                      |
| `speedy-jooq-query-processor`       | jOOQ-based SQL generation and query execution                                             |
| `speedy-json-io`                    | JSON serialization/deserialization support                                                |
| `spring-boot-starter-speedy-api`    | Spring Boot auto-configuration entry point                                                |
| `speedy-java-client`                | Fluent, typed Java client SDK                                                              |
| `antlr-parser`                      | ANTLR4 grammar for a URL DSL (legacy)                                                      |
| `speedy-test-app`                   | Full Spring Boot integration-test application                                             |
| `jacoco-aggregate`                  | Aggregates code-coverage reports across modules                                           |

---

## Building

Requires JDK 17+ and Maven.

```bash
mvn clean install
```

This compiles every module and runs the test suite (unit tests plus `speedy-test-app` integration tests against an in-memory H2 database).

---

## Documentation

Full documentation is published at **[silentsamurai.github.io/Speedy-API](https://silentsamurai.github.io/Speedy-API/)** and lives in [`docs/`](docs/README.md):

- [Getting Started](docs/getting-started.md)
- [GET Operations](docs/get-operation.md) · [Query Operations](docs/query-operation.md) · [POST](docs/post-operation.md) · [PUT](docs/put-operation.md) · [DELETE](docs/delete-operation.md)
- [Field References](docs/field-references.md) · [Multi-Level Expansions](docs/multi-level-expansions.md)
- [Validation Rules](docs/validation-rules.md) · [Speedy Events](docs/speedy-events.md) · [Exception Handling](docs/exception-handling.md)
- [Custom Types](docs/custom-types.md) · [OpenAPI Integration](docs/api-docs.md)
- [Java Client](docs/java-client.md)

---

## License

Speedy-API is released under the [Apache License 2.0](LICENSE).
