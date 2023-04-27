# Speedy Put API

create entity CRUD apis with writing a line of code

### Put Operations

#### Update Resource

update transaction object using primary key

**URL**

```javascript
PUT / speedy / v1 / Transaction(id = 'transaction-1')
```

**Response Body**

```json
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
// only non key fields allowed
```

**Response**

```json
{
    "payload": {
        "id": "transaction-1",
        "type": "DEBIT",
        "amount": 250,
        "currency": "INR",
        "date": "2023-04-27T13:53:49+00:00",
        "quantity": 10,
        "product": {
            "id": "product-1",
            "name": "Product 1"
        },
        "vendor": {
            "id": "vendor-1",
            "name": "Vendor 1"
        },
        "customer": {
            "id": "customer-1",
            "name": "Customer 1"
        },
        "notes": ""
    },
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>