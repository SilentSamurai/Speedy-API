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
    "totalPageCount": 1
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
    "pageCount": 1,
    "pageIndex": 0
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
    "pageCount": 1,
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
    "pageCount": 1,
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
    "pageCount": 2,
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
    "pageCount": 2,
    "pageIndex": 0
}
```