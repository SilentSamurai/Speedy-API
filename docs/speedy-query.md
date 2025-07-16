# SpeedyQuery

A fluent query builder for constructing complex database queries in the Speedy API. This class provides a type-safe and intuitive way to build queries with conditions, ordering, pagination, field selection, and entity expansion.

## Overview

SpeedyQuery offers:
- **Fluent API**: Chain methods for building complex queries
- **Type Safety**: Compile-time validation of query structure
- **Conditional Logic**: Support for AND/OR conditions with nested logic
- **Pagination**: Built-in support for page-based results
- **Field Selection**: Choose specific fields to return
- **Entity Expansion**: Include related entities in results
- **JSON Output**: Generate JSON query objects for API consumption

## Quick Start

### Basic Query

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("active", eq(true)))
    .orderByAsc("name")
    .pageSize(20)
    .build();
```

### Complex Query

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

SpeedyQuery query = SpeedyQuery.from("users")
    .where(
        and(
            condition("age", gte(18)),
            condition("active", eq(true)),
            or(
                condition("role", eq("admin")),
                condition("role", eq("moderator"))
            )
        )
    )
    .select("id", "name", "email", "role")
    .expand("profile", "permissions")
    .orderByDesc("createdAt")
    .pageNo(1)
    .pageSize(50)
    .build();
```

## Query Structure

The generated query follows this JSON structure:

```json
{
  "$from": "entity_name",
  "$where": { "conditions" },
  "$select": ["field1", "field2"],
  "$expand": ["relation1", "relation2"],
  "$orderBy": { "field": "ASC|DESC" },
  "$page": { "$index": 0, "$size": 10 }
}
```

## Builder Methods

### Entity Selection

```java
// Start with entity name (recommended)
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("active", eq(true)))
    .build();

// Or set entity later
SpeedyQuery query = SpeedyQuery.from()
    .fromEntity("users")
    .where(condition("active", eq(true)))
    .build();
```

### Field Selection

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .select("id", "name", "email", "createdAt")
    .where(condition("active", eq(true)))
    .build();
```

### Entity Expansion

SpeedyQuery supports both simple entity expansions and multi-level nested expansions using dot notation.

#### Simple Entity Expansion

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .expand("profile")
    .expand("permissions")
    .expand("department")
    .where(condition("active", eq(true)))
    .build();
```

#### Multi-Level Expansion

Use dot notation to expand nested relationships:

```java
// Expand product and its category
SpeedyQuery query = SpeedyQuery.from("inventory")
    .expand("Product")
    .expand("Product.Category")
    .where(condition("quantity", gt(0)))
    .build();

// Deep nested expansion
SpeedyQuery query = SpeedyQuery.from("inventory")
    .expand("Product")
    .expand("Product.Category")
    .expand("Product.Category.Supplier")
    .expand("Product.Category.Supplier.Address")
    .where(condition("quantity", gt(0)))
    .build();
```

#### Complex Multi-Level Expansion Examples

```java
// Multiple expansion paths at different levels
SpeedyQuery query = SpeedyQuery.from("inventory")
    .expand("Product")
    .expand("Product.Category")
    .expand("Procurement")
    .expand("Procurement.Product")
    .expand("Procurement.Product.Category")
    .where(condition("quantity", gt(0)))
    .build();

// Expansion with field selection
SpeedyQuery query = SpeedyQuery.from("inventory")
    .select("id", "quantity", "location")
    .expand("Product")
    .expand("Product.Category")
    .expand("Product.Category.Supplier")
    .where(condition("quantity", gt(0)))
    .build();
```

#### Multi-Level Expansion Rules

- **Dot Notation**: Use dots (`.`) to separate entity levels in the expansion path
- **Path Validation**: Each segment in the path must be a valid entity association
- **Performance**: Deep expansions may impact query performance
- **Validation**: Invalid expansion paths will result in a `BadRequestException`

#### Multi-Level Expansion Examples

| Use Case | Query | Description |
|----------|-------|-------------|
| Product with Category | `.expand("Product").expand("Product.Category")` | Include product and its category |
| Deep Supplier Chain | `.expand("Product.Category.Supplier.Address")` | Include complete supplier chain |
| Multiple Paths | `.expand("Product.Category").expand("Procurement.Product")` | Different expansion paths |
| Selective Expansion | `.expand("Product").expand("Product.Category")` | Only expand specific paths |

### Ordering

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .orderByAsc("name")
    .orderByAsc("createdAt")
    .orderByDesc("lastLogin")
    .where(condition("active", eq(true)))
    .build();
```

### Pagination

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .pageNo(2)      // Get the third page (0-based indexing)
    .pageSize(50)   // Get 50 records per page
    .where(condition("active", eq(true)))
    .build();
```

## Comparison Operators

### Basic Comparisons

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

// Equal to
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("status", eq("active")))
    .build();

// Not equal to
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("status", ne("inactive")))
    .build();

// Greater than
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("age", gt(18)))
    .build();

// Less than
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("price", lt(100)))
    .build();

// Greater than or equal
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("score", gte(80)))
    .build();

// Less than or equal
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("quantity", lte(10)))
    .build();
```

### Array Operations

```java
// In array of values
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("role", in("admin", "moderator", "user")))
    .build();

// Not in array
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("status", nin("deleted", "archived")))
    .build();
```

### Pattern Matching

```java
// Pattern matching (wildcard)
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("name", matches("*john*")))
    .build();
```

**Wildcard Syntax:**
- `*` = 0 or more characters/spaces
- `?` = exactly 1 character/space
- Examples:
  - `"*john*"` matches "john", "johnny", "john doe", "my john", etc.
  - `"john*"` matches "john", "johnny", "john doe", etc.
  - `"*john"` matches "john", "my john", "the john", etc.
  - `"j?hn"` matches "john", "jahn", etc.

### Field References

SpeedyQuery supports field-to-field comparisons using the `field()` method. This allows you to compare values between different fields in the same entity.

#### Basic Field Reference

```java
// Compare two fields for equality
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("salePrice", eq("$regularPrice")))
    .build();
```

#### Field Reference with Comparison Operators

```java
// Find products where sale price is less than regular price
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("salePrice", lt("$regularPrice")))
    .build();

// Find orders where start date is before end date
SpeedyQuery query = SpeedyQuery.from("orders")
    .where(condition("startDate", lte("$endDate")))
    .build();
```

#### Complex Field Reference Examples

```java
// Compare user tracking fields
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("createdBy", eq("$updatedBy")))
    .build();

// Inventory check with field reference
SpeedyQuery query = SpeedyQuery.from("products")
    .where(
        and(
            condition("currentStock", gte(field("minimumStock"))),
            condition("salePrice", lt(field("regularPrice")))
        )
    )
    .build();
```

#### Field Reference Rules

- **Method**: Use `"$fieldName"` to reference another field
- **Field Names**: Use the exact field names as defined in your entity
- **Supported Operators**: All comparison operators support field references
- **Validation**: Invalid field references will result in a `NotFoundException`

#### Field Reference Examples

| Use Case | Query | Description |
|----------|-------|-------------|
| Price Comparison | `condition("salePrice", lt("$regularPrice"))` | Find products on sale |
| Date Range | `condition("startDate", lte("$endDate"))` | Valid date ranges |
| User Tracking | `condition("createdBy", eq("$updatedBy"))` | Self-updated records |
| Inventory Check | `condition("currentStock", gte("$minimumStock"))` | Sufficient inventory |

## Logical Operators

### AND Conditions

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .where(
        and(
            condition("active", eq(true)),
            condition("age", gte(18)),
            condition("verified", eq(true))
        )
    )
    .build();
```

### OR Conditions

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .where(
        or(
            condition("role", eq("admin")),
            condition("role", eq("moderator"))
        )
    )
    .build();
```

### Complex Nested Logic

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .where(
        and(
            condition("active", eq(true)),
            condition("age", gte(18)),
            or(
                condition("role", eq("admin")),
                condition("role", eq("moderator")),
                and(
                    condition("verified", eq(true)),
                    condition("premium", eq(true))
                )
            )
        )
    )
    .build();
```

## Real-World Examples

### User Search

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .where(
        and(
            condition("active", eq(true)),
            or(
                condition("name", matches("*john*")),
                condition("email", matches("*john*"))
            ),
            condition("age", gte(18))
        )
    )
    .select("id", "name", "email", "avatar")
    .orderByAsc("name")
    .pageSize(20)
    .build();
```

### Product Filtering

```java
SpeedyQuery query = SpeedyQuery.from("products")
    .where(
        and(
            condition("category", in("electronics", "computers")),
            condition("price", gte(500)),
            condition("inStock", eq(true)),
            or(
                condition("brand", eq("Apple")),
                condition("brand", eq("Samsung"))
            )
        )
    )
    .select("id", "name", "price", "brand", "rating")
    .expand("reviews", "images")
    .orderByDesc("rating")
    .orderByAsc("price")
    .pageSize(50)
    .build();
```

### Order History

```java
SpeedyQuery query = SpeedyQuery.from("orders")
    .where(
        and(
            condition("userId", eq(userId)),
            condition("status", nin("cancelled", "refunded")),
            condition("createdAt", gte(startDate)),
            condition("createdAt", lte(endDate))
        )
    )
    .select("id", "total", "status", "createdAt")
    .expand("items", "shipping")
    .orderByDesc("createdAt")
    .pageSize(100)
    .build();
```

### Product Inventory Management

```java
SpeedyQuery query = SpeedyQuery.from("products")
    .where(
        and(
            // Find products where sale price is less than regular price
            condition("salePrice", lt(field("regularPrice"))),
            // Ensure current stock is above minimum threshold
            condition("currentStock", gte(field("minimumStock"))),
            // Only active products
            condition("active", eq(true)),
            // In specific categories
            condition("category", in("electronics", "computers"))
        )
    )
    .select("id", "name", "salePrice", "regularPrice", "currentStock", "minimumStock")
    .orderByAsc("currentStock")  // Show low stock first
    .pageSize(50)
    .build();
```

### Date Range Validation

```java
SpeedyQuery query = SpeedyQuery.from("events")
    .where(
        and(
            // Ensure start date is before end date
            condition("startDate", lte(field("endDate"))),
            // Events in the future
            condition("startDate", gte(LocalDate.now().toString())),
            // Active events only
            condition("status", eq("active"))
        )
    )
    .select("id", "title", "startDate", "endDate", "location")
    .orderByAsc("startDate")
    .pageSize(20)
    .build();
```

### Date and Time Queries

```java
// Query by date range
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .where(condition("localDate", gt(LocalDate.now().toString())))
    .build();

// Query by time
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .where(condition("localTime", gt(LocalTime.of(11, 0).toString())))
    .build();

// Query by instant
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .where(condition("instantTime", lt(Instant.now().toString())))
    .build();
```

### Numeric Queries

```java
// Query by double value
SpeedyQuery query = SpeedyQuery.from("ValueTestEntity")
    .where(condition("doubleValue", eq(1.5430434)))
    .build();
```

## Debugging Queries

### Pretty Print

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("active", eq(true)))
    .select("id", "name")
    .prettyPrint()  // Logs the query structure
    .build();
```

This will log the generated JSON query to help with debugging.

### Manual JSON Inspection

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("active", eq(true)))
    .build();

JsonNode queryJson = query.build();
System.out.println(queryJson.toPrettyString());
```

## Best Practices

### 1. Use Static Imports

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

// Much cleaner than SpeedyQuery.from("users")
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("active", eq(true)))
    .build();
```

### 2. Validate Inputs

```java
public SpeedyQuery buildUserQuery(String status, int minAge) {
    if (status == null || status.trim().isEmpty()) {
        throw new IllegalArgumentException("Status cannot be null or empty");
    }
    if (minAge < 0) {
        throw new IllegalArgumentException("Minimum age cannot be negative");
    }
    
    return SpeedyQuery.from("users")
        .where(
            and(
                condition("status", eq(status)),
                condition("age", gte(minAge))
            )
        )
        .build();
}
```

### 3. Optimize Field Selection

```java
// Good: Select only needed fields
SpeedyQuery query = SpeedyQuery.from("users")
    .select("id", "name", "email")  // Only get what you need
    .where(condition("active", eq(true)))
    .build();

// Avoid: Getting all fields
SpeedyQuery badQuery = SpeedyQuery.from("users")
    .where(condition("active", eq(true)))
    .build();  // Gets everything!
```

### 4. Use Appropriate Page Sizes

```java
// Good: Reasonable page size
SpeedyQuery query = SpeedyQuery.from("users")
    .pageSize(20)  // Good for UI pagination
    .where(condition("active", eq(true)))
    .build();

// Avoid: Too large page sizes
SpeedyQuery badQuery = SpeedyQuery.from("users")
    .pageSize(10000)  // Could cause performance issues
    .where(condition("active", eq(true)))
    .build();
```

### 5. Build Reusable Query Components

```java
public class UserQueries {
    
    public static SpeedyQuery activeUsers() {
        return SpeedyQuery.from("users")
            .where(condition("active", eq(true)))
            .build();
    }
    
    public static SpeedyQuery usersByRole(String role) {
        return SpeedyQuery.from("users")
            .where(condition("role", eq(role)))
            .build();
    }
    
    public static SpeedyQuery usersWithProfile() {
        return SpeedyQuery.from("users")
            .expand("profile")
            .select("id", "name", "email")
            .build();
    }
}

// Usage
SpeedyQuery query = UserQueries.activeUsers();
SpeedyQuery adminQuery = UserQueries.usersByRole("admin");
```

## Operator Reference

### Comparison Operators

| Operator | Method | Description | Example |
|----------|--------|-------------|---------|
| `$eq` | `eq(Object)` | Equal to | `eq("active")` |
| `$ne` | `ne(Object)` | Not equal to | `ne("inactive")` |
| `$gt` | `gt(Object)` | Greater than | `gt(18)` |
| `$lt` | `lt(Object)` | Less than | `lt(100)` |
| `$gte` | `gte(Object)` | Greater than or equal | `gte(80)` |
| `$lte` | `lte(Object)` | Less than or equal | `lte(10)` |
| `$in` | `in(Object...)` | In array of values | `in("A", "B", "C")` |
| `$nin` | `nin(Object...)` | Not in array | `nin("deleted", "archived")` |
| `$matches` | `matches(Object)` | Pattern matching | `matches("*john*")` |

### Logical Operators

| Operator | Method | Description | Example |
|----------|--------|-------------|---------|
| `$and` | `and(JsonNode...)` | Logical AND | `and(cond1, cond2, cond3)` |
| `$or` | `or(JsonNode...)` | Logical OR | `or(cond1, cond2)` |

### Query Methods

| Category | Methods | Description |
|----------|---------|-------------|
| **Builder** | `from()`, `from(String entity)` | Create new query instances |
| **Source** | `fromEntity(String entity)` | Set the target entity |
| **Conditions** | `where(JsonNode...)` | Add WHERE conditions |
| **Selection** | `select(String...)` | Choose fields to return |
| **Expansion** | `expand(String...)` | Include related entities |
| **Ordering** | `orderByAsc(String)`, `orderByDesc(String)` | Sort results |
| **Pagination** | `pageNo(int)`, `pageSize(int)` | Control result pagination |
| **Execution** | `build()`, `prettyPrint()` | Generate final query | 