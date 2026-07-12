# Request-Scoped Authorization Policies

Speedy-API does not authenticate callers or decode tokens. Your application remains responsible for authentication
(for example, with Spring Security), then supplies the authenticated caller and their authorization policy through
`ISpeedyConfiguration.policyPerReq()`.

When that method returns `Optional.empty()`—the default—policy enforcement is disabled and existing applications retain
their current behavior.

## Configure a Policy Per Request

Resolve the authenticated caller from your application's security context, session, API key, or tenant resolver. Return
a `SpeedyPolicy` containing the caller (`Principal`) and their policy document.

```java
@Override
public Optional<SpeedyPolicy> policyPerReq() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
        return Optional.empty();
    }

    Principal principal = new MapPrincipal(authentication.getName(), Map.of(
            "department", "HR"
    ));
    return Optional.of(new SpeedyPolicy(policyDocument, principal));
}
```

Construct `policyDocument` directly with `PolicyDocument` and `PolicyRule`, or parse a JSON policy with
`PolicyDocumentParser` and `PolicyConditionRegistry`. The policy document is immutable, so a shared document can be
combined safely with a different request principal each time.

## Policy Format

```json
{
  "defaultEffect": "Deny",
  "policies": [
    {
      "id": "hr-can-read-salaries",
      "role": "b6cf4ec8-7f67-4f5a-b8cf-b930f30356ec",
      "effect": "Allow",
      "action": "read",
      "subject": "Employee.salary"
    },
    {
      "id": "user-owns-record",
      "role": "c055fbe4-fb86-408e-8a24-318ae0224b0f",
      "effect": "Allow",
      "action": ["read", "update"],
      "subject": "Employee",
      "conditions": { "type": "FieldEquals", "field": "ownerId", "value": "${principal.id}" }
    }
  ]
}
```

Each policy uses `id`, `role`, `effect`, `action`, `subject`, and `conditions`. `action` accepts a string or an array
of strings; `manage` expands to all CRUD actions. `subject` accepts a string or an array of strings; `Entity` and
`Entity.*` both apply to every field, while `Entity.field` targets one field. `policies` and `conditions` each accept
either one object or an array of objects; conditions are AND-combined. An explicit `DENY` overrides matching `ALLOW`
rules. The default effect is `DENY` when omitted from JSON or supplied as `null` when constructing a
`PolicyDocument` directly.

`role` is policy-store metadata. Resolve the policies for the authenticated caller's roles in
`ISpeedyConfiguration.policyPerReq()` before creating the request-scoped `SpeedyPolicy`.

The built-in `FieldEquals` condition compares an entity field to either a literal or `${principal.id}` / a
`${principal.attribute}` reference. Additional condition types can be parsed by registering a
`PolicyConditionFactory`.

## Enforcement Behavior

| Operation | Policy behavior |
| --- | --- |
| Read | Fields denied for a row are omitted from its response. Key fields remain available so returned rows retain an identity. |
| Read with row conditions | Translatable `ALLOW` conditions are added to the query `WHERE` clause, so paging and `totalCount` operate on visible rows. An unconditional or untranslatable grant leaves SQL unrestricted; per-row field omission remains enforced. |
| Query filters and ordering | A field must be readable unconditionally. A conditionally readable field cannot be used to filter or sort, preventing information leakage. |
| Create and update | Every supplied field must be permitted. A prohibited field returns `403 Forbidden`. Update conditions evaluate the current database row. |
| Delete | The target row is loaded and evaluated before deletion. A denied row returns `403 Forbidden`. |

`@SpeedyAction` remains available for static, application-wide CRUD gates. It composes with policies: a request must
pass both the static annotation gate and the request-scoped policy when a policy is configured.

## Security Boundary

Policies are authorization, not authentication. Keep endpoint authentication, token validation, transport security,
and principal construction in your application. A policy must be resolved from trusted server-side identity data—not
from an unverified request header or request body.
