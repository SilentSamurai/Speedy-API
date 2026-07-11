# Conditional Requests / ETags

Speedy supports HTTP conditional requests — `ETag` generation, cache validation via
`If-None-Match`, and optimistic concurrency control via `If-Match` — as an **opt-in, near-zero-cost**
feature. Dropping Speedy into an app costs nothing extra unless an entity explicitly asks for it.

### Opt-in via `@SpeedyETag`

Annotate one field with `@SpeedyETag` to designate it as the entity's conditional-request token.
Speedy **owns** the field from then on: it stamps a fresh value into it on every create/update/
replace, then reads that value back to build the `ETag`. There is no content hashing anywhere in
this feature — an entity that doesn't opt in costs a single cached lookup (empty) on every request.

```java

@Entity
public class Supplier {

    @Id
    private String id;

    @SpeedyETag
    @Column(name = "row_version", length = 64)
    private String rowVersion;

    // ...
}
```

The annotated field must **not** also be set by the client — mark it non-deserializable (Speedy's
JPA processor does this automatically) so a client-supplied value is always ignored and overwritten.

### Strategies

| Strategy           | Column type                               | Behavior                                                   |
|--------------------|-------------------------------------------|------------------------------------------------------------|
| `RANDOM` (default) | text                                      | A fresh opaque token (random UUID) on every create/update. |
| `TIMESTAMP`        | date / time / date-time / zoned-date-time | Set to `now()` on every create/update.                     |

```java

@SpeedyETag(strategy = EtagStrategy.TIMESTAMP)
@Column(name = "updated_at")
private LocalDateTime updatedAt;
```

Speedy validates the strategy against the field's column type at startup and fails fast on a
mismatch (e.g. `RANDOM` on a numeric column).

A JPA `@Version` field is auto-honored as an ETag field **when its column is temporal** (mapped to
`TIMESTAMP`) — Speedy persists via its own query layer, bypassing Hibernate's version-increment, so a
numeric `@Version` is left unmanaged unless you annotate it with `@SpeedyETag` directly.

### GET — `ETag` and `If-None-Match`

A GET that resolves to exactly one row (not a paginated slice of a larger match) on an ETag-enabled
entity carries a weak `ETag` header:

```http
[GET] /speedy/v1/Supplier?id=1a2b3c4d-5678-90ab-cdef-1234567890ab
```

```http
200 OK
ETag: W/"3f29c1a0-...-9e2b"
```

Send that value back as `If-None-Match` to revalidate a cached copy:

```http
[GET] /speedy/v1/Supplier?id=1a2b3c4d-5678-90ab-cdef-1234567890ab
If-None-Match: W/"3f29c1a0-...-9e2b"
```

```http
304 Not Modified
ETag: W/"3f29c1a0-...-9e2b"
```

A stale or absent match returns the normal `200` with the current body and `ETag`. Entities with no
`@SpeedyETag` field never emit an `ETag`, and any `If-None-Match` sent to them is simply ignored.
A multi-row list response never carries an `ETag` — there is no single resource to identify.

### Writes — `If-Match`

`PATCH`, `PUT`, and `DELETE` honor `If-Match` for **single-item requests only** (one entity /
one key), since a single header value can't unambiguously address several resources:

```http
[PATCH] /speedy/v1/Supplier/$update
If-Match: W/"3f29c1a0-...-9e2b"
```

```json
{
    "id": "1a2b3c4d-5678-90ab-cdef-1234567890ab",
    "name": "New Name"
}
```

- **Matching tag** — the write proceeds, and the response carries the **new** `ETag`.
- **Stale tag, or the row doesn't exist** — `412 Precondition Failed`.
- **`If-Match: *`** — succeeds if the row exists, `412` if it doesn't.
- **Multi-item batch with `If-Match`** — `400 Bad Request` (ambiguous).
- **`If-Match` against an entity with no `@SpeedyETag` field** — `400 Bad Request`, so a client is
  never falsely assured of concurrency protection.

```json
{
    "status": 412,
    "message": "If-Match precondition failed for Supplier",
    "timestamp": "2026-05-28T13:45:00"
}
```

See [Exception Handling](exception-handling.md) for how `412`/`400` fit the general error-mapping
pipeline.

<hr>
