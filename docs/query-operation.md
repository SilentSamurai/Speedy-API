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
| $matches | =*     | Pattern Matching       | `{ "name": { "$matches": "John*" } }`    |

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