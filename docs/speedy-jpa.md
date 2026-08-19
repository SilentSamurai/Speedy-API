# Speedy Jpa

### Overview

Configure Jpa Entity so that speedy can retrieve the resource details

### Speedy Action Type

specify different operation on fields & entity

**ReadOnly Entity**

```java

@SpeedyAction(ActionType.READ)
@Table(name = "readonly")
@Entity
public class Readonly {

}
```

**ReadOnly Field**

speedy is not responsible to update this field, either it is written by default value at database level or by speedy
events

```java

@Table(name = "entity")
@Entity
public class Entity {

    @SpeedyAction(ActionType.READ)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
```

**Exact Action Field**

speedy is not responsible to update this field, either it is written by default value at database level or by speedy
events

```java

@Table(name = "entity")
@Entity
public class Entity {

    // id cannot be updated or removed
    @SpeedyAction({ActionType.READ, ActionType.CREATE})
    @Column(name = "id")
    private String id;

}
```

### Speedy Ignore

Exclude entities or individual fields from the Speedy metamodel. Ignored entities and fields
are completely invisible to all Speedy operations — they do not appear in `$metadata`, cannot
be queried, filtered, selected, created, updated, or deleted.

**Entity-Level Ignore**

Annotate an entity class with `@SpeedyIgnore` to exclude the entire entity from the metamodel.
The entity will be absent from `$metadata` and all CRUD requests targeting it will return
**400 Bad Request**.

```java
@SpeedyIgnore
@Table(name = "internal_entity")
@Entity
public class InternalEntity {
    
}
```

**Field-Level Ignore**

Annotate individual fields with `@SpeedyIgnore` to exclude them from the entity's metadata.
Ignored fields do not appear in the `fields` array of `$metadata`, cannot be used as query
filters (URL params or `$where`), and cannot be referenced in `$select` or `$expand`.

```java
import jakarta.persistence.Column;

@Table(name = "entity")
@Entity
public class Entity {

    @SpeedyIgnore
    @Column(name = "internal")
    private String internal;

    @SpeedyIgnore
    @Column(name = "secret_code")
    private Integer secretCode;

    // Non-ignored fields remain fully visible
    @Column(name = "public_name")
    private String publicName;
}
```

**Association Propagation**

When an entity is ignored at the class level, any `@ManyToOne` or `@OneToOne` association
in other entities that references it is also automatically excluded from the metamodel.

```java
@SpeedyIgnore
@Entity
public class InternalEntity { /* ... */ }

@Entity
public class PublicEntity {

    // This association will be excluded because InternalEntity has @SpeedyIgnore
    @ManyToOne
    @JoinColumn(name = "internal_id")
    private InternalEntity internalRef;
}
```

### Composite-Key Associations

A to-one association (`@ManyToOne`/`@OneToOne`) whose target entity has a composite primary key
is mapped through JPA's plural `@JoinColumns` — one `@JoinColumn` per key column, each naming the
key column it references via `referencedColumnName`:

```java
@Table(name = "orders")
@Entity
@IdClass(OrderId.class) // product_id + supplier_id
public class Order { /* ... */ }

@Table(name = "order_shipments")
@Entity
public class OrderShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @ManyToOne(optional = false)
    @JoinColumns({
            @JoinColumn(name = "order_product_id", referencedColumnName = "product_id"),
            @JoinColumn(name = "order_supplier_id", referencedColumnName = "supplier_id")
    })
    private Order order;
}
```

Every `@JoinColumn` must set `referencedColumnName` — it is the only thing a local column can be
matched against, so a missing or unmatched one fails fast at metamodel build time, as does
declaring fewer join columns than the target's key has.

A composite-key association behaves like a single-column one everywhere:

- **Reads / writes** use the keys-only reference object, which carries *every* key field:
  `{"order": {"productId": "...", "supplierId": "..."}}`. A write payload missing a key field is
  rejected with `400 Bad Request` (`order.supplierId is required`).
- **Query filters** navigate the association (`order.price = 44`, `order.supplierId = "..."`), which
  joins on all key columns at once. Comparing the association field itself to a single value is
  rejected — reference one of the target's key fields instead. `$isnull` is true only when every
  foreign-key column is null.
- **`$expand`** matches parent rows to targets on the full key, so two targets that share one key
  column still resolve to their own rows.

### Speedy Association

By default Speedy only discovers associations from real JPA relationships (`@ManyToOne`/`@OneToOne`).
If a foreign key has to stay a plain scalar column — for example a `UUID` field on a shared or
legacy entity that can't be remapped to an object reference — annotate it with `@SpeedyAssociation`
to wire it into the metamodel as an association anyway. It always binds to the target entity's
primary key, exactly like `@ManyToOne` does.

```java
@Table(name = "entity")
@Entity
public class Entity {

    @Column(name = "target_id")
    @SpeedyAssociation(Target.class)
    private UUID target;
}
```

The target entity is given as exactly one of `value()` (its JPA entity class) or `entity()` (its
Speedy entity name as a `String`) — use the latter when the target class isn't available to
reference directly, e.g. across module boundaries:

```java
    @Column(name = "target_id")
    @SpeedyAssociation(entity = "Target")
    private UUID target;
```

Specifying both, or neither, fails fast at metamodel build time.

The scalar field's own declared Java type doesn't need to match the target's primary key type —
the actual read/write conversion is driven entirely by the target key field's type, so this works
identically against `UUID`, `String`, or numeric (e.g. `Long` `IDENTITY`) primary keys. A scalar
column *is* a single foreign-key column by construction, so the target must have a single-column
primary key — pointing `@SpeedyAssociation` at a composite-key entity fails fast at metamodel
build time. For composite-key targets use a real `@ManyToOne` with `@JoinColumns` instead (see
[Composite-Key Associations](#composite-key-associations)).

Once annotated, the field behaves exactly like a `@ManyToOne` association: it gains `$expand`
support, `$filter` navigation through the association (e.g. `target.name`), and is serialized as
a keys-only reference object (or fully expanded object with `$expand`) rather than a bare scalar.
This also means write payloads must use the nested-object shape (`{"target": {"id": "<uuid>"}}`)
instead of a bare scalar value — a plain `UUID` value is rejected, same as for any other
association field.

**Renaming the exposed property.** A scalar FK is conventionally named after the column it maps
(`someFieldId`), but the annotation turns it into an object reference — so left alone it is
exposed and navigated as `someFieldId.id`, with a `{"someFieldId": {"id": "<uuid>"}}` write
payload. The `Id` suffix now describes a property that isn't an id. Use `name()` to expose it
under the name it would have had as a `@ManyToOne`:

```java
    @Column(name = "some_field_id")
    @SpeedyAssociation(value = SomeEntity.class, name = "someField")
    private UUID someFieldId;
```

The rename applies everywhere the property name surfaces — request/response JSON, the generated
OpenAPI schema properties, and `$filter` navigation paths (`someField.id`) — while `@Column`
still drives the DB column name. It is equivalent to putting `@JsonProperty("someField")` on the
field, just co-located with the annotation that causes the shape change; setting both to
*different* values fails fast at metamodel build time.

Note that `$expand` is unaffected either way: `$expand` entries are matched by the *associated
entity's* name, not the owning field's property name (see `ExpansionPathTracker`), the same as
for every other association kind.

### Speedy Sensitive

Prevent fields from being used in `$` field references in queries.
Applies to both fields and entity classes.

**Field-Level Sensitivity**

```java
@Table(name = "entity")
@Entity
public class Entity {

    // Cannot be referenced via $secretField in query conditions
    @SpeedySensitive
    @Column(name = "secret_field")
    private String secretField;

    @Column(name = "public_field")
    private String publicField;
}
```

**Entity-Level Sensitivity**

All fields inherit sensitivity by default; individual fields can opt out:

```java
@SpeedySensitive
@Table(name = "entity")
@Entity
public class Entity {

    @Column(name = "field_a")
    private String fieldA;  // Inherits sensitivity from class

    @SpeedySensitive(false) // Override — allowed in $ references
    @Column(name = "field_b")
    private String fieldB;
}
```

See [Field References](field-references.md#sensitivity-control-with-speedysensitive) for runtime behavior.

### Speedy Type

Override the `ColumnType` that Speedy infers from a field's Java type. This is useful when
the database column type differs from what the Java type normally maps to, and you want
Speedy's query builder to use the correct SQL type.

```java
import com.github.silent.samurai.speedy.annotations.SpeedyType;
import com.github.silent.samurai.speedy.enums.ColumnType;

@Table(name = "entity")
@Entity
public class Entity {

    @SpeedyType(ColumnType.TEXT)
    @Column(name = "description")
    private String description;  // Inferred: VARCHAR → Overridden: TEXT

    @SpeedyType(ColumnType.BIGINT)
    @Column(name = "count")
    private Integer count;  // Inferred: INTEGER → Overridden: BIGINT

    @SpeedyType(ColumnType.FLOAT)
    @Column(name = "amount")
    private Double amount;  // Inferred: DOUBLE → Overridden: FLOAT
}
```

| `ColumnType`          | Java equivalent typically inferred from        | ValueType family |
|-----------------------|------------------------------------------------|------------------|
| `VARCHAR`             | `String`                                       | TEXT             |
| `TEXT`                | — (must override)                              | TEXT             |
| `CHAR`                | — (must override)                              | TEXT             |
| `UUID`                | `java.util.UUID`                               | TEXT             |
| `INTEGER`             | `int`, `Integer`, `long`, `Long`, `short`      | INT              |
| `SMALLINT`            | — (must override)                              | INT              |
| `BIGINT`              | `BigInteger` or override                       | INT              |
| `FLOAT`               | `float`, `Float` or override                   | FLOAT            |
| `DOUBLE`              | `double`, `Double`                             | FLOAT            |
| `DECIMAL`             | `BigDecimal` or override                       | FLOAT            |
| `NUMERIC`             | — (must override)                              | FLOAT            |
| `REAL`                | — (must override)                              | FLOAT            |
| `BOOLEAN`             | `boolean`, `Boolean`                           | BOOL             |
| `DATE`                | `java.sql.Date`, `java.util.Date`, `LocalDate` | DATE             |
| `TIME`                | `LocalTime`                                    | TIME             |
| `TIMESTAMP`           | `LocalDateTime`, `Timestamp`                   | DATE_TIME        |
| `TIMESTAMP_WITH_ZONE` | `ZonedDateTime`, `OffsetDateTime`, `Instant`   | ZONED_DATE_TIME  |
| `BLOB` / `CLOB`       | — (must override)                              | OBJECT           |

### Jpa Entity

**User Entity**

```java

@Setter
@Getter
@Table(name = "users")
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    protected UUID id;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @Column(name = "email", nullable = false, length = 250)
    private String email;

    @Column(name = "type", nullable = false, length = 512)
    private String type;

    @SpeedyAction(ActionType.READ)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @SpeedyAction(ActionType.READ)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @SpeedyAction(ActionType.READ)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_login_at")
    private LocalDate lastLoginDate;

    @Column(name = "login_count")
    private Integer loginCount;
}
```
