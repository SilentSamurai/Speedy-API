# Speedy API Feature Test Gaps

Features that are missing integration tests in `speedy-test-app`. Entities are just test vehicles — gaps below are organized by framework feature.

---

## 1. `@SpeedyAction` Permission Enforcement

| Feature | Gap |
|---|---|
| **Class-level `READ`** blocks create/update/delete | VirtualEntity has `@SpeedyAction(READ)` on the class. No test verifies POST/PUT/DELETE are rejected. |
| **Field-level `READ`** hides from deserialization | `Customer.createdBy` has `@SpeedyAction(READ)`. No test verifies the field is not writable on create/update. |
| **Serialization of READ-only fields** | Verify `User.createdAt`, `updatedAt`, `deletedAt` appear in GET responses but are ignored in create payloads. |

---

## 2. Query DSL (`POST /$query`)

### 2.1 `$select` — Untested
```json
{ "$from": "Product", "$select": ["name", "description"] }
```
- Verify response **only** contains `name` and `description` (no `id`, no `category` nesting)
- Verify `$select` + `$expand` together: selected field from root + expanded association

### 2.2 `$where` Operators
| Operator | Tested? | Gap |
|---|---|---|
| `$eq` | Yes | — |
| `$ne` | Yes | — |
| `$gt` / `$gte` | Yes (numeric) | Not tested on date fields |
| `$lt` / `$lte` | Yes (numeric) | Not tested on date fields |
| `$in` / `$nin` | Yes | — |
| `$matches` | Yes (String) | Not tested on numeric/date fields |
| `$and` / `$or` | Yes (basic) | Not tested nested across FK fields |
| `$neq null` / `null` | Yes | — |

### 2.3 Field-Reference Comparisons (`$fieldName`)
Only tested on `Invoice` (`discount < $dueAmount`, `invoiceDate = $createdAt`). Could also test on:
- `Inventory`: `cost < $soldPrice`, `cost > $discount`

### 2.4 Missing/Invalid `$from`
```json
{ "$where": { "name": "test" } }
```
Should return 400.

---

## 3. GET URL Query Params

Only basic `?id='1'` and `?name='cat-1-1'` are tested. Framework-level params are completely untested:

| Param | Example | Tested? |
|---|---|---|
| `$expand` | `?$expand=Category` | No |
| `$orderBy` | `?$orderBy=name ASC` | No |
| `$page` + `$pageSize` | `?$page=0&$pageSize=5` | No |
| `$select` | `?$select=name,description` | No |
| Multi-condition (AND) | `?name='X'&category.id='1'` | No |

---

## 4. Response Serialization

### 4.1 Pagination Metadata
No test verifies paginated responses include `pageIndex`, `pageSize`, `totalElements`.

### 4.2 Count Response Format
Partially tested (`SpeedyV2SelectCountTest`). No test for count with `$where` + `$expand` combination.

### 4.3 Entity Metadata Endpoint Per-Entity
Only `GET /$metadata` (all entities) is tested. `GET /Category/$metadata` is not.

### 4.4 Error Response Format
No centralized test verifying all errors follow `{"status":..., "message":..., "timestamp":...}`.

---

## 5. Event System (`@SpeedyEvent`)

### 5.1 Untested Event Types
| Event | Status |
|---|---|
| `POST_INSERT` | Zero tests. The `categoryPostInsertEvent` registers it but no test verifies it fires. |
| `POST_UPDATE` | Zero tests across all entities. |
| `POST_DELETE` | Zero tests across all entities. |

### 5.2 `PRE_DELETE` for User
`@Disabled` at `UserEventTest.java:143` — tagged "TODO: support soft delete". Either enable or remove the handler.

### 5.3 `SpeedyEntity` Parameter Type
`categoryPostInsertEvent(SpeedyEntity category)` takes the generic `SpeedyEntity` type. No test verifies event handlers work with the generic type parameter vs domain POJOs.

---

## 6. Validation System

### 6.1 Custom Validators (`@SpeedyValidator`) — Untested
| Validator | Entity | Operation | Status |
|---|---|---|---|
| `validateSupplier` | Supplier | CREATE | **No test** — requires name + phoneNo |
| `preventInvalidProductName` | Product | CREATE | **No test** — blocks name "invalid-trigger" |

### 6.2 Annotation-Based Validators — Untested
| Annotation | Tested? |
|---|---|
| `@SpeedyPositiveOrZero` | No |
| `@SpeedyNegativeOrZero` | No |
| `@SpeedyDecimalMax` | No |
| `@SpeedyDateRange` boundary values | No (only out-of-range tested) |

---

## 7. Bulk Operations

| Feature | Gap |
|---|---|
| **Bulk DELETE** | Only single-ID delete tested. No test deletes multiple entities at once. |
| **Bulk CREATE via client SDK** | All client tests create a single entity. No test creates multiple entities in one call. |
| **Empty `$create` body** | `POST /$create` with `[]` — not tested (should return 400). |

---

## 8. CRUD Lifecycle Coverage

| Entity | Create | GET | Query | Update | Delete | Notes |
|---|---|---|---|---|---|---|
| Category | Yes | Yes | Yes | Yes | Yes | — |
| Product | Yes | Yes | Yes | Yes | Yes | — |
| Company | Yes | Yes | Yes | Yes | Yes | Enum + events tested |
| Supplier | Yes | Yes | — | — | — | Missing update/delete |
| Procurement | Yes | Yes | Yes | — | — | Missing update/delete |
| Customer | — | — | — | — | — | Only bad-request create |
| Invoice | — | — | Partial | — | — | Only field-ref queries |
| Inventory | Yes | Yes | Yes | — | — | Missing update/delete |
| Currency | Yes | Yes | Yes | Yes | Yes | — |
| ExchangeRate | Yes | Yes | Partial | — | — | Missing update/delete |
| User | Yes | — | — | Partial | `@Disabled` | PRE_INSERT/UPDATE tested |
| Order | Yes | Yes | — | Yes | Yes | Composite key |
| Task | Yes | Yes | Yes | Yes | — | Enum types tested |
| FkNullEntity | Yes | Yes | — | — | — | Null FK only |
| PkUuidTest | Yes | — | Yes | — | — | UUID PK only |
| ValueTestEntity | Yes | Yes | Yes | — | — | Date/time types only |
| AnnotatedPerson | Yes | — | — | Partial | Partial | Missing update/delete validation |
| VirtualEntity | — | — | — | — | — | READ-only; no tests |

---

## 9. Edge Cases & Error Handling

| Gap | Notes |
|---|---|
| Malformed JSON body for `$create` | Send `{ notAnArray: true }` instead of `[...]` |
| Missing `$from` in query body | Should return 400 |
| `$create` with null FK values | Partially tested via `NullAssociationTest` |
| Concurrent requests | No race condition tests |
| XSS injection | No `<script>` payload tests in create |
| `HttpMethod` mismatch | e.g., POST to `/$metadata`, GET to `/$create` |
| `$query` with `$select` containing non-existent field | Should return 400 or gracefully handle |

---

## 10. Priority Ranking (by Feature Impact)

| # | Feature | Effort |
|---|---|---|
| 1 | `$select` in POST `/$query` | Low |
| 2 | GET URL params (`$expand`, `$orderBy`, `$page`, `$select`) | Low |
| 3 | Custom validators (`validateSupplier`, `preventInvalidProductName`) | Low |
| 4 | `@SpeedyAction` enforcement (READ-only entity, field-level READ) | Medium |
| 5 | Missing annotation validators (PositiveOrZero, NegativeOrZero, DecimalMax) | Low |
| 6 | POST_INSERT / POST_UPDATE / POST_DELETE event verification | Medium |
| 7 | Bulk DELETE | Low |
| 8 | Pagination metadata in response | Low |
| 9 | Nested AND/OR with FK conditions in `$where` | Medium |
| 10 | Field-ref queries on Inventory (`cost < $soldPrice`) | Low |
| 11 | `$matches` on non-string fields | Low |
| 12 | GET multi-condition AND (URL params) | Low |
| 13 | `$select` + `$expand` combined | Low |
| 14 | Missing `$from` / malformed query body | Low |
| 15 | Error response format consistency | Low |
| 16 | XSS / special character injection | Low |
| 17 | Concurrent request tests | High |
