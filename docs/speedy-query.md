# SpeedyQuery

`SpeedyQuery` is a set of static factory methods for building the `$where` condition tree used by
`speedy.query(entity)` (see [SpeedyClient](speedy-client.md)). Operator methods (`eq`, `gt`, `in`, ...) return Jackson
`JsonNode`s that compose into the same JSON query structure documented in [Query Operations](query-operation.md).

## Quick Start

```java
import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;

List<User> users = speedy.query("User")
        .where(
                and(
                        condition("active", eq(true)),
                        condition("age", gte(18))
                )
        )
        .select("id", "name", "email")
        .orderByAsc("name")
        .pageSize(20)
        .execute()
        .list(User.class);
```

## Query Structure

`speedy.query(entity)` builds and sends this JSON body to `POST /{Entity}/$query`:

```json
{
  "$from": "entity_name",
  "$where": { "conditions": "..." },
  "$select": ["field1", "field2"],
  "$expand": ["relation1", "relation2"],
  "$orderBy": { "field": "ASC|DESC" },
  "$page": { "$index": 0, "$size": 10 }
}
```

## Builder Methods

`speedy.query(entity)` returns a `QueryBuilder` with:

| Method                          | Effect                                                    |
|-----------------------------------|-------------------------------------------------------------|
| `where(JsonNode... conditions)`  | Sets/merges `$where` conditions                             |
| `select(String... fields)`       | Adds fields to `$select` (projection)                       |
| `expand(String... relations)`    | Adds relations to `$expand`, dot-notation for multi-level    |
| `orderByAsc(String field)`       | Adds an ascending `$orderBy` entry                           |
| `orderByDesc(String field)`      | Adds a descending `$orderBy` entry                           |
| `pageNo(int)`                    | Sets `$page.$index` (0-based)                                |
| `pageSize(int)`                  | Sets `$page.$size`                                           |
| `build()`                        | Returns the built JSON body without executing                |
| `execute()`                      | Sends the query and returns a `SpeedyResult`                 |
| `count()`                        | Sends a `$select=$count`-equivalent request and returns `long`|

### Field Selection

```java
List<User> users = speedy.query("User")
        .select("id", "name", "email", "createdAt")
        .where(condition("active", eq(true)))
        .execute()
        .list(User.class);
```

### Entity Expansion

`expand(...)` supports dot notation for nested relationships, same as the [GET](get-operation.md) and
[Multi-Level Expansions](multi-level-expansions.md) URL DSL:

```java
List<Inventory> inventory = speedy.query("Inventory")
        .where(condition("quantity", gt(0)))
        .expand("Product")
        .expand("Product.Category")
        .expand("Product.Category.Supplier")
        .execute()
        .list(Inventory.class);
```

### Ordering

```java
speedy.query("User")
        .where(condition("active", eq(true)))
        .orderByAsc("name")
        .orderByDesc("createdAt")
        .execute();
```

Each call adds one field to `$orderBy`; the server applies them in the order given.

### Pagination

```java
speedy.query("User")
        .where(condition("active", eq(true)))
        .pageNo(2)      // third page, 0-based
        .pageSize(50)
        .execute();
```

### Count

```java
long total = speedy.query("User")
        .where(condition("active", eq(true)))
        .count();
```

## Comparison Operators

Each returns a Jackson `ObjectNode` wrapping a single operator key, to be passed into `condition(field, operator)`:

| Operator     | Method                | Example                        |
|--------------|------------------------|---------------------------------|
| `$eq`        | `eq(Object)`           | `condition("status", eq("active"))` |
| `$ne`        | `ne(Object)`           | `condition("status", ne("inactive"))` |
| `$gt`        | `gt(Object)`           | `condition("age", gt(18))`      |
| `$lt`        | `lt(Object)`           | `condition("price", lt(100))`   |
| `$gte`       | `gte(Object)`          | `condition("score", gte(80))`   |
| `$lte`       | `lte(Object)`          | `condition("quantity", lte(10))`|
| `$in`        | `in(Object...)`        | `condition("role", in("admin", "user"))` |
| `$nin`       | `nin(Object...)`       | `condition("status", nin("deleted", "archived"))` |
| `$matches`   | `matches(Object)`      | `condition("name", matches("*john*"))` |
| `$contains`  | `contains(Object)`     | `condition("tags", contains("urgent"))` |
| `$between`   | `between(low, high)`   | `condition("cost", between(10, 50))` |
| `$isnull`    | `isnull()`             | `condition("modifiedAt", isnull())` |
| `$isnotnull` | `isnotnull()`          | `condition("createdAt", isnotnull())` |

**Wildcard syntax for `$matches`:** `*` matches zero or more characters, `?` matches exactly one.

**`$between` / `$isnull` / `$isnotnull` error conditions:**

| Operator     | Error Condition          | Message                                                  |
|--------------|--------------------------|------------------------------------------------------------|
| `$between`   | Non-array value           | "$between only accepts an array"                          |
| `$between`   | Array with ≠ 2 values     | "$between requires exactly 2 values"                       |
| `$isnull`    | `false` value             | "$isnull requires true. Use $isnotnull for IS NOT NULL"    |
| `$isnotnull` | `false` value             | "$isnotnull requires true. Use $isnull for IS NULL"        |

## Logical Operators

`and(...)` / `or(...)` combine multiple `condition(...)` calls, and can nest:

```java
speedy.query("User")
        .where(
                and(
                        condition("active", eq(true)),
                        condition("age", gte(18)),
                        or(
                                condition("role", eq("admin")),
                                condition("role", eq("moderator"))
                        )
                )
        )
        .execute();
```

`and`/`or` can also nest *inside* a single `condition(...)` call, combining several operators on the same field
without repeating it as a top-level condition — e.g. "field matches a value OR is null":

```java
speedy.query("Product")
        .where(condition("categoryId", or(eq(null), eq("electronics"))))
        .execute();
```

This generates:

```json
{
  "$where": {
    "categoryId": { "$or": [{ "$eq": null }, { "$eq": "electronics" }] }
  }
}
```

## Field-to-Field References

There's no dedicated `field(...)` helper — reference another field by passing its name prefixed with `$` as the
operator value, exactly as in the raw JSON DSL described in [Field References](field-references.md):

```java
// Find products on sale: salePrice < regularPrice
speedy.query("Product")
        .where(condition("salePrice", lt("$regularPrice")))
        .execute();

// Valid date ranges: startDate <= endDate
speedy.query("Order")
        .where(condition("startDate", lte("$endDate")))
        .execute();
```

## Debugging

```java
QueryBuilder query = speedy.query("User")
        .where(condition("active", eq(true)))
        .select("id", "name");

JsonNode body = query.build();
System.out.println(body.toPrettyString());
```

`build()` returns the JSON body without sending the request, so it's safe to inspect or log before calling
`execute()`.
