# Speedy Query API

This document provides detailed information on how to use the Speedy Query API to query tables with different operators.

## Query Operations

### Query Resource by Field

**Request**

To query a user by name, use the following request:

```http
POST /speedy/v1/User/$query
Accept: application/json
Content-Type: application/json

{
    "$where": {
        "name": "John Doe"
    }
}
```

Alternatively, you can use the $eq operator:

```http
POST /speedy/v1/User/$query
Accept: application/json
Content-Type: application/json

{
    "$where": {
        "name": {
           "$eq": "John Doe" 
        }
    }
}
```

**Response**

```json
{
    "payload": [
        {
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
            "name": "John Doe",
            "updatedAt": "2024-02-28T15:00:00",
            "email": "john.doe@example.com",
            ...
        }
    ],
    "pageIndex": 0,
    "pageSize": 1,
    "totalPageCount": 1
}
```

### Query Using Comparison Operators

You can use different comparison operators for numerical and string fields.

**Request**

```http
POST /speedy/v1/User/$query  
Accept: application/json  
Content-Type: application/json  

{
    "$where": {
        "cost": {
            "$eq": "0",
            "$ne": "100",
            "$lt": "50",
            "$gt": "200"
        }
    }
}
```

**Response**

```json
{
    "payload": [
        {
            ...
        }
    ],
    "pageIndex": 0,
    "pageSize": 2
}
```

#### Supported Operators

| Operator | Symbol | Description            | Example                                  |
|----------|--------|------------------------|------------------------------------------|
| $eq      | =      | Equals                 | `{ "cost": { "$eq": 100 } }`             |
| $ne      | !=     | Not Equals             | `{ "cost": { "$ne": 100 } }`             |
| $lt      | <      | Less Than              | `{ "cost": { "$lt": 50 } }`              |
| $gt      | >      | Greater Than           | `{ "cost": { "$gt": 50 } }`              |
| $lte     | <=     | Less Than or Equals    | `{ "cost": { "$lte": 50 } }`             |
| $gte     | >=     | Greater Than or Equals | `{ "cost": { "$gte": 50 } }`             |
| $in      | <>     | In Array               | `{ "cost": { "$in": [50, 100, 150] } }`  |
| $nin     | <!>    | Not In Array           | `{ "cost": { "$nin": [50, 100, 150] } }` |
| $matches | =*     | Pattern Matching       | `{ "name": { "$matches": "*John*" } }`    |

**Pattern Matching Syntax:**
- `*` = 0 or more characters/spaces
- `?` = exactly 1 character/space
- Examples:
  - `"*John*"` matches "John", "Johnny", "John Doe", "My John", etc.
  - `"John*"` matches "John", "Johnny", "John Doe", etc.
  - `"*John"` matches "John", "My John", "The John", etc.

### Query Using Field References

Speedy supports field-to-field comparisons using the `$` prefix. This allows you to compare values between different fields in the same entity or related entities.

#### Basic Field Reference

**Request**

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

This query finds products where the sale price equals the regular price.

#### Field Reference with Operators

**Request**

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

This query finds products where the sale price is less than the regular price.

#### Complex Field Reference Examples

**Compare Date Fields**

```http
POST /speedy/v1/Order/$query
Accept: application/json
Content-Type: application/json

{
    "$where": {
        "startDate": {
            "$lte": "$endDate"
        }
    }
}
```

**Compare User Fields**

```http
POST /speedy/v1/User/$query
Accept: application/json
Content-Type: application/json

{
    "$where": {
        "createdBy": "$updatedBy"
    }
}
```

**Field Reference with Logical Operators**

```http
POST /speedy/v1/Product/$query
Accept: application/json
Content-Type: application/json

{
    "$where": {
        "$and": [
            { "salePrice": { "$lt": "$regularPrice" } },
            { "stockQuantity": { "$gt": "$minimumStock" } }
        ]
    }
}
```

#### Field Reference Rules

- **Prefix**: All field references must start with `$`
- **Field Names**: Use the exact field names as defined in your entity
- **Supported Operators**: All comparison operators support field references
- **Validation**: Invalid field references will result in a `NotFoundException`

#### Field Reference Examples

| Use Case | Query | Description |
|----------|-------|-------------|
| Price Comparison | `{ "salePrice": { "$lt": "$regularPrice" } }` | Find products on sale |
| Date Range | `{ "startDate": { "$lte": "$endDate" } }` | Valid date ranges |
| User Tracking | `{ "createdBy": "$updatedBy" }` | Self-updated records |
| Inventory Check | `{ "currentStock": { "$gte": "$minimumStock" } }` | Sufficient inventory |

### Entity Expansion

Include related entities in your query results using the `$expand` parameter.

#### Simple Entity Expansion

```http
POST /speedy/v1/Inventory/$query
Accept: application/json
Content-Type: application/json

{
    "$expand": ["Product", "Procurement"]
}
```

#### Multi-Level Expansion

Use dot notation to expand nested relationships:

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

#### Complex Multi-Level Expansion

```http
POST /speedy/v1/Inventory/$query
Accept: application/json
Content-Type: application/json

{
    "$expand": [
        "Product",
        "Product.Category",
        "Product.Category.Supplier",
        "Procurement",
        "Procurement.Product",
        "Procurement.Product.Category"
    ]
}
```

#### Expansion with Other Query Parameters

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
        "Product.Category.Supplier"
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

#### Multi-Level Expansion Rules

- **Dot Notation**: Use dots (`.`) to separate entity levels in the expansion path
- **Path Validation**: Each segment in the path must be a valid entity association
- **Backward Compatibility**: Simple entity names (e.g., `"Product"`) still work
- **Performance**: Deep expansions may impact query performance
- **Validation**: Invalid expansion paths will result in a `BadRequestException`

#### Multi-Level Expansion Examples

| Use Case | Expansion | Description |
|----------|-----------|-------------|
| Product with Category | `["Product", "Product.Category"]` | Include product and its category |
| Deep Supplier Chain | `["Product.Category.Supplier.Address"]` | Include complete supplier chain |
| Multiple Paths | `["Product.Category", "Procurement.Product"]` | Different expansion paths |
| Selective Expansion | `["Product", "Product.Category"]` | Only expand specific paths |

### Query Using Logical Operator - AND and OR

Combine multiple conditions using logical operators.

**And Request**

```http
POST /speedy/v1/User/$query  
Accept: application/json  
Content-Type: application/json  

{
    "$where": {
        "$and": [
            { "id": "1" },
            { "desc": "desc1" }
        ]
    }
}
```

**OR Request**

```http
POST /speedy/v1/User/$query  
Accept: application/json  
Content-Type: application/json  

{
    "$where": {
        "$or": [
            { "id": "1" },
            { "desc": "desc1" }
        ]
    }
}
```

**AND-OR Request**

```http
POST /speedy/v1/User/$query  
Accept: application/json  
Content-Type: application/json  

{
    "$where": {
        "$or": [
            "$and":[
                { "id": "1" },
                { "desc": "desc1" }
            ]
            "$and":[
                { "id": "1" },
                { "desc": "desc1" }
            ]
        ]
    }
}
```

#### Logical Operators

| Operator | Description                        | Example                                                 |
|----------|------------------------------------|---------------------------------------------------------|
| $and     | Logical AND (All conditions match) | `{ "$and": [{ "role": "admin" }, { "active": true }] }` |
| $or      | Logical OR (Any condition matches) | `{ "$or": [{ "role": "admin" }, { "role": "user" }] }`  |

### Paging Query

Control the size of the request using paging:
**Request**

```http
POST /speedy/v1/User/$query
Accept: application/json
Content-Type: application/json

{
    "$page": {
        "$index": 0,
        "$size": 2
    }
}
```

**Request**

```json
{
    "payload": [
        {
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
            "name": "John Doe",
            "updatedAt": "2024-02-28T15:00:00",
            "email": "john.doe@example.com",
            ...
        }
    ],
    "pageIndex": 0,
    "pageSize": 2
}
```

### Order By Query

Sort the results using the orderBy parameter:
**Request**

```http
POST /speedy/v1/User/$query
Accept: application/json
Content-Type: application/json

{
    "$orderBy": {
        "createdAt": "ASC"
    },
}
```

**Request**

```json
{
    "payload": [
        {
            ...
        },
        {
            ...
        }
    ],
    "pageIndex": 0,
    "pageSize": 2
}
```