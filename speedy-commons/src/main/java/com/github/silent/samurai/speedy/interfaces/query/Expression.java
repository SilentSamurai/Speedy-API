package com.github.silent.samurai.speedy.interfaces.query;

import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;

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
/// SpeedyValue stringValue = SpeedyValueFactory.fromString("John");
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
/// @since 1.0
/// @see Literal
/// @see Identifier
/// @see BinaryCondition
public sealed interface Expression permits Literal, Identifier {

    /// Factory method to create an Expression from a JSON ValueNode.
    /// 
    /// This method converts JSON values into appropriate Expression instances
    /// based on the field metadata and value type.
    /// 
    /// @param fieldMetadata the metadata of the field being queried
    /// @param valueNode the JSON value node to convert
    /// @return a new Expression instance (either Literal or Identifier)
    /// @throws SpeedyHttpException if the conversion fails or the value is invalid
    static Expression fromSymbol(FieldMetadata fieldMetadata, ValueNode valueNode) throws SpeedyHttpException {
        return new Literal(SpeedyValueFactory.fromJsonValue(fieldMetadata, valueNode));
    }
}

