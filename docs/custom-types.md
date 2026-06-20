# Custom Types

Speedy lets you register custom Java types that are automatically handled across all
edges — JSON request/response, database persistence, URL query parameters, and the
event system (POJO conversion in event handlers).

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

```java
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
@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {

    @Override
    public List<SpeedyTypeModule> typeModules() {
        return List.of(new EmailTypeModule());
    }
}
```

### 5. Use the type in an entity

```java
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

> **`@SpeedyType(ColumnType.VARCHAR)`** is required so Speedy's metamodel processor
> knows the database column type for `Email`.

---

## How It Works

Registering via `asText` adds a `Codec<Email>` to `JavaTypeRegistry` — the single
registry that drives every conversion path involving a custom type:

| Path | What happens |
|------|-------------|
| JSON request | `SpeedyText("user@example.com")` → `JavaTypeRegistry` → `Email` |
| JSON response | `Email` → `JavaTypeRegistry` → `SpeedyText` → `"user@example.com"` |
| Event handler | `SpeedyEntity` ↔ `POJO` via `SpeedyToJava` / `JavaToSpeedy` (both use `JavaTypeRegistry`) |
| URL query param | Parsed as `String` → `SpeedyText` → DB filter (no custom registration needed) |
| Database | JPA `@Convert` handles `Email ↔ String`; query processor handles `String ↔ SpeedyText` |

---

## TypeBuilder API

| Method | Use when |
|--------|----------|
| `asText(T::toString, T::new)` | Type is always represented as a `String` — text, email, phone, IBAN, etc. |
| `codec(SpeedyValue → T, T → SpeedyValue)` | Type maps to a non-text `SpeedyValue` — e.g. a numeric or date wrapper |

### Non-text example

```java
// A type whose internal representation is SpeedyDouble
public class Percentage {
    private final double value;
    public Percentage(double value) { this.value = value; }
    public double getValue() { return value; }
}

ctx.forType(Percentage.class)
   .codec(
       sv -> new Percentage(sv.asDouble()),
       p  -> new SpeedyDouble(p.getValue())
   );
```
