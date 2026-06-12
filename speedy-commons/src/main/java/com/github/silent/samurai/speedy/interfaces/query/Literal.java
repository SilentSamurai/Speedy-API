package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

/// # Literal Expression Record
///
/// A record implementation of [Expression] that represents constant values in query conditions.
/// This class wraps a [SpeedyValue] to provide type-safe handling of literal values such as
/// strings, numbers, booleans, dates, and collections.
///
/// ## Purpose
///
/// The `Literal` record is used to represent constant values in query expressions, enabling
/// comparisons between database fields and static values. It serves as a wrapper around
/// [SpeedyValue] to integrate with the Expression type system.
///
/// ## Supported Value Types
///
/// Through the wrapped [SpeedyValue], this record supports:
/// - **Primitive types**: strings, integers, doubles, booleans
/// - **Date/Time types**: LocalDate, LocalDateTime, Instant
/// - **Collections**: arrays and lists for IN/NOT_IN operations
/// - **Null values**: for IS NULL/IS NOT NULL conditions
///
/// ## Usage Examples
///
/// ### String Literal
/// ```java
/// Literal nameLiteral = new Literal(new SpeedyText("John Doe"));
/// // Used in condition: WHERE name = 'John Doe'
/// ```
///
/// ### Numeric Literal
/// ```java
/// Literal ageLiteral = new Literal(new SpeedyInt(25L));
/// // Used in condition: WHERE age >= 25
/// ```
///
/// ### Collection Literal (for IN operations)
/// ```java
/// List<SpeedyValue> values = List.of(new SpeedyText("ACTIVE"), new SpeedyText("PENDING"));
/// Literal statusLiteral = new Literal(new SpeedyCollection(values));
/// // Used in condition: WHERE status IN ('ACTIVE', 'PENDING')
/// ```
///
/// ### Null Literal
/// ```java
/// Literal nullLiteral = new Literal(SpeedyNull.SPEEDY_NULL);
/// // Used in condition: WHERE deleted_at IS NULL
/// ```
///
/// ## Integration with Query Building
///
/// The `Literal` record integrates seamlessly with:
/// - **JOOQ query building**: Converts to SQL parameters and values
/// - **JSON query parsing**: Created from JSON API requests
/// - **Condition factories**: Used in all binary condition types
///
/// ## Type Safety
///
/// As part of the sealed [Expression] hierarchy, `Literal` provides:
/// - Compile-time type checking
/// - Exhaustive pattern matching support
/// - Clear separation from field references ([Identifier])
///
/// @param value the wrapped SpeedyValue containing the actual literal value
/// @see Expression
/// @see SpeedyValue
/// @see Identifier
/// @see BinaryCondition
/// @since 1.0
public record Literal(SpeedyValue value) implements Expression {
}
