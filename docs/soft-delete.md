# Soft Delete

Soft delete is an opt-in, per-entity mode where `DELETE` marks a row as deleted instead of removing
it. Soft-deleted rows are hidden from reads by default, can be restored, and — if the entity allows
it — permanently purged.

## Enabling soft delete

Annotate the entity with `@SpeedySoftDelete`, naming a marker field. The marker must be a boolean or
a temporal field (`DATE`, `TIMESTAMP`/`LocalDateTime`, `TIMESTAMP WITH ZONE`/`ZonedDateTime`).

```java
@Entity
@Table(name = "users")
@SpeedySoftDelete(field = "deletedAt", allowViewDeleted = true, allowHardDelete = true)
public class User extends AbstractBaseEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ... other fields
}
```

| Attribute           | Default | Effect                                                                          |
|---------------------|---------|---------------------------------------------------------------------------------|
| `field`             | —       | The marker field the engine sets on delete and clears on restore (required).    |
| `allowViewDeleted`  | `false` | Permits `$deleted=include` / `$deleted=only` read requests.                      |
| `allowHardDelete`   | `false` | Exposes the `$purge` permanent-delete endpoint.                                  |

Both gates are **off by default** — secure by default. The marker itself:

- **Temporal marker**: set to the current timestamp on delete, `NULL` on restore. "Not deleted"
  means the marker is `NULL`.
- **Boolean marker**: set to `true` on delete, `false` on restore. A boolean marker should be
  non-null (default `false`).

## Deleting

`DELETE /speedy/v1/User/$delete` behaves exactly as before from the client's perspective, but for a
soft-delete entity the engine sets the marker instead of removing the row. `PRE_DELETE` /
`POST_DELETE` events still fire. Deleting an already soft-deleted row returns `404` (it reads as
absent to normal CRUD, just like a hard-deleted row).

## Reading — the `$deleted` parameter

By default, soft-deleted rows are hidden from every read (`GET`, `POST /$query`, and GET-by-key).
Control visibility with `$deleted`:

| Value             | Meaning                                     |
|-------------------|---------------------------------------------|
| `exclude` (default) | Hide soft-deleted rows.                   |
| `include`         | Return live **and** soft-deleted rows.      |
| `only`            | Return **only** soft-deleted rows (a recycle-bin view). |

`include` and `only` require `@SpeedySoftDelete(allowViewDeleted = true)`; otherwise the request is
rejected with `403 Forbidden`.

**URL (GET):**

```http
[GET] /speedy/v1/User?$deleted=only
```

**JSON body (`$query`):**

```json
{
    "$from": "User",
    "$deleted": "include",
    "$where": { "type": "regular" }
}
```

The visibility filter is ANDed into your `$where`, so it composes with any other conditions.

## Restore

Restore un-deletes soft-deleted rows by clearing the marker. Available whenever soft delete is
enabled.

```http
[POST] /speedy/v1/User/$restore
```

**Request Body** (a primary-key array, same shape as `$delete`):

```json
[
    { "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab" }
]
```

Each key must identify a currently soft-deleted row (else `404`). The response returns the restored
(now live) rows.

## Purge

Purge permanently (hard) deletes rows, bypassing soft delete — for clearing the recycle bin or
honoring erasure requests. It requires `@SpeedySoftDelete(allowHardDelete = true)`; otherwise the
request is rejected with `403 Forbidden`.

```http
[POST] /speedy/v1/User/$purge
```

**Request Body** (a primary-key array):

```json
[
    { "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab" }
]
```

Each key must exist — live or soft-deleted — else `404`.

## Java client

```java
speedy.delete("User").key("id", id).execute();      // soft delete
speedy.restore("User").key("id", id).execute();      // un-delete
speedy.purge("User").key("id", id).execute();        // permanent delete

speedy.get("User").onlyDeleted().execute();          // recycle-bin view
speedy.get("User").includeDeleted().execute();       // live + deleted
speedy.query("User").includeDeleted().execute();
```

## Notes

- Restore and purge do **not** fire lifecycle events in this version (there are no
  `PRE_RESTORE`/`POST_PURGE` event types).
- Soft delete is entirely backend-neutral: it reuses the existing update/delete/query mechanisms, so
  it works across every persistence backend without backend-specific code.

<hr>
