# Speedy Get API

create entity CRUD apis without writing a line of code

### Get Operations

#### Get all resource

retrieve all resource in a single resource type / entity

**URL**

```http
GET / speedy / v1 / Transaction
```

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "type": "DEBIT",
            "amount": 250,
            "currency": "INR",
            "date": "2023-04-27T13:53:49+00:00",
            "quantity": 10,
            "notes": "",
            "procurement": {
                "id": "procurement-1",
                "product": "Product 1"
            },
            "account": {
                "id": "account-1",
                "name": "New Transaction Account",
                "type": "Cash"
            }
        },
        {
            "id": "transaction-2",
            "type": "CREDIT",
            "amount": 674,
            "currency": "INR",
            "date": "2023-04-27T13:53:49+00:00",
            "quantity": 4,
            "notes": "",
            "procurement": {
                "id": "procurement-1",
                "product": "Product 1"
            },
            "account": {
                "id": "account-2",
                "name": "New Savings Account",
                "type": "Electronic"
            }
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>

#### Filter via Key Field

retrieve a single resource with primary key

**URL**

```javascript
GET / speedy / v1 / Transaction(id = 'transaction-1')
```

**Response**

```json
{
    "payload": {
        "id": "transaction-1",
        "type": "DEBIT",
        "amount": 250,
        "currency": "INR",
        "quantity": 10,
        "account": {
            "id": "account-1",
            "name": "New Transaction Account",
            "type": "Cash"
        },
        "procurement": {
            "id": "procurement-1",
            "product": "Product 1"
        },
        "invoices": [
            {
                "id": "inv-1",
                "date": "2023-04-27T13:53:49+00:00",
                "discount": 23
            }
        ],
        "notes": ""
    },
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>

#### Filter via Non Key Field

retrieve multiple resource with filter with resource fields

**URL**

```javascript
GET / speedy / v1 / Transaction(type = 'DEBIT')
```

get all transaction of DEBIT type

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "type": "DEBIT",
            ...
        },
        {
            "id": "transaction-5",
            "type": "DEBIT",
            ...
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

```javascript
GET / speedy / v1 / Transaction(type = 'DEBIT', currency = 'INR')
```

get all transaction of DEBIT type and currency INR

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "type": "DEBIT",
            "currency": "INR",
            ...
        },
        {
            "id": "transaction-7",
            "type": "DEBIT",
            "currency": "INR",
            ...
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>

#### Filter with And Operator

retrieve multiple resource with filter with resource fields

**URL**

```javascript
GET / speedy / v1 / Transaction(type = 'DEBIT' & currency = 'INR')
```

get all transaction of DEBIT type and currency INR
<hr>

#### Filter with Or Operator

retrieve multiple resource with filter with resource fields

**URL**

```javascript
GET / speedy / v1 / Transaction(currency = 'USD' | currency = 'EUR')
```

get all transaction of currency USD or currency EUR

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "currency": "INR",
            ...
        },
        {
            "id": "transaction-12",
            "currency": "USD",
            ...
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>

#### Filter with Comparison Operator

retrieve multiple resource with filter with resource fields

**URL**

get all transaction where amount > 100

```javascript
GET / speedy / v1 / Transaction(amount > 100)
```

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "amount": 100,
            ...
        },
        {
            "id": "transaction-11",
            "amount": 150,
            ...
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

**URL**

get all transaction where amount <= 50

```javascript
GET / speedy / v1 / Transaction(amount <= 50)
```

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "amount": 50,
            ...
        },
        {
            "id": "transaction-11",
            "amount": 20,
            ...
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

**URL**

get all transaction where amount != 50

```javascript
GET / speedy / v1 / Transaction(amount != 50)
```

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "amount": 20,
            ...
        },
        {
            "id": "transaction-11",
            "amount": 100,
            ...
        },
        {
            "id": "transaction-11",
            "amount": 150,
            ...
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>

#### Filter with Contains Operator

retrieve multiple resource with in operators

**URL**

```javascript
GET / speedy / v1 / Transaction(type < > ['CREDIT', 'DEBIT'])
```

```javascript
GET / speedy / v1 / Transaction(cost < > [23, 50, 72])
```

eliminate multiple resource with not in operators

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "type": "CREDIT",
            ...
        },
        {
            "id": "transaction-11",
            "amount": "DEBIT",
            ...
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

**URL**

```javascript
GET / speedy / v1 / Transaction(type < ! > ['TRANSFER'])
```

```javascript
GET / speedy / v1 / Transaction(cost < ! > [23, 50, 72])
```

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "type": "CREDIT",
            ...
        },
        {
            "id": "transaction-11",
            "amount": "DEBIT",
            ...
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

<hr>

#### Filter with Association

retrieve multiple resource with foreign keys
**URL**

```javascript
GET / speedy / v1 / Transaction(procurement.id = 'procurement-1')
```

```javascript
GET / speedy / v1 / Transaction(procurement.product = 'Product 1')
```

```javascript
GET / speedy / v1 / Transaction(account.type = 'cash')
```

**Response**

```json
{
    "payload": [
        {
            "id": "transaction-1",
            "procurement": {
                ...
                "product": "Product 1"
            },
            ...
        },
        {
            "id": "transaction-11",
            "procurement": {
                ...
                "product": "Product 1"
            },
            ...
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```