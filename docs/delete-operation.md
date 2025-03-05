# Speedy Delete API

create entity CRUD apis with writing a line of code

### Delete Operations

#### Bulk Delete

delete multiple resource

**URL**

```http
[DELETE] /speedy/v1/User
```

**Request Body**

```json
[
    {
        "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab"
    },
    {
        "id": "2b3c4d5e-6789-01ab-cdef-2345678901bc"
    }
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