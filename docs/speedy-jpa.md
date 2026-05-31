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
