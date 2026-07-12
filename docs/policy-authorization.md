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

## Policy Format

Create a document with its default effect and ordered rules:

```java
SpeedyPolicy hrCanReadSalaries = new SpeedyPolicy(
        "hr-can-read-salaries",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.READ),
        List.of("Employee.salary"),
        List.of());

SpeedyPolicy userOwnsRecord = new SpeedyPolicy(
        "user-owns-record",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.READ, PermissionType.UPDATE),
        List.of("Employee.*"),
        List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));

PolicyDocument policyDocument = new PolicyDocument(
        PolicyEffect.DENY,
        List.of(hrCanReadSalaries, userOwnsRecord));
```

The five-argument `SpeedyPolicy` constructor takes `id`, `effect`, `action`, `subject`, and `conditions`. The
six-argument form adds an optional `role` string for policy-store metadata; resolve the rules for the caller's roles
before building the document.

`action` is a `Set<PermissionType>` containing any of `CREATE`, `READ`, `UPDATE`, and `DELETE`. `subject` is a list
of selectors:

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
        List.of("Invoice.*"),
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
        List.of("Invoice.status"),
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
        List.of("Product.name"),
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
        List.of("Product.*"),
        List.of(new QueryCondition(Map.of("description", "${principal.id}"))));
```

For `principal.id = "Description 1"`, `GET /speedy/v1/Product` returns only the product whose description is
`Description 1`. The condition is injected before paging and counting, so `totalCount`, `totalPages`, and the payload
all describe the visible set. The same rule does **not** allow `GET /speedy/v1/Product?name='Product 1'`: `name` is
only conditionally readable, so filtering or sorting by it is rejected with `400`.

### A public field plus a conditional field

```java
SpeedyPolicy descriptions = new SpeedyPolicy(
        "read-descriptions",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.READ),
        List.of("Product.description"),
        List.of());
SpeedyPolicy ownNames = new SpeedyPolicy(
        "read-own-names",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.READ),
        List.of("Product.name"),
        List.of(new QueryCondition(Map.of("description", "${principal.id}"))));
```

All products are returned with `description`. Only the product whose description matches `principal.id` also includes
`name`. This is useful when a response may contain public and tenant- or owner-scoped fields together.

### Explicit deny wins

```java
SpeedyPolicy allowAllProductFields = new SpeedyPolicy(
        "allow-product-fields", PolicyEffect.ALLOW, Set.of(PermissionType.READ),
        List.of("Product.*"), List.of());
SpeedyPolicy denyDescription = new SpeedyPolicy(
        "deny-description", PolicyEffect.DENY, Set.of(PermissionType.READ),
        List.of("Product.description"), List.of());
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
        List.of("Category.name"),
        List.of(new QueryCondition(Map.of("name", "${principal.id}"))));
```

`PATCH /speedy/v1/Category/$update` may include the primary key without granting permission to `Category.id`; the key
only identifies the row. If that row's current `name` matches `principal.id`, changing `name` is allowed and the
response is serialized through the normal policy-aware response filter. In `per-entity` bulk mode, a request
containing one matching row and one non-matching row returns `207 Multi-Status`: the matching update is in `succeeded`
and the other item is in `failed` with status `403`.

### Create only selected values

```java
SpeedyPolicy importNames = new SpeedyPolicy(
        "import-approved-names",
        PolicyEffect.ALLOW,
        Set.of(PermissionType.CREATE),
        List.of("Category.name"),
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
| Read / query | An action with no possible `ALLOW` is rejected with `403`. An unconditional `DENY` for `Entity` or `Entity.*` also rejects the request before returning rows or counts. Fields denied for a particular row are omitted; primary-key fields are always emitted so a returned row keeps its identity. |
| Read row conditions | Conditional `READ` allows are translated into the query `WHERE` clause before paging and `totalCount`; conditions from different allow rules are OR-combined. An unconditional read allow, or a condition that cannot be translated, leaves SQL unrestricted, but per-row field filtering remains in force. |
| Filters and ordering | A referenced field must be readable **unconditionally**. A conditional allow cannot make a field filterable or sortable, because that could disclose row data. This check also applies to fields reached through an association. |
| Create | The entity must permit `CREATE`, and every supplied writable field must be allowed for the submitted entity. A denied field returns `403 Forbidden`. |
| Update | The entity must permit `UPDATE`, and every supplied writable (non-key) field is evaluated against the target row's current persisted state. The primary key only identifies the row and does not need `UPDATE` permission. This supports rules such as “update only records I own.” A denied field returns `403 Forbidden`. |
| Delete | The entity must permit `DELETE`; Speedy loads the target row and evaluates its conditions before deleting it. A denied row returns `403 Forbidden`. |

Policy failures follow the normal bulk transaction mode. A denied create field is checked before creation, so the whole
create request fails. In `batch` mode, a denied update or delete rolls back the whole request. In `per-entity` mode,
successful items commit independently; a mix of successes and policy failures returns `207 Multi-Status`, with the
failed item reporting status `403`. See [PUT Operations](put-operation.md#transaction-mode-transaction) and
[DELETE Operations](delete-operation.md) for bulk transaction-mode details.

## Security Boundary

Policies are authorization, not authentication. Keep endpoint authentication, token validation, transport security,
and principal construction in your application. Never derive `SpeedyAuthContext` variables or policy rules from an
unverified request header or request body.
