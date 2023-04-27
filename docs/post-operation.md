# Speedy Post API

create entity CRUD apis with writing a line of code

### Post Operations

#### Bulk Create Resources

create multiple transaction

**URL**

```javascript
POST / speedy / v1 / Transaction
```

**Response Body**

```json
[
    {
        "type": "DEBIT",
        "amount": 250,
        "currency": "INR",
        "date": "2023-04-27T13:53:49+00:00",
        "quantity": 10,
        "product": {
            "id": "product-1"
        },
        "vendor": {
            "id": "vendor-1"
        },
        "customer": {
            "id": "customer-1"
        },
        "notes": ""
    }
    // other resource of same type
]
```

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-3"
        },
        {
            "id": "transaction-4"
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>