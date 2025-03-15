# Speedy Post API

create entity CRUD apis with writing a line of code

### Post Operations

#### Bulk Create Resources

create multiple User

**URL**

```http
[POST] /speedy/v1/User
```

**Request Body**

```json
[
    {
        "name": "John Doe",
        "email": "john.doe@example.com",
        "type": "ADMIN",
        "phoneNo": "9876543210"
    }
    // other resource of same type
]
```

**Response**

```json
{
    "payload": [
        {
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab"
        },
        {
            "id": "2b3c4d5e-6789-01ab-cdef-2345678901bc"
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>