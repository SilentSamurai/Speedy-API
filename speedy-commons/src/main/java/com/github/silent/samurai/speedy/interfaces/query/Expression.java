package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;

/// # Expression Interface
///
/// A sealed interface representing expressions in query conditions within the Speedy API framework.
/// This interface serves as the base type for all query expressions and enforces type safety through
/// the sealed interface pattern.
///
/// ## Overview
///
/// The `Expression` interface is designed to represent different types of values that can be used
/// in query conditions. It uses Java's sealed interface feature to restrict implementations to only
/// two specific types:
///
/// - **[Literal]** - Represents constant values (strings, numbers, booleans, etc.)
/// - **[Identifier]** - Represents field references for field-to-field comparisons
///
/// ## Usage Examples
///
/// ### Literal Expression
/// ```java
/// // Creating a literal expression for a string value
/// SpeedyValue stringValue = new SpeedyText("John");
/// Expression literalExpr = new Literal(stringValue);
/// ```
///
/// ### Identifier Expression
/// ```java
/// // Creating an identifier expression for field comparison
/// QueryField userField = conditionFactory.createQueryField("user.name");
/// Expression identifierExpr = new Identifier(userField);
/// ```
///
/// ## Type Safety
///
/// The sealed interface pattern ensures that:
/// - Only known implementations can exist
/// - Pattern matching in switch expressions is exhaustive
/// - Compile-time verification of all possible cases
///
/// ## Integration
///
/// This interface is primarily used in:
/// - **Binary conditions** for query filtering
/// - **JOOQ query building** for SQL generation
/// - **JSON query parsing** for API request processing
///
/// @see Literal
/// @see Identifier
/// @see BinaryCondition
/// @since 1.0
public sealed interface Expression permits Literal, Identifier {

}

