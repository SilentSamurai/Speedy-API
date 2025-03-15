# Speedy Put API

create entity CRUD apis with writing a line of code

### Put Operations

#### Update Resource

update transaction object using primary key

**URL**

```http
[PUT] /speedy/v1/User/2b3c4d5e-6789-01ab-cdef-2345678901bc
```

**Request Body**

```json
{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "type": "ADMIN",
    "phoneNo": "9876543210"
}
// only non key fields allowed
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