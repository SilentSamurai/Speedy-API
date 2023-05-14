# Speedy Delete API

create entity CRUD apis with writing a line of code

### Delete Operations

#### Bulk Delete

delete multiple transactions

**URL**

```http
[DELETE] /speedy/v1/Transaction
```

**Request Body**

```json
[
    {
        "id": "transaction-1"
    },
    {
        "id": "transaction-2"
    }
]
```

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1"
        },
        {
            "id": "transaction-2"
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>