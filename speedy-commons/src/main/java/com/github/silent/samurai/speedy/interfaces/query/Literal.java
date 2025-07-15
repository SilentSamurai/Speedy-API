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
/// SpeedyValue nameValue = SpeedyValueFactory.fromString("John Doe");
/// Literal nameLiteral = new Literal(nameValue);
/// 
/// // Used in condition: WHERE name = 'John Doe'
/// BinaryCondition condition = new EqCondition(userNameField, nameLiteral);
/// ```
/// 
/// ### Numeric Literal
/// ```java
/// SpeedyValue ageValue = SpeedyValueFactory.fromInteger(25);
/// Literal ageLiteral = new Literal(ageValue);
/// 
/// // Used in condition: WHERE age >= 25
/// BinaryCondition condition = new GreaterThanEqualCondition(ageField, ageLiteral);
/// ```
/// 
/// ### Collection Literal (for IN operations)
/// ```java
/// List<String> statusList = Arrays.asList("ACTIVE", "PENDING", "APPROVED");
/// SpeedyValue statusCollection = SpeedyValueFactory.fromCollection(statusList);
/// Literal statusLiteral = new Literal(statusCollection);
/// 
/// // Used in condition: WHERE status IN ('ACTIVE', 'PENDING', 'APPROVED')
/// BinaryCondition condition = new InCondition(statusField, statusLiteral);
/// ```
/// 
/// ### Null Literal
/// ```java
/// SpeedyValue nullValue = SpeedyValueFactory.fromNull();
/// Literal nullLiteral = new Literal(nullValue);
/// 
/// // Used in condition: WHERE deleted_at IS NULL
/// BinaryCondition condition = new EqCondition(deletedAtField, nullLiteral);
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
/// 
/// @since 1.0
/// @see Expression
/// @see SpeedyValue
/// @see Identifier
/// @see BinaryCondition
public record Literal(SpeedyValue value) implements Expression {
}
