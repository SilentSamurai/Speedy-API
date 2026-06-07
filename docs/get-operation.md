# Speedy Get API

create entity CRUD apis without writing a line of code

### Get Operations

#### Get all resource

retrieve all resource in a single resource type / entity

**URL**

```http
[GET] /speedy/v1/User
```

**Response**

```json
{
    "payload": [
        {
            "lastLoginDate": null,
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
            "name": "John Doe",
            "updatedAt": "2024-02-28T15:00:00",
            "email": "john.doe@example.com",
            "loginCount": 0,
            "type": "ADMIN",
            "phoneNo": "9876543210",
            "createdAt": "2024-02-28T12:00:00",
            "deletedAt": null
        },
        {
            "lastLoginDate": null,
            "id": "2b3c4d5e-6789-01ab-cdef-2345678901bc",
            "name": "Jane Smith",
            "updatedAt": "2024-02-28T16:30:00",
            "email": "jane.smith@example.com",
            "loginCount": 0,
            "type": "USER",
            "phoneNo": "8765432109",
            "createdAt": "2024-02-28T13:15:00",
            "deletedAt": null
        },
        ...
    ],
    "pageIndex": 0,
    "pageSize": 2,
    "totalCount": 2,
    "totalPages": 1
}
```

<hr>

#### Filter via Key Field

retrieve a single resource with primary key

**URL**

```http
[GET] /speedy/v1/User?id=1a2b3c4d-5678-90ab-cdef-1234567890ab
```

**Response**

```json
{
    "payload": [
        {
            "lastLoginDate": null,
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
            "name": "John Doe",
            "updatedAt": "2024-02-28T15:00:00",
            "email": "john.doe@example.com",
            "loginCount": 0,
            "type": "ADMIN",
            "phoneNo": "9876543210",
            "createdAt": "2024-02-28T12:00:00",
            "deletedAt": null
        }
    ],
    "pageSize": 1,
    "pageIndex": 0,
    "totalCount": 1,
    "totalPages": 1
}
```

<hr>

#### Filter via Non Key Field

retrieve multiple resource with filter with resource fields

**URL**

```http
[GET] /speedy/v1/User ? type = "ADMIN"
```

get all admin users

**Response**

```json
{
    "payload": [
        {
            "lastLoginDate": null,
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
            "name": "John Doe",
            "updatedAt": "2024-02-28T15:00:00",
            "email": "john.doe@example.com",
            "loginCount": 0,
            "type": "ADMIN",
            "phoneNo": "9876543210",
            "createdAt": "2024-02-28T12:00:00",
            "deletedAt": null
        }
    ],
    "pageSize": 1,
    "pageIndex": 0
}
```

<hr>

#### Filter with Multiple Fields

retrieve multiple resource with filter with resource fields

**URL**

```http
[GET] /speedy/v1/User ? type = "ADMIN" & name = "John Doe"
```

get all admin user with name John Doe

**Response**

```json
{
    "payload": [
        {
            "lastLoginDate": null,
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
            "name": "John Doe",
            "updatedAt": "2024-02-28T15:00:00",
            "email": "john.doe@example.com",
            "loginCount": 0,
            "type": "ADMIN",
            "phoneNo": "9876543210",
            "createdAt": "2024-02-28T12:00:00",
            "deletedAt": null
        }
    ],
    "pageSize": 1,
    "pageIndex": 0
}
```

<hr>

#### Paged Request

the paging the request

**URL**

```http
[GET] /speedy/v1/User ? $pageSize = 100 & $pageNo = 0
```

```http
[GET] /speedy/v1/User ? $pageSize = 10 & $pageNo = 2
```

**Response**

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
    "pageSize": 2,
    "pageIndex": 0
    // page starts from 0 indexed
}
```

#### Ordered Request

order the request with different columns

```http
[GET] /speedy/v1/User ? $orderByAsc = 'createdAt' & $orderByDesc = 'amount'
```

**Response**

```json
{
    "payload": [
        // ordered request
        {
            ...
        },
        {
            ...
        }
    ],
    "pageSize": 2,
    "pageIndex": 0
}
```

<hr>

#### Entity Expansion

include related entities in your results using the `$expand` parameter

**Simple Expansion**

```http
[GET] /speedy/v1/Inventory ? $expand = 'Product'
```

**Multi-Level Expansion**

```http
[GET] /speedy/v1/Inventory ? $expand = 'Product' & $expand = 'Product.Category'
```

**Complex Multi-Level Expansion**

```http
[GET] /speedy/v1/Inventory ? $expand = 'Product' & $expand = 'Product.Category' & $expand = 'Product.Category.Supplier' & $expand = 'Procurement' & $expand = 'Procurement.Product'
```

**Response with Expanded Entities**

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
                        "name": "TechCorp"
                    }
                }
            },
            "procurement": {
                "id": "procurement-1",
                "amount": 50000,
                "product": {
                    "id": "product-1",
                    "name": "Laptop"
                }
            }
        }
    ],
    "pageSize": 1,
    "pageIndex": 0
}
```

For detailed information about multi-level expansions, see [Multi-Level Expansions](multi-level-expansions.md).

<hr>

#### Select Fields

Limit the response to specific fields using the `$select` parameter.

```http
[GET] /speedy/v1/User ? $select=id,name,email,type
```

**Response**

```json
{
    "payload": [
        {
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
            "name": "John Doe",
            "email": "john.doe@example.com",
            "type": "ADMIN"
        }
    ],
    "pageIndex": 0,
    "pageSize": 10,
    "totalCount": 1,
    "totalPages": 1
}
```

Only the fields listed in `$select` are included in the response payload. All other entity fields are omitted.

**Performance note:** When `$select` is specified, the database query fetches only the requested columns (plus primary
keys and required foreign keys) rather than `SELECT *`. This reduces database I/O, network transfer, and memory usage,
especially for entities with many columns or large text/blob fields.

**Combining `$select` with `$expand`:**

```http
[GET] /speedy/v1/Inventory ? $select=id,quantity & $expand=Product
```

When `$expand` is also specified, the select applies to the root entity while expanded entities return all their fields.

<hr>

#### Count Queries

Return only the entity count without loading records using `$select=$count`.

```http
[GET] /speedy/v1/User ? $select=$count
```

**Response**

```json
{
    "count": 42
}
```

This executes a lightweight `COUNT(*)` query without fetching or serializing any entity data.

<hr>

#### Page Size Limits

GET requests respect configurable page size limits:

- **No `$pageSize` specified** — the server uses its configured `defaultPageSize`, clamped to `maxPageSize`.
- **`$pageSize` specified** — the value must not exceed `maxPageSize`. If it does, the server responds with **400 Bad
  Request**.

```http
GET /speedy/v1/User ? $pageSize=5000
```

```json
{
    "status": 400,
    "message": "Bad Request: page size exceeds maximum allowed",
    "timestamp": "2026-05-29T12:00:00Z"
}
```

Configure the limits by implementing `ISpeedyConfiguration.maxPageSize()` and `ISpeedyConfiguration.defaultPageSize()`
in your application.