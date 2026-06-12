# Custom Types

Speedy lets you register custom Java types that are automatically converted across all
edges — JSON in/out, database columns, query-string parameters, and Java POJO conversion
(event handlers, validators).

## Overview

A custom type is any Java class you want to use in your entities instead of a primitive
or standard type (e.g. `Email`, `Money`, `PhoneNumber`). You register it once via a
`SpeedyTypeModule`, and Speedy handles the conversions everywhere.

## Quick Start — `Email` Type

### 1. Create your value class

```java
public final class Email {
    private final String value;

    public Email(String value) {
        // your validation logic
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
        this.value = value;
    }

    public String getValue() { return value; }

    @Override
    public String toString() { return value; }
}
```

### 2. Add a JPA `AttributeConverter`

This teaches Hibernate how to store the custom type in the database.

```java
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmailConverter implements AttributeConverter<Email, String> {
    public String convertToDatabaseColumn(Email email) {
        return email == null ? null : email.toString();
    }
    public Email convertToEntityAttribute(String s) {
        return s == null ? null : new Email(s);
    }
}
```

> **`autoApply = true`** makes the converter apply to every `Email` field automatically.

### 3. Create a `SpeedyTypeModule`

This tells Speedy how to convert the type across all edges. For a type that is
represented as text everywhere (JSON, database, query-string), you only need one line:

```java
import com.github.silent.samurai.speedy.mappings.ConversionContext;
import com.github.silent.samurai.speedy.mappings.SpeedyTypeModule;

public class EmailTypeModule implements SpeedyTypeModule {
    @Override
    public void contribute(ConversionContext ctx) {
        ctx.forType(Email.class)
                .asText(Email::toString, Email::new);
    }
}
```

### 4. Register the module in your config

```java
import com.github.silent.samurai.speedy.mappings.SpeedyTypeModule;

@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {
    // ... other methods ...

    @Override
    public List<SpeedyTypeModule> typeModules() {
        return List.of(new EmailTypeModule());
    }
}
```

### 5. Use the type in an entity

```java
import com.github.silent.samurai.speedy.annotations.SpeedyType;
import com.github.silent.samurai.speedy.enums.ColumnType;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @SpeedyType(ColumnType.VARCHAR)
    @Convert(converter = EmailConverter.class)
    @Column(nullable = false, length = 320)
    private Email email;
}
```

> **`@SpeedyType(ColumnType.VARCHAR)`** is needed because Speedy's metamodel
> processor does not know the column type for `Email.class` automatically.

## How It Works

When you call `ctx.forType(Email.class).asText(...)`, Speedy registers converters
on three internal registries:

| Edge | Registry Key | Effect |
|---|---|---|
| JSON in/out | `JsonRegistry` (keyed by `ValueType.TEXT`) | Email serializes as `"user@example.com"` in JSON |
| Java POJO | `JavaTypeRegistry` (keyed by `Email.class`) | Event handlers receive `Email` objects, not raw strings |
| DB column | `JooqConverters` default `VARCHAR` → `SpeedyText` | Already handled — no extra config needed |

### Custom DB or JSON behaviour

If your type needs different representations on different edges, use the per-edge
overrides:

```java
ctx.forType(Money.class)
    .asText(Money::toString, Money::parse)       // JSON + POJO + query-string
    .onDb(ColumnType.NUMERIC,                      // DB stores as BigDecimal
        sv  -> BigDecimal.valueOf(...),
        raw -> Money.of((BigDecimal) raw));
```

The `TypeBuilder` fluent API provides:

| Method | Purpose |
|---|---|
| `asText(enc, dec)` | Register for all text-based edges (JSON, POJO, query-string) |
| `onDb(colType, enc, dec)` | Override DB conversion only |
| `onJson(vt, enc, dec)` | Override JSON conversion only |
| `onJava(vt, enc, dec)` | Override Java POJO conversion only |
