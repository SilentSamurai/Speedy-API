# Field References in Speedy

Field references allow you to compare values between different fields in the same entity or related entities. This powerful feature enables complex queries that would otherwise require multiple database calls or complex joins.

## Overview

Field references use the `$` prefix in JSON queries and the `field()` method in the Java client to reference other fields in comparisons.

## JSON API Usage

### Basic Field Reference

Compare two fields for equality:

```http
POST /speedy/v1/Product/$query
Accept: application/json
Content-Type: application/json

{
    "$where": {
        "salePrice": "$regularPrice"
    }
}
```

This finds products where the sale price equals the regular price.

### Field Reference with Operators

Use any comparison operator with field references:

```http
POST /speedy/v1/Product/$query
Accept: application/json
Content-Type: application/json

{
    "$where": {
        "salePrice": {
            "$lt": "$regularPrice"
        }
    }
}
```

This finds products where the sale price is less than the regular price.

## Java Client Usage

### Basic Field Reference

```java
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

// Compare two fields for equality
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("salePrice", eq("$regularPrice")))
    .build();
```

### Field Reference with Comparison Operators

```java
// Less than comparison
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("salePrice", lt("$regularPrice")))
    .build();

// Greater than or equal comparison
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("currentStock", gte("$minimumStock")))
    .build();

// Not equal comparison
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("createdBy", ne("$updatedBy")))
    .build();
```

## Common Use Cases

### 1. Price Comparisons

**Find products on sale:**

```java
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("salePrice", lt("$regularPrice")))
    .build();
```

**Find products with significant discounts:**

```java
SpeedyQuery query = SpeedyQuery.from("products")
    .where(
        and(
            condition("salePrice", lt("$regularPrice")),
            condition("discountPercentage", gte(20))
        )
    )
    .build();
```

### 2. Date Range Validation

**Find events with valid date ranges:**

```java
SpeedyQuery query = SpeedyQuery.from("events")
    .where(condition("startDate", lte("$endDate")))
    .build();
```

**Find overlapping appointments:**

```java
SpeedyQuery query = SpeedyQuery.from("appointments")
    .where(
        or(
            condition("startTime", lt("$endTime")),
            condition("endTime", gt("$startTime"))
        )
    )
    .build();
```

### 3. Inventory Management

**Find products with sufficient stock:**

```java
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("currentStock", gte("$minimumStock")))
    .build();
```

**Find products that need reordering:**

```java
SpeedyQuery query = SpeedyQuery.from("products")
    .where(
        and(
            condition("currentStock", lt("$minimumStock")),
            condition("active", eq(true))
        )
    )
    .build();
```

### 4. User Activity Tracking

**Find self-updated records:**

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .where(condition("createdBy", eq("$updatedBy")))
    .build();
```

**Find records updated by different users:**

```java
SpeedyQuery query = SpeedyQuery.from("documents")
    .where(condition("createdBy", ne("$updatedBy")))
    .build();
```

### 5. Financial Calculations

**Find transactions with matching amounts:**

```java
SpeedyQuery query = SpeedyQuery.from("transactions")
    .where(condition("debitAmount", eq("$creditAmount")))
    .build();
```

**Find profitable orders:**

```java
SpeedyQuery query = SpeedyQuery.from("orders")
    .where(condition("revenue", gt("$cost")))
    .build();
```

## Complex Examples

### Product Inventory with Multiple Conditions

```java
SpeedyQuery query = SpeedyQuery.from("products")
    .where(
        and(
            // On sale
            condition("salePrice", lt("$regularPrice")),
            // Sufficient stock
            condition("currentStock", gte("$minimumStock")),
            // Active products
            condition("active", eq(true)),
            // In specific categories
            condition("category", in("electronics", "computers"))
        )
    )
    .select("id", "name", "salePrice", "regularPrice", "currentStock", "minimumStock")
    .orderByAsc("currentStock")
    .build();
```

### Event Scheduling Validation

```java
SpeedyQuery query = SpeedyQuery.from("events")
    .where(
        and(
            // Valid date range
            condition("startDate", lte("$endDate")),
            // Future events
            condition("startDate", gte(LocalDate.now().toString())),
            // Active events
            condition("status", eq("active")),
            // Within budget
            condition("actualCost", lte("$budget"))
        )
    )
    .select("id", "title", "startDate", "endDate", "actualCost", "budget")
    .orderByAsc("startDate")
    .build();
```

### User Activity Analysis

```java
SpeedyQuery query = SpeedyQuery.from("users")
    .where(
        and(
            // Active users
            condition("active", eq(true)),
            // Recent activity
            condition("lastLoginDate", gte(LocalDate.now().minusDays(30).toString())),
            // Self-managed accounts
            condition("createdBy", eq("$updatedBy")),
            // Sufficient login count
            condition("loginCount", gte("$minimumLogins"))
        )
    )
    .select("id", "name", "lastLoginDate", "loginCount", "createdBy", "updatedBy")
    .orderByDesc("lastLoginDate")
    .build();
```

## Field Reference Rules

### Syntax Rules

- **JSON API**: Use `$fieldName` to reference another field
- **Java Client**: Use `"$fieldName"` to reference another field
- **Field Names**: Must match exactly with entity field names
- **Case Sensitivity**: Field names are case-sensitive

### Validation Rules

- **Field Existence**: Referenced fields must exist in the entity
- **Type Compatibility**: Fields should be of compatible types for comparison
- **Error Handling**: Invalid field references result in `NotFoundException`

### Supported Operators

All comparison operators support field references:

| Operator | JSON Example | Java Example |
|----------|--------------|--------------|
| Equal | `"field1": "$field2"` | `eq("$field2")` |
| Not Equal | `"field1": {"$ne": "$field2"}` | `ne("$field2")` |
| Less Than | `"field1": {"$lt": "$field2"}` | `lt("$field2")` |
| Greater Than | `"field1": {"$gt": "$field2"}` | `gt("$field2")` |
| Less Than or Equal | `"field1": {"$lte": "$field2"}` | `lte("$field2")` |
| Greater Than or Equal | `"field1": {"$gte": "$field2"}` | `gte("$field2")` |

## Best Practices

### 1. Use Descriptive Field Names

```java
// Good: Clear field names
condition("salePrice", lt("$regularPrice"))

// Avoid: Unclear field names
condition("price1", lt("$price2"))
```

### 2. Combine with Other Conditions

```java
// Good: Combine field references with literal values
SpeedyQuery query = SpeedyQuery.from("products")
    .where(
        and(
            condition("salePrice", lt("$regularPrice")),
            condition("active", eq(true)),
            condition("category", in("electronics", "computers"))
        )
    )
    .build();
```

### 3. Validate Field Existence

```java
// Ensure fields exist before using them in queries
public SpeedyQuery buildPriceComparisonQuery() {
    // Validate that both fields exist in the entity
    if (!entityMetadata.has("salePrice") || !entityMetadata.has("regularPrice")) {
        throw new IllegalArgumentException("Required fields not found in entity");
    }
    
    return SpeedyQuery.from("products")
        .where(condition("salePrice", lt("$regularPrice")))
        .build();
}
```

### 4. Use for Data Validation

```java
// Validate data integrity
SpeedyQuery query = SpeedyQuery.from("orders")
    .where(
        and(
            condition("startDate", lte("$endDate")),
            condition("totalAmount", gte("$subtotal"))
        )
    )
    .build();
```

## Error Handling

### Common Errors

1. **Field Not Found**: `NotFoundException` when referencing non-existent fields
2. **Type Mismatch**: Comparison errors when fields have incompatible types
3. **Invalid Syntax**: Parsing errors with malformed field references

### Debugging Tips

```java
// Enable pretty printing to see generated queries
SpeedyQuery query = SpeedyQuery.from("products")
    .where(condition("salePrice", lt("$regularPrice")))
    .prettyPrint()  // Logs the query structure
    .build();

// Validate field names
System.out.println("Available fields: " + entityMetadata.getAllFieldNames());
```

## Performance Considerations

- **Indexing**: Ensure referenced fields are properly indexed
- **Query Complexity**: Complex field reference queries may impact performance
- **Database Support**: Field references work with all supported databases (H2, PostgreSQL, MySQL)

For more information about querying, see the [Query Operations](query-operation.md) and [SpeedyQuery](speedy-query.md) documentation. 