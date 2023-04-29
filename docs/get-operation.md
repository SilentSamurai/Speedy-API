# Speedy Get API

create entity CRUD apis with writing a line of code

### Get Operations

#### Get all resource

retrieve all resource in a single resource type / entity

**URL**

```javascript
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
            "notes": ""
        },
        {
            "id": "transaction-2",
            "type": "CREDIT",
            "amount": 674,
            "currency": "INR",
            "date": "2023-04-27T13:53:49+00:00",
            "quantity": 4,
            "notes": ""
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
            "amount": 250,
            "currency": "INR",
            "date": "2023-04-27T13:53:49+00:00",
            "quantity": 10,
            "notes": ""
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
<hr>

#### Filter with Comparison Operator

retrieve multiple resource with filter with resource fields

**URL**

get all transaction where amount > 100

```javascript
GET / speedy / v1 / Transaction(amount > 100)
```

get all transaction where amount <= 50

```javascript
GET / speedy / v1 / Transaction(amount <= 50)
```

get all transaction where amount != 50

```javascript
GET / speedy / v1 / Transaction(amount != 50)
```

<hr>

#### Filter with Contains Operator

retrieve multiple resource with in operators

**URL**

```javascript
GET / speedy / v1 / Transaction(type <> ['CREDIT', 'DEBIT'])
```

```javascript
GET / speedy / v1 / Transaction(cost <> [23, 50, 72])
```

eliminate multiple resource with not in operators

**URL**

```javascript
GET / speedy / v1 / Transaction(type <!> ['TRANSFET'])
```

```javascript
GET / speedy / v1 / Transaction(cost <!> [23, 50, 72])
```