# Speedy Get API

create entity CRUD apis with writing a line of code

## Get Operations

### Category API

#### Get All Categories
retrieve all categories for resource server
##### Request

```http
GET /speedy/v1/Category
get all categories
====
```

##### Response

```json
{
    "payload": [
        {
            "name": "Generic Category Name",
            "id": "category-id-1"
        }
    ],
    "pageCount": 0,
    "pageIndex": 0
}
```

#### Get a single Category
retrieve a single category from resource server
##### Request

```http
GET /speedy/v1/Category(id='category-id-1')
```

##### Response

```json
{
    "payload": {
        "name": "Generic Category Name",
        "id": "category-id-1"
    },
    "pageCount": 0,
    "pageIndex": 0
}
```

