package com.github.silent.samurai.speedy.interfaces.query;

/// # Identifier Expression Record
/// 
/// A record implementation of [Expression] that represents field references in query conditions.
/// This class wraps a [QueryField] to enable field-to-field comparisons and dynamic query building
/// where the comparison value comes from another database column rather than a literal value.
/// 
/// ## Purpose
/// 
/// The `Identifier` record is used to represent database field references in query expressions,
/// enabling comparisons between different database columns. This is essential for complex queries
/// where you need to compare values from different fields within the same or related tables.
/// 
/// ## Field Types Supported
/// 
/// Through the wrapped [QueryField], this record supports:
/// - **Normal fields**: Direct column references from the primary table
/// - **Associated fields**: Foreign key relationships and joined table columns
/// - **Computed fields**: Derived or calculated field references
/// 
/// ## Usage Examples
/// 
/// ### Simple Field Comparison
/// ```java
/// // Compare two fields in the same table: WHERE start_date <= end_date
/// QueryField startDateField = conditionFactory.createQueryField("startDate");
/// QueryField endDateField = conditionFactory.createQueryField("endDate");
/// 
/// Identifier endDateIdentifier = new Identifier(endDateField);
/// BinaryCondition condition = new LessThanEqualCondition(startDateField, endDateIdentifier);
/// ```
/// 
/// ### Associated Field Comparison
/// ```java
/// // Compare fields across related tables: WHERE user.created_date >= profile.last_login
/// QueryField userCreatedField = conditionFactory.createQueryField("user.createdDate");
/// QueryField profileLoginField = conditionFactory.createQueryField("profile.lastLogin");
/// 
/// Identifier profileLoginIdentifier = new Identifier(profileLoginField);
/// BinaryCondition condition = new GreaterThanEqualCondition(userCreatedField, profileLoginIdentifier);
/// ```
/// 
/// ### Price Comparison Example
/// ```java
/// // Find products where sale price is less than regular price: WHERE sale_price < regular_price
/// QueryField salePriceField = conditionFactory.createQueryField("salePrice");
/// QueryField regularPriceField = conditionFactory.createQueryField("regularPrice");
/// 
/// Identifier regularPriceIdentifier = new Identifier(regularPriceField);
/// BinaryCondition condition = new LessThanCondition(salePriceField, regularPriceIdentifier);
/// ```
/// 
/// ### Equality Check Between Fields
/// ```java
/// // Check if two fields have the same value: WHERE created_by = updated_by
/// QueryField createdByField = conditionFactory.createQueryField("createdBy");
/// QueryField updatedByField = conditionFactory.createQueryField("updatedBy");
/// 
/// Identifier updatedByIdentifier = new Identifier(updatedByField);
/// BinaryCondition condition = new EqCondition(createdByField, updatedByIdentifier);
/// ```
/// 
/// ## SQL Generation
/// 
/// When processed by the JOOQ query builder, `Identifier` expressions generate SQL that compares
/// two database columns directly:
/// 
/// ```sql
/// -- Instead of: WHERE column = 'literal_value'
/// -- Generates: WHERE column1 = column2
/// -- Or: WHERE table1.column1 > table2.column2 (for joins)
/// ```
/// 
/// ## Integration with Query Building
/// 
/// The `Identifier` record integrates with:
/// - **JOOQ query building**: Converts to proper SQL field references
/// - **Join handling**: Manages table aliases for associated fields
/// - **Condition factories**: Used in all binary condition types
/// - **Query validation**: Ensures field references are valid
/// 
/// ## Type Safety
/// 
/// As part of the sealed [Expression] hierarchy, `Identifier` provides:
/// - Compile-time type checking for field references
/// - Exhaustive pattern matching support
/// - Clear separation from literal values ([Literal])
/// - Validation of field existence and accessibility
/// 
/// @param field the QueryField representing the database column to reference
/// 
/// @since 1.0
/// @see Expression
/// @see QueryField
/// @see Literal
/// @see BinaryCondition
public record Identifier(QueryField field) implements Expression {

}
