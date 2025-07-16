# Multi-Level Expansions

Speedy API supports multi-level entity expansions using dot notation, allowing you to include deeply nested related entities in your query results.

## Overview

Multi-level expansions enable you to traverse entity relationships and include data from related entities at any depth. This is particularly useful for complex data models where entities have multiple levels of relationships.

## Basic Syntax

Use dot notation (`.`) to separate entity levels in the expansion path:

```
Entity.RelatedEntity.NestedEntity
```

## Examples

### Simple Multi-Level Expansion

```java
// Expand product and its category
SpeedyQuery query = SpeedyQuery.from("inventory")
    .expand("Product")
    .expand("Product.Category")
    .build();
```

### Deep Nested Expansion

```java
// Expand complete supplier chain
SpeedyQuery query = SpeedyQuery.from("inventory")
    .expand("Product")
    .expand("Product.Category")
    .expand("Product.Category.Supplier")
    .expand("Product.Category.Supplier.Address")
    .build();
```

### Multiple Expansion Paths

```java
// Different expansion paths from the same root entity
SpeedyQuery query = SpeedyQuery.from("inventory")
    .expand("Product")
    .expand("Product.Category")
    .expand("Procurement")
    .expand("Procurement.Product")
    .expand("Procurement.Product.Category")
    .build();
```

## HTTP API Examples

### Basic Multi-Level Expansion

```http
POST /speedy/v1/Inventory/$query
Accept: application/json
Content-Type: application/json

{
    "$expand": [
        "Product",
        "Product.Category",
        "Product.Category.Supplier"
    ]
}
```

### Complex Multi-Level Expansion

```http
POST /speedy/v1/Inventory/$query
Accept: application/json
Content-Type: application/json

{
    "$where": {
        "quantity": { "$gt": 0 }
    },
    "$select": ["id", "quantity", "location"],
    "$expand": [
        "Product",
        "Product.Category",
        "Product.Category.Supplier",
        "Product.Category.Supplier.Address",
        "Procurement",
        "Procurement.Product",
        "Procurement.Product.Category"
    ],
    "$orderBy": {
        "quantity": "DESC"
    },
    "$page": {
        "$index": 0,
        "$size": 10
    }
}
```

## Response Structure

When using multi-level expansions, the response will include nested objects representing the expanded entities:

```json
{
    "payload": [
        {
            "id": "inventory-1",
            "quantity": 100,
            "location": "Warehouse A",
            "product": {
                "id": "product-1",
                "name": "Laptop",
                "price": 999.99,
                "category": {
                    "id": "category-1",
                    "name": "Electronics",
                    "supplier": {
                        "id": "supplier-1",
                        "name": "TechCorp",
                        "address": {
                            "id": "address-1",
                            "street": "123 Tech Street",
                            "city": "Tech City"
                        }
                    }
                }
            },
            "procurement": {
                "id": "procurement-1",
                "amount": 50000,
                "product": {
                    "id": "product-1",
                    "name": "Laptop",
                    "category": {
                        "id": "category-1",
                        "name": "Electronics"
                    }
                }
            }
        }
    ],
    "pageIndex": 0,
    "pageSize": 10
}
```

## Rules and Best Practices

### Expansion Rules

- **Dot Notation**: Use dots (`.`) to separate entity levels
- **Path Validation**: Each segment must be a valid entity association
- **Backward Compatibility**: Simple entity names (e.g., `"Product"`) still work
- **Case Sensitivity**: Entity names are case-sensitive
- **Validation**: Invalid expansion paths result in `BadRequestException`

### Performance Considerations

- **Query Complexity**: Deep expansions increase query complexity
- **Data Volume**: Multi-level expansions can significantly increase response size
- **Database Load**: Complex joins may impact database performance
- **Memory Usage**: Large nested objects consume more memory

### Best Practices

1. **Selective Expansion**: Only expand the paths you need
2. **Field Selection**: Use `$select` to limit returned fields
3. **Pagination**: Always use pagination for large datasets
4. **Testing**: Test expansion paths with your specific data model
5. **Monitoring**: Monitor query performance with complex expansions

## Common Use Cases

### E-commerce Inventory

```java
// Get inventory with complete product information
SpeedyQuery query = SpeedyQuery.from("inventory")
    .expand("Product")
    .expand("Product.Category")
    .expand("Product.Category.Supplier")
    .expand("Product.Category.Supplier.Address")
    .where(condition("quantity", gt(0)))
    .build();
```

### Order Management

```java
// Get orders with customer and product details
SpeedyQuery query = SpeedyQuery.from("orders")
    .expand("Customer")
    .expand("Customer.Address")
    .expand("OrderItems")
    .expand("OrderItems.Product")
    .expand("OrderItems.Product.Category")
    .where(condition("status", eq("pending")))
    .build();
```

### User Management

```java
// Get users with complete profile information
SpeedyQuery query = SpeedyQuery.from("users")
    .expand("Profile")
    .expand("Profile.Address")
    .expand("Department")
    .expand("Department.Manager")
    .expand("Permissions")
    .where(condition("active", eq(true)))
    .build();
```

## Error Handling

### Invalid Expansion Path

```json
{
    "error": "Bad Request",
    "message": "Invalid expansion path: Product.NonExistentEntity",
    "status": 400
}
```

### Circular Reference

```json
{
    "error": "Bad Request", 
    "message": "Circular reference detected in expansion path",
    "status": 400
}
```

### Entity Not Found

```json
{
    "error": "Not Found",
    "message": "Entity 'NonExistentEntity' not found",
    "status": 404
}
```

## Implementation Details

### ExpansionPathTracker

The system uses an `ExpansionPathTracker` to manage multi-level expansions:

- **Path Tracking**: Maintains current expansion path using a stack
- **Dot Notation Support**: Generates paths like `Inventory.Product.Category`
- **Backward Compatibility**: Supports both dot notation and entity-based expansions
- **Recursive Processing**: Handles nested entity expansions efficiently

### Validation

Expansion paths are validated to ensure:
- Each segment is a valid entity association
- No empty segments in the path
- Proper dot notation formatting
- No circular references

### Serialization

The `JSONSerializerV2` handles serialization of multi-level expansions:
- Supports dot notation expansions
- Maintains backward compatibility
- Efficient field filtering
- Proper nested object structure

## Related Documentation

- [Query Operations](query-operation.md) - General query operations
- [SpeedyQuery](speedy-query.md) - Java client query builder
- [Field References](field-references.md) - Field-to-field comparisons
- [Getting Started](getting-started.md) - Basic setup and configuration 