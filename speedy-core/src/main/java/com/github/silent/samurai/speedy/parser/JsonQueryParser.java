package com.github.silent.samurai.speedy.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/// # JsonQueryBuilder
/// 
/// A builder class that converts JSON query objects into [SpeedyQuery] instances for the Speedy API framework.
/// This class parses JSON query structures and constructs type-safe query objects that can be executed
/// by various query processors (JOOQ, JPA, etc.).
/// 
/// ## Purpose
/// 
/// The `JsonQueryBuilder` serves as the primary parser for JSON-based query requests in the Speedy API.
/// It transforms JSON query objects into structured [SpeedyQuery] instances that encapsulate all query
/// components including conditions, ordering, pagination, field selection, and entity expansion.
/// 
/// ## Supported Query Structure
/// 
/// The builder supports the following JSON query format:
/// ```json
/// {
///   "$from": "entity_name",
///   "$where": {
///     "field1": "value1",
///     "field2": { "$eq": "value2" },
///     "field3": { "$in": ["value3", "value4"] },
///     "$and": [
///       { "field4": { "$gt": 10 } },
///       { "field5": { "$lt": 100 } }
///     ],
///     "$or": [
///       { "field6": "value6" },
///       { "field7": "value7" }
///     ]
///   },
///   "$select": ["field1", "field2"],
///   "$expand": ["relation1", "relation2.nested"],
///   "$orderBy": { "field1": "ASC", "field2": "DESC" },
///   "$page": { "$index": 0, "$size": 10 }
/// }
/// ```
/// 
/// ## Supported Operators
/// 
/// The builder supports all standard comparison operators:
/// - **$eq**: Equal to (also supports shorthand: `"field": "value"`)
/// - **$ne**: Not equal to
/// - **$gt**: Greater than
/// - **$lt**: Less than
/// - **$gte**: Greater than or equal
/// - **$lte**: Less than or equal
/// - **$in**: In collection
/// - **$nin**: Not in collection
/// - **$matches**: Pattern matching
/// 
/// ## Field References
/// 
/// The builder supports field-to-field comparisons using the `$` prefix:
/// ```JSON
/// {
///   "$where": {
///     "field1": "$field2",  // Compare field1 with field2
///     "field3": { "$lt": "$field4" }  // field3 < field4
///   }
/// }
/// ```
/// 
/// ## Usage Examples
/// 
/// ### Basic Query Construction
/// ```java
/// String jsonQuery = """
/// {
///   "$from": "Product",
///   "$where": {
///     "category": "electronics",
///     "price": { "$gte": 100 }
///   }
/// }
/// """;
/// 
/// JsonNode rootNode = objectMapper.readTree(jsonQuery);
/// JsonQueryBuilder builder = new JsonQueryBuilder(metaModel, "Product", rootNode);
/// SpeedyQuery query = builder.build();
/// ```
/// 
/// ### Complex Query with Boolean Logic
/// ```java
/// String jsonQuery = """
/// {
///   "$from": "User",
///   "$where": {
///     "$and": [
///       { "age": { "$gte": 18 } },
///       { "active": true },
///       {
///         "$or": [
///           { "role": "admin" },
///           { "role": "moderator" }
///         ]
///       }
///     ]
///   },
///   "$orderBy": { "createdAt": "DESC" },
///   "$page": { "$index": 0, "$size": 20 }
/// }
/// """;
/// 
/// JsonQueryBuilder builder = new JsonQueryBuilder(metaModel, rootNode);
/// SpeedyQuery query = builder.build();
/// ```
/// 
/// ### Query with Field References and Expansion
/// ```java
/// String jsonQuery = """
/// {
///   "$from": "Order",
///   "$where": {
///     "totalAmount": { "$gt": "$minimumOrder" },
///     "status": "pending"
///   },
///   "$expand": ["customer", "customer.profile"],
///   "$select": ["id", "totalAmount", "customer.name"]
/// }
/// """;
/// 
/// JsonQueryBuilder builder = new JsonQueryBuilder(metaModel, rootNode);
/// SpeedyQuery query = builder.build();
/// ```
/// 
/// ## Constructor Variants
/// 
/// The builder provides three constructor variants to accommodate different initialization scenarios:
/// 
/// 1. **Entity by Name**: `JsonQueryBuilder(metaModel, entityName, rootNode)`
/// 2. **Entity by Metadata**: `JsonQueryBuilder(metaModel, entityMetadata, rootNode)`
/// 3. **Entity from JSON**: `JsonQueryBuilder(metaModel, rootNode)` - extracts entity from `$from` field
/// 
/// ## Error Handling
/// 
/// The builder throws specific exceptions for different error scenarios:
/// - **[BadRequestException]**: Invalid query structure, unsupported operators, or malformed JSON
/// - **[NotFoundException]**: Entity not found in the meta model
/// - **[SpeedyHttpException]**: General parsing or processing errors
/// 
/// ## Integration
/// 
/// The `JsonQueryBuilder` integrates with:
/// - **Query Handlers**: Used by [QueryHandler] to process JSON request bodies
/// - **Query Processors**: Generates [SpeedyQuery] objects for JOOQ, JPA, or other processors
/// - **Condition Factory**: Leverages [ConditionFactory] for condition creation
/// - **Meta Model**: Validates entities and fields against the application's meta model
/// 
/// @since 1.0
/// @see SpeedyQuery
/// @see ConditionFactory
/// @see QueryHandler
/// @see Expression
/// @see BinaryCondition
/// @see BooleanCondition
public class JsonQueryParser {

    /// The meta model containing entity and field metadata for validation and query building.
    final MetaModel metaModel;
    
    /// The root JSON node containing the complete query structure.
    final JsonNode rootNode;
    
    /// The internal query implementation that gets built during the parsing process.
    final SpeedyQueryImpl speedyQuery;

    /// Factory for creating various types of query conditions.
    final ConditionFactory conditionFactory;

    /// Creates a new JsonQueryBuilder with the specified entity name.
    /// 
    /// This constructor is used when the entity name is known in advance and provided
    /// as a parameter. The entity will be looked up in the metamodel during construction.
    /// 
    /// @param metaModel the meta model containing entity metadata
    /// @param from the name of the entity to query
    /// @param rootNode the JSON root node containing the query structure
    /// @throws NotFoundException if the specified entity is not found in the metamodel
    public JsonQueryParser(MetaModel metaModel, String from, JsonNode rootNode) throws NotFoundException {
        this.metaModel = metaModel;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(metaModel.findEntityMetadata(from));
        this.conditionFactory = speedyQuery.getConditionFactory();
    }

    /// Creates a new JsonQueryBuilder with the specified entity metadata.
    /// 
    /// This constructor is used when the entity metadata is already available,
    /// avoiding the need to look it up in the meta model.
    /// 
    /// @param metaModel the meta model containing entity metadata
    /// @param entityMetadata the metadata of the entity to query
    /// @param rootNode the JSON root node containing the query structure
    /// @throws BadRequestException if the query structure is invalid
    /// @throws NotFoundException if the entity metadata is invalid
    public JsonQueryParser(MetaModel metaModel, EntityMetadata entityMetadata, JsonNode rootNode) throws BadRequestException, NotFoundException {
        this.metaModel = metaModel;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(entityMetadata);
        this.conditionFactory = speedyQuery.getConditionFactory();
    }

    /// Creates a new JsonQueryBuilder and extracts the entity name from the JSON.
    /// 
    /// This constructor extracts the entity name from the `$from` field in the JSON
    /// and looks it up in the meta model. This is useful when the entity name
    /// is part of the query structure itself.
    /// 
    /// @param metaModel the meta model containing entity metadata
    /// @param rootNode the JSON root node containing the query structure
    /// @throws BadRequestException if the query structure is invalid or `$from` is missing/invalid
    /// @throws NotFoundException if the entity specified in `$from` is not found
    public JsonQueryParser(MetaModel metaModel, JsonNode rootNode) throws BadRequestException, NotFoundException {
        this.metaModel = metaModel;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(metaModel.findEntityMetadata(getFrom()));
        this.conditionFactory = speedyQuery.getConditionFactory();
    }

    /// Extracts the entity name from the `$from` field in the JSON root node.
    /// 
    /// This method validates that the `$from` field exists and contains a string value.
    /// It's used by the constructor that extracts the entity name from the JSON.
    /// 
    /// @return the entity name as a string
    /// @throws BadRequestException if `$from` is missing, not a string, or invalid
    String getFrom() throws BadRequestException {
        if (rootNode.has("$from")) {
            JsonNode jsonNode = rootNode.get("$from");
            if (jsonNode.isTextual()) {
                return jsonNode.asText();
            }
        }
        throw new BadRequestException("$from must be a string");
    }

    /// Captures a single binary query condition from a field node.
    /// 
    /// This method handles two types of field conditions:
    /// 1. **Shorthand equality**: `"field": "value"` → `field = value`
    /// 2. **Explicit operator**: `"field": { "$eq": "value" }` → `field = value`
    /// 
    /// The method also handles special cases like:
    /// - **Collection operators** (`$in`, `$nin`) with array values
    /// - **Field references** using the `$` prefix
    /// - **Multiple value operators** that accept arrays
    /// 
    /// @param fieldName the name of the field being queried
    /// @param fieldNode the JSON node containing the field's condition
    /// @return a BinaryCondition representing the field's query condition
    /// @throws SpeedyHttpException if the condition cannot be parsed or is invalid
    BinaryCondition captureSingleBinaryQuery(String fieldName, JsonNode fieldNode) throws SpeedyHttpException {
        QueryField queryField = speedyQuery.getConditionFactory().createQueryField(fieldName);
        // if equals short-handed a: b
        if (fieldNode.isValueNode()) {
            Expression expression = buildExpression(queryField.getMetadataForParsing(), (ValueNode) fieldNode);
            return conditionFactory.createBiCondition(queryField, ConditionOperator.EQ, expression);
        }
        // if operator is provided a: { $eq: b }
        if (fieldNode.isObject()) {
            //  fieldNode = { $eq : b }
            for (Iterator<String> it2 = fieldNode.fieldNames(); it2.hasNext(); ) {
                // capture operator
                String operatorSymbol = it2.next();
                // parse operator
                ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);
                // capture value node after operator
                JsonNode valueNode = fieldNode.get(operatorSymbol);

                // if the operator is $in or $nin and the value is an array, a : { $in : [b, c] }
                if (operator.doesAcceptMultipleValues() && valueNode.isArray()) {
                    List<SpeedyValue> speedyValueList = new LinkedList<>();
                    for (JsonNode node : valueNode) {
                        if (node.isValueNode()) {
                            SpeedyValue speedyValue = SpeedyValueFactory.fromJsonValue(queryField.getMetadataForParsing(), (ValueNode) node);
                            speedyValueList.add(speedyValue);
                        }
                    }
                    SpeedyCollection fieldValue = SpeedyValueFactory.fromCollection(speedyValueList);
                    // standard SQL does not support the syntax: COLA IN [COLB, COLC]
                    return conditionFactory.createBiCondition(queryField, operator, new Literal(fieldValue));
                }
                // if operator is $eq $lte $matches , value will be basic a : { $matches : "sup*" }
                if (valueNode.isValueNode()) {
                    Expression expression = buildExpression(queryField.getMetadataForParsing(), (ValueNode) valueNode);
                    return conditionFactory.createBiCondition(queryField, operator, expression);
                }
            }
        }
        throw new BadRequestException("Invalid query");
    }

    /// Creates an AND condition from an object containing multiple field conditions.
    /// 
    /// This method processes an object where each field represents a separate condition
    /// and combines them with AND logic. For example:
    /// ```json
    /// {
    ///   "field1": "value1",
    ///   "field2": { "$gt": 10 },
    ///   "field3": { "$in": ["a", "b", "c"] }
    /// }
    /// ```
    /// Creates: `field1 = value1 AND field2 > 10 AND field3 IN ('a', 'b', 'c')`
    /// 
    /// @param objectNode the JSON object containing multiple field conditions
    /// @return a BooleanCondition with AND operator containing all sub-conditions
    /// @throws SpeedyHttpException if any of the field conditions cannot be parsed
    BooleanCondition createAndCondition(ObjectNode objectNode) throws SpeedyHttpException {
        // create $and condition from object: { a : { $eq : b }, c : d }
        BooleanCondition booleanCondition = new BooleanConditionImpl(ConditionOperator.AND);
        for (Iterator<String> it2 = objectNode.fieldNames(); it2.hasNext(); ) {
            String fieldName = it2.next();
            JsonNode fieldNode = objectNode.get(fieldName);
            // create a sub query on above $and query
            BinaryCondition binaryCondition = captureSingleBinaryQuery(fieldName, fieldNode);
            booleanCondition.addSubCondition(binaryCondition);
        }
        return booleanCondition;
    }

    /// Creates a boolean condition (AND/OR) from a JSON node.
    /// 
    /// This method handles complex boolean logic by recursively processing:
    /// - **$and** conditions: All sub-conditions must be true
    /// - **$or** conditions: At least one sub-condition must be true
    /// - **Implicit AND**: When no explicit boolean operator is specified
    /// 
    /// The method supports nested boolean conditions and can handle both
    /// array and object representations of boolean conditions.
    /// 
    /// @param jsonNode the JSON node containing boolean conditions
    /// @return a BooleanCondition representing the boolean logic
    /// @throws SpeedyHttpException if the boolean condition structure is invalid
    BooleanCondition createBooleanCondition(ObjectNode jsonNode) throws SpeedyHttpException {
        if (jsonNode.hasNonNull("$or")) {
            BooleanCondition booleanCondition = new BooleanConditionImpl(ConditionOperator.OR);
            JsonNode conditionNode = jsonNode.get("$or");
            if (conditionNode.isArray()) {
                for (JsonNode node : conditionNode) {
                    if (node.isObject()) {
                        BooleanCondition andCondition = createBooleanCondition((ObjectNode) node);
                        booleanCondition.addSubCondition(andCondition);
                    } else {
                        throw new BadRequestException("Invalid query ");
                    }
                }
                return booleanCondition;
            }
            if (conditionNode.isObject()) {
                return createAndCondition((ObjectNode) conditionNode);
            }
        } else if (jsonNode.hasNonNull("$and")) {
            JsonNode conditionNode = jsonNode.get("$and");
            if (conditionNode.isArray()) {
                BooleanCondition booleanCondition = new BooleanConditionImpl(ConditionOperator.AND);
                for (JsonNode node : conditionNode) {
                    if (node.isObject()) {
                        BooleanCondition andCondition = createBooleanCondition((ObjectNode) node);
                        booleanCondition.addSubCondition(andCondition);
                    }
                }
                return booleanCondition;
            }
            if (conditionNode.isObject()) {
                return createAndCondition((ObjectNode) conditionNode);
            }
        }
        return createAndCondition(jsonNode);
    }

    /// Builds an Expression from a JSON value node.
    /// 
    /// This method determines whether a value represents a literal or a field reference:
    /// - **Field Reference**: Values starting with `$` are treated as field references
    /// - **Literal Value**: All other values are treated as literal constants
    /// 
    /// Field references are converted to [Identifier] expressions, while literal
    /// values are converted to [Literal] expressions using [SpeedyValueFactory].
    /// 
    /// @param metadata the field metadata for type conversion
    /// @param symbol the JSON value node to convert
    /// @return an Expression (either Identifier or Literal)
    /// @throws SpeedyHttpException if the conversion fails
    Expression buildExpression(FieldMetadata metadata, ValueNode symbol) throws SpeedyHttpException {
        if (symbol.isTextual() && symbol.asText().startsWith("$")) {
            String field = symbol.asText().substring(1);
            QueryField queryField = this.conditionFactory.createQueryField(field);
            return new Identifier(queryField);
        } else {
            return new Literal(SpeedyValueFactory.fromJsonValue(metadata, symbol));
        }
    }

    /// Builds the WHERE clause from the JSON query.
    /// 
    /// This method processes the `$where` field in the JSON query and creates
    /// the corresponding condition structure. It validates that the `$where`
    /// field is an object and delegates to [createBooleanCondition] for processing.
    /// 
    /// @throws SpeedyHttpException if the WHERE clause is invalid or cannot be parsed
    void buildWhere() throws SpeedyHttpException {
        if (rootNode.has("$where")) {
            JsonNode jsonNode = rootNode.get("$where");
            if (jsonNode.isObject()) {
                speedyQuery.setWhere(createBooleanCondition((ObjectNode) jsonNode));
                return;
            }
            throw new BadRequestException("$where must be an object");
        }
    }

    /// Builds the ORDER BY clause from the JSON query.
    /// 
    /// This method processes the `$orderBy` field and adds ordering instructions
    /// to the query. It supports both ascending (`ASC`) and descending (`DESC`)
    /// ordering for multiple fields.
    /// 
    /// Expected format:
    /// ```json
    /// {
    ///   "$orderBy": {
    ///     "field1": "ASC",
    ///     "field2": "DESC"
    ///   }
    /// }
    /// ```
    /// 
    /// @throws NotFoundException if a field in the ORDER BY clause is not found
    /// @throws BadRequestException if the ORDER BY clause format is invalid
    void buildOrderBy() throws NotFoundException, BadRequestException {
        if (rootNode.has("$orderBy")) {
            JsonNode jsonNode = rootNode.get("$orderBy");
            if (jsonNode.isObject()) {
                for (Iterator<String> it2 = jsonNode.fieldNames(); it2.hasNext(); ) {
                    String fieldName = it2.next();
                    JsonNode fieldNode = jsonNode.get(fieldName);
                    if (fieldNode.isTextual() && fieldNode.asText().equalsIgnoreCase("ASC")) {
                        speedyQuery.orderByAsc(fieldName);
                    } else if (fieldNode.isTextual() && fieldNode.asText().equalsIgnoreCase("DESC")) {
                        speedyQuery.orderByDesc(fieldName);
                    } else {
                        throw new BadRequestException("order by should be field name: asc|desc");
                    }
                }
            }
        }
    }

    /// Builds the pagination information from the JSON query.
    /// 
    /// This method processes the `$page` field and sets the pagination parameters
    /// for the query. Both `$index` (page number) and `$size` (page size) are optional.
    /// 
    /// Expected format:
    /// ```json
    /// {
    ///   "$page": {
    ///     "$index": 0,
    ///     "$size": 10
    ///   }
    /// }
    /// ```
    /// 
    /// If `$index` is not provided, it defaults to 0. If `$size` is not provided,
    /// it uses the default page size configured in the query implementation.
    void buildPaging() {
        if (rootNode.has("$page")) {
            JsonNode jsonNode = rootNode.get("$page");
            if (jsonNode.isObject()) {
                if (jsonNode.hasNonNull("$index")) {
                    speedyQuery.addPageNo(jsonNode.get("$index").asInt());
                }
                if (jsonNode.hasNonNull("$size")) {
                    speedyQuery.addPageSize(jsonNode.get("$size").asInt());
                }
            }
        }
    }

    /// Builds the expansion information from the JSON query.
    /// 
    /// This method processes the `$expand` field and adds entity expansion
    /// instructions to the query. It supports both simple entity names and
    /// dot notation for nested expansions.
    /// 
    /// Expected format:
    /// ```json
    /// {
    ///   "$expand": ["entity1", "entity2.nested", "entity3.nested.deeper"]
    /// }
    /// ```
    /// 
    /// The method validates expansion paths to ensure they are properly formatted
    /// and don't contain empty segments.
    /// 
    /// @throws SpeedyHttpException if the expansion format is invalid
    void buildExpand() throws SpeedyHttpException {
        if (rootNode.has("$expand")) {
            JsonNode jsonNode = rootNode.get("$expand");
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    if (node.isTextual()) {
                        String expansion = node.asText();
                        // Validate dot notation expansion
                        validateExpansionPath(expansion);
                        speedyQuery.addExpand(expansion);
                    }
                }
            }
        }
    }

    /// Validates an expansion path for proper formatting.
    /// 
    /// This method ensures that expansion paths using dot notation are properly
    /// formatted and don't contain empty segments. It's used by [buildExpand]
    /// to validate expansion paths before adding them to the query.
    /// 
    /// @param expansion the expansion path to validate
    /// @throws BadRequestException if the expansion path is invalid
    private void validateExpansionPath(String expansion) throws BadRequestException {
        if (expansion.contains(".")) {
            String[] parts = expansion.split("\\.");
            if (parts.length < 2) {
                throw new BadRequestException("Invalid expansion path: " + expansion);
            }

            // Validate that each part is a valid entity/association name
            for (String part : parts) {
                if (part.trim().isEmpty()) {
                    throw new BadRequestException("Invalid expansion path: " + expansion + " - empty segment");
                }
            }
        }
    }

    /// Builds the field selection information from the JSON query.
    /// 
    /// This method processes the `$select` field and adds field selection
    /// instructions to the query. It supports both single field names and
    /// arrays of field names.
    /// 
    /// Expected format:
    /// ```json
    /// {
    ///   "$select": ["field1", "field2", "field3"]
    /// }
    /// ```
    /// 
    /// Or single field:
    /// ```json
    /// {
    ///   "$select": "field1"
    /// }
    /// ```
    /// 
    /// If no `$select` is specified, all fields are returned by default.
    void buildSelect() {
        if (rootNode.has("$select")) {
            JsonNode jsonNode = rootNode.get("$select");
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    if (node.isTextual()) {
                        speedyQuery.addSelect(node.asText());
                    }
                }
            } else if (jsonNode.isTextual()) {
                speedyQuery.addSelect(jsonNode.asText());
            }
        }
    }

    /// Builds the complete SpeedyQuery from the JSON structure.
    /// 
    /// This method orchestrates the parsing of all query components in the correct order:
    /// 1. **Field Selection** (`$select`) - determines which fields to return
    /// 2. **WHERE Conditions** (`$where`) - filters the results
    /// 3. **Ordering** (`$orderBy`) - sorts the results
    /// 4. **Pagination** (`$page`) - controls result pagination
    /// 5. **Expansion** (`$expand`) - includes related entities
    /// 
    /// The method returns a fully constructed [SpeedyQuery] object that can be
    /// executed by query processors or used for further processing.
    /// 
    /// @return a complete SpeedyQuery object ready for execution
    /// @throws SpeedyHttpException if any part of the query cannot be parsed or is invalid
    public SpeedyQuery build() throws SpeedyHttpException {
        buildSelect();
        buildWhere();
        buildOrderBy();
        buildPaging();
        buildExpand();
        return speedyQuery;
    }
}
