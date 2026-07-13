# Speedy Update APIs — PUT vs PATCH

Speedy exposes two update verbs on the `$update` endpoint, and they follow standard HTTP
semantics:

| Verb    | Semantics      | Omitted non-key fields          | Required fields               |
|---------|----------------|---------------------------------|-------------------------------|
| `PATCH` | Partial update | Left **unchanged**              | Not enforced                  |
| `PUT`   | Full replace   | **Reset to null** (if nullable) | **Enforced** (400 if missing) |

Both address a single record by its primary key, which is carried **in the request body**
alongside the fields to write:

```http
[PUT]   /speedy/v1/Supplier/$update
[PATCH] /speedy/v1/Supplier/$update
```

## PATCH — partial update

Only the fields present in the payload are written; every other column keeps its current
value.

**Request Body**

```json
{
    "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
    "name": "John Doe"
}
```

Here only `name` changes; `address`, `email`, `phoneNo`, etc. are untouched.

## PUT — full replace

The payload is treated as the **complete representation** of the resource:

- **All required fields must be present** — a missing required field is rejected with `400`.
- **Omitted nullable non-key fields are reset to `null`** (columns that are read-only or
  database-generated are left as-is).
- The operation is **idempotent** — applying the same body twice yields the same state.

**Request Body**

```json
{
    "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
    "name": "John Doe",
    "phoneNo": "9876543210",
    "altPhoneNo": "9876543211"
}
```

Because `address` and `email` (nullable) are omitted, PUT clears them; `name`, `phoneNo`,
and `altPhoneNo` (required) must be supplied.

**Response** (both verbs return the persisted row)

```json
{
    "payload": [
        {
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
            "name": "John Doe",
            "address": null,
            "email": null,
            "phoneNo": "9876543210",
            "altPhoneNo": "9876543211"
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

> Only non-key fields are writable in the body; the primary key identifies the record.

## Missing resource

For both verbs, a request whose primary key is **complete but does not match any row**
returns `404 Not Found`. An **incomplete** primary key returns `400 Bad Request`.

## Optimistic concurrency — `If-Match`

For a single-item PATCH/PUT on an entity with a `@SpeedyETag` field, an `If-Match` header is
checked against the row's current `ETag` before the write; a stale tag (or a missing row) returns
`412 Precondition Failed`, and a successful write returns the **new** `ETag`. See
[Conditional Requests / ETags](conditional-requests.md) for the opt-in, strategies, and the
`If-Match: *` / multi-item rules.

## Bulk Update / Bulk Replace

Both verbs also accept a JSON **array** of items on the same `$update` endpoint — one call
updates (or replaces) several records at once. Each array element is a full request body
(primary key + fields), with the same PATCH/PUT semantics described above applied per item.

```http
[PATCH] /speedy/v1/Supplier/$update
[PUT]   /speedy/v1/Supplier/$update
```

**Request Body**

```json
[
    {
        "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
        "name": "John Doe"
    },
    {
        "id": "2b3c4d5e-6789-01ab-cdef-2345678901bc",
        "name": "Jane Doe"
    }
]
```

**Response**

```json
{
    "payload": [
        {
            "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
            "name": "John Doe",
            "address": null,
            "email": null,
            "phoneNo": "9876543210",
            "altPhoneNo": "9876543211"
        },
        {
            "id": "2b3c4d5e-6789-01ab-cdef-2345678901bc",
            "name": "Jane Doe",
            "address": null,
            "email": null,
            "phoneNo": "9876543212",
            "altPhoneNo": "9876543213"
        }
    ],
    "pageCount": 1,
    "pageIndex": 0
}
```

A single-object body (not wrapped in an array) is still accepted as shorthand for a
one-item update — this is how the single-record examples above work.

### Fine-grained bulk control — `@SpeedyBulk`

By default, entities **reject** a multi-item array (`400 Bad Request`) — a single-element
array or bare object is always allowed. Use `@SpeedyBulk` to opt into all bulk write operations:

```java
@SpeedyBulk
public class Supplier { ...
}
```

To allow only a subset, specify `BulkOperation` values. `UPDATE` controls `PATCH`; `REPLACE`
controls `PUT` so the two update modes can be independently enabled.

```java
@SpeedyBulk({BulkOperation.CREATE, BulkOperation.DELETE})
public class ImportableSupplier { ...
}

@SpeedyBulk(BulkOperation.CREATE)
public class CreateOnlyResource { ...
}
```

`BulkOperation.ALL` is equivalent to `@SpeedyBulk`; `@SpeedyBulk({})` explicitly disables bulk.
The same operation-specific rule governs `$create`, `$update` (PATCH and PUT), and `$delete`.

### Atomicity

Every bulk write — `$create`, `$update` (PATCH and PUT), and `$delete` — runs in a single
transaction. If **any** item fails (validation, a missing row, a lifecycle event, or a constraint
violation), the whole request is rejected and **nothing is committed**. The response carries that
failure's HTTP status (e.g. `400`, `404`, `409`); there is no partial-success result.

To apply a subset of items independently, send them as separate requests.

<hr>
