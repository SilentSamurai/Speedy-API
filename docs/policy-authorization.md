# Request-Scoped Authorization Policies

Speedy-API does not authenticate callers or decode tokens. Your application authenticates the request (for example,
with Spring Security), resolves its trusted identity, and returns the caller's policy from
`ISpeedyConfiguration.authContextPerReq()`.

Each policy context contains a `PolicyDocument` and the pre-resolved variables that its conditions may use. Speedy
does not inspect the authentication mechanism or construct these values for you.

## Configure a Policy Per Request

Resolve the caller from your application's security context, session, API key, or tenant resolver. Return a
`SpeedyAuthContext` with the caller's policy document and variables. Variable values use `SpeedyValue` types, such as
`SpeedyText`.

```java
@Override
public Optional<SpeedyAuthContext> authContextPerReq() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
        return Optional.of(new SpeedyAuthContext(new PolicyDocument(PolicyEffect.DENY, List.of())));
    }

    return Optional.of(new SpeedyAuthContext(
            policyDocument,
            Map.of(
                    "principal.id", new SpeedyText(authentication.getName()),
                    "principal.department", new SpeedyText("HR")
            )
    ));
}
```

`authContextPerReq()` is invoked for every request. A shared, immutable `PolicyDocument` can safely be combined with
different caller variables each time.

> `Optional.empty()` is fail-closed: `DefaultSpeedyEngine` substitutes an empty, `DENY`-by-default policy document.
> It does not disable policy enforcement. Applications upgrading to policies must return an explicit
> `new SpeedyAuthContext(new PolicyDocument(PolicyEffect.ALLOW, List.of()))` for requests that should retain
> unrestricted access.

## Fluent Builder

`PolicyBuilder` provides a concise, fail-closed way to construct a request context. `PolicyConditions` supplies
common row-condition expressions; use `variable("principal.id")` to reference a trusted context variable.

```java
import static com.github.silent.samurai.speedy.policy.PolicyConditions.fieldEquals;
import static com.github.silent.samurai.speedy.policy.PolicyConditions.variable;

SpeedyAuthContext context = PolicyBuilder.denyByDefault()
        .principalId(authentication.getName())
        .allow("read-own-invoices", PermissionType.READ, "Invoice.*",
                fieldEquals("ownerId", variable("principal.id")))
        // A write may only gate on a field the caller can read, so grant read of ownerId too.
        .allow("read-invoice-owner", PermissionType.READ, "Invoice.ownerId")
        .allow("update-own-status", PermissionType.UPDATE, "Invoice.status",
                fieldEquals("ownerId", variable("principal.id")))
        .build();
```

For anything beyond a single field equality, pass the condition map to `QueryCondition` directly — it accepts the
full `$or`/`$and`/`$in`/operator/`${variable}` grammar, e.g.
`new QueryCondition(Map.of("status", Map.of("$in", List.of("DRAFT", "REVIEW"))))`. The builder also supports
`variable(name, speedyValue)`, explicit `deny(...)` rules, and `allowByDefault()` when an application intentionally
needs an allow-by-default document.

**Write-condition guard.** A field an `UPDATE`/`REPLACE`/`DELETE` rule gates on must itself be readable by the caller
(as `read-invoice-owner` grants above). Otherwise the write's success/failure would leak that field's values for
rows the caller cannot read. A caller lacking that read grant is refused before any row is touched. `CREATE`
conditions are exempt: they test only the caller's own submitted values, which reveal nothing new.

## Policy Format

Create a document with its default effect and ordered rules:

```java
SpeedyPolicy hrCanReadSalaries = new SpeedyPolicy(
        "hr-can-read-salaries",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.READ),
        "Employee.salary",
        List.of());

SpeedyPolicy userOwnsRecord = new SpeedyPolicy(
        "user-owns-record",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.READ, PermissionType.UPDATE),
        "Employee.*",
        List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));

PolicyDocument policyDocument = new PolicyDocument(
        PolicyEffect.DENY,
        List.of(hrCanReadSalaries, userOwnsRecord));
```

The five-argument `SpeedyPolicy` constructor takes `id`, `effect`, `action`, `subject`, and `conditions`. The
six-argument form adds an optional `role` string for policy-store metadata; resolve the rules for the caller's roles
before building the document.

`action` is a `Set<PermissionType>` containing any of `CREATE`, `READ`, `UPDATE`, `REPLACE`, and `DELETE`. Each policy has one
`subject` selector:

| Selector | Matches |
| --- | --- |
| `Entity` | Every field on the entity |
| `Entity.*` | Every field on the entity |
| `Entity.field` | That field only |

When no rule matches, `PolicyDocument.defaultEffect()` applies (`null` is treated as `DENY`). A matching `DENY`
overrides every matching `ALLOW`, regardless of their order. Conditions on one rule are AND-combined; separate
conditional `ALLOW` rules grant access independently.

## Row Conditions and Variables

`QueryCondition` accepts the same condition structure as request `$where` clauses. It supports field comparisons,
boolean `$and` / `$or` groups, and operators such as `$in`. Literal values and `${variable}` references may be mixed:

```java
SpeedyPolicy visibleInvoices = new SpeedyPolicy(
        "visible-invoices",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.READ),
        "Invoice.*",
        List.of(new QueryCondition(Map.of(
                "$or", List.of(
                        Map.of("ownerId", "${principal.id}"),
                        Map.of("department", "${principal.department}")
                )
        ))));

SpeedyPolicy editableStates = new SpeedyPolicy(
        "editable-states",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.UPDATE),
        "Invoice.status",
        List.of(new QueryCondition(Map.of(
                "status", Map.of("$in", List.of("DRAFT", "REVIEW"))
        ))));
```

The variable names in the map supplied to `SpeedyAuthContext` must exactly match the references in a condition—for
example, `"principal.id"` for `${principal.id}`. Resolve them only from trusted server-side identity data.

## Policy Examples and Results

The examples below assume `new PolicyDocument(PolicyEffect.DENY, rules)`: anything not granted is denied. They also
assume `principal.id` is present in the `SpeedyAuthContext` variables.

### Field-only read access

```java
SpeedyPolicy namesOnly = new SpeedyPolicy(
        "product-names-only",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.READ),
        "Product.name",
        List.of());
```

`GET /speedy/v1/Product` succeeds because the rule grants some `READ` access. Every returned product includes its
primary key and `name`; fields such as `description` are omitted. An unconditional field-level `DENY` affects only that
field, so it does not block the whole entity when another field remains allowed.

### Rows and fields visible only to their owner

```java
SpeedyPolicy ownProducts = new SpeedyPolicy(
        "own-products",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.READ),
        "Product.*",
        List.of(new QueryCondition(Map.of("description", "${principal.id}"))));
```

For `principal.id = "Description 1"`, `GET /speedy/v1/Product` returns only the product whose description is
`Description 1`. The condition is injected before paging and counting, so `totalCount`, `totalPages`, and the payload
all describe the visible set. The same rule does **not** allow `GET /speedy/v1/Product?name='Product 1'`: `name` is
only conditionally readable, so filtering or sorting by it is rejected with `400`.

Field-level READ visibility is always unconditional — it never depends on a row's own data. A condition only ever
selects which rows are visible at all, and only takes effect on a whole-entity selector (`Entity` / `Entity.*`); a
condition attached to a field-specific selector (`Entity.field`) never grants or denies READ. To hide one field from
an otherwise-visible row, pair the row condition above with an unconditional field `DENY`:

```java
SpeedyPolicy hideDescription = new SpeedyPolicy(
        "hide-description",
        PolicyEffect.DENY,
        Set.of(PermissionType.READ),
        "Product.description",
        List.of());
```

The caller still only sees their own product (row visibility stays conditional), but `description` is stripped from
it regardless of the row's data (field visibility is unconditional) — row selection can filter on a field's raw
column value even while that same field is denied from the response.

This restriction is READ-only. `CREATE`, `UPDATE`, and `REPLACE` field checks validate a write rather than gate read visibility,
so field-specific selectors with conditions keep working there exactly as shown in
[Owner-scoped update and partial bulk results](#owner-scoped-update-and-partial-bulk-results) and
[Create only selected values](#create-only-selected-values) below.

### Explicit deny wins

```java
SpeedyPolicy allowAllProductFields = new SpeedyPolicy(
        "allow-product-fields", PolicyEffect.ALLOW, Set.of(PermissionType.READ),
        "Product.*", List.of());
SpeedyPolicy denyDescription = new SpeedyPolicy(
        "deny-description", PolicyEffect.DENY, Set.of(PermissionType.READ),
        "Product.description", List.of());
```

`GET /speedy/v1/Product` succeeds and returns `name`, but never `description`. The result is the same whichever order
the two rules appear in the document. By contrast, an unconditional deny for `Product` or `Product.*` makes the whole
read request fail with `403`.

### Owner-scoped update and partial bulk results

```java
SpeedyPolicy updateOwnCategoryNames = new SpeedyPolicy(
        "update-own-category-names",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.UPDATE),
        "Category.name",
        List.of(new QueryCondition(Map.of("name", "${principal.id}"))));
```

`PATCH /speedy/v1/Category/$update` may include the primary key without granting permission to `Category.id`; the key
only identifies the row. If that row's current `name` matches `principal.id`, changing `name` is allowed and the
response is serialized through the normal policy-aware response filter. For a bulk request, every target must pass
this check before the request enters update execution. One non-matching row rejects the entire request with `403`.

### Create only selected values

```java
SpeedyPolicy importNames = new SpeedyPolicy(
        "import-approved-names",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.CREATE),
        "Category.name",
        List.of(new QueryCondition(Map.of(
                "name", Map.of("$in", List.of("approved-a", "approved-b"))
        ))));
```

Creating either approved name succeeds. A create request containing any other name fails with `403`; no item from that
create request is written.

## Enforcement Behavior

Every operation must pass both static `@SpeedyAction` checks and the request policy. A policy has an entity-level
gate as well as field- and row-level checks:

| Operation | Policy behavior |
| --- | --- |
| Read / query | An action with no possible `ALLOW` is rejected with `403`. An unconditional `DENY` for `Entity` or `Entity.*` also rejects the request before returning rows or counts. Field-level READ grants/denies are always unconditional — they never depend on row data — so a field is either included on every row it's checked against or omitted from all of them; primary-key fields are always emitted so a returned row keeps its identity. |
| Read row conditions | Only whole-entity (`Entity` / `Entity.*`) conditional `READ` allows are translated into the query `WHERE` clause before paging and `totalCount`; conditions from different allow rules are OR-combined. A condition on a field-specific selector never affects row visibility. An unconditional read allow, or a condition that cannot be translated, leaves SQL unrestricted, but per-row field filtering remains in force. |
| Filters and ordering | A referenced field must be readable **unconditionally**. A conditional allow cannot make a field filterable or sortable, because that could disclose row data. This check also applies to fields reached through an association. |
| Create | The entity must permit `CREATE`, and every supplied writable field must be allowed for the submitted entity. A denied field returns `403 Forbidden`. |
| Update | The entity must permit `UPDATE`, and every supplied writable (non-key) field is evaluated against the target row's current persisted state. The primary key only identifies the row and does not need `UPDATE` permission. This supports rules such as “update only records I own.” A denied field returns `403 Forbidden`. |
| Replace | `PUT` must be permitted by both `@SpeedyAction(REPLACE)` and `PermissionType.REPLACE`. Its writable fields are evaluated against the target row's current persisted state, just as for update. `UPDATE`/PATCH permission does not grant replace access. |
| Delete | The entity must permit `DELETE`; Speedy loads the target row and evaluates its conditions before deleting it. A denied row returns `403 Forbidden`. |

Policy failures for update and replace are request-wide preflight failures. Every target row is authorized before any
update event or SQL write begins; one denied item ends the whole request with `403`. A denied create field is likewise
checked before creation, so the whole create request fails. Writes are all-or-nothing: any validation, authorization, or
persistence failure rejects the entire request and commits nothing. PATCH and PUT also verify that every target key
exists before downstream authorization or write handlers run; a missing target ends the request with `404` before any
update event or SQL write.
See [PUT Operations](put-operation.md#atomicity) and [DELETE Operations](delete-operation.md) for bulk write details.

## Security Boundary

Policies are authorization, not authentication. Keep endpoint authentication, token validation, transport security,
and principal construction in your application. Never derive `SpeedyAuthContext` variables or policy rules from an
unverified request header or request body.
