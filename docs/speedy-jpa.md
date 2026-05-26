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


**Ignore Entity**

tell speedy to ignore entity

```java
@SpeedyIgnore
@Table(name = "entity")
@Entity
public class Entity {
    
}
```

**Ignore Fields**

tell speedy to ignore entity

```java
import jakarta.persistence.Column;

@Table(name = "entity")
@Entity
public class Entity {

    @SpeedyIgnore
    @Column(name = "internal")
    private String internal;
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
