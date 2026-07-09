# Speedy Update APIs — PUT vs PATCH

Speedy exposes two update verbs on the `$update` endpoint, and they follow standard HTTP
semantics:

| Verb    | Semantics       | Omitted non-key fields        | Required fields          |
|---------|-----------------|-------------------------------|--------------------------|
| `PATCH` | Partial update  | Left **unchanged**            | Not enforced             |
| `PUT`   | Full replace    | **Reset to null** (if nullable) | **Enforced** (400 if missing) |

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

<hr>
