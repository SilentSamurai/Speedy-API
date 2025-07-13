package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/// # SpeedyQuery
///
/// A fluent query builder for constructing complex database queries in the Speedy API.
/// This class provides a type-safe and intuitive way to build queries with conditions,
/// ordering, pagination, field selection, and entity expansion.
///
/// ## Usage Examples
///
/// ### Basic Query
/// ```java
/// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
///
/// SpeedyQuery query = from("users")
///     .where(condition("active", eq(true)))
///     .orderByAsc("name")
///     .pageSize(20)
///     .build();
///```
///
/// ### Complex Query with Multiple Conditions
/// ```java
/// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
///
/// SpeedyQuery query = from("users")
///     .where(
///         and(
///             condition("age", gte(18)),
///             condition("active", eq(true)),
///             or(
///                 condition("role", eq("admin")),
///                 condition("role", eq("moderator"))
///)
///)
///)
///     .select("id", "name", "email", "role")
///     .expand("profile", "permissions")
///     .orderByDesc("createdAt")
///     .pageNo(1)
///     .pageSize(50)
///     .build();
///```
///
/// ### Search Query
/// ```java
/// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
///
/// SpeedyQuery query = from("products")
///     .where(
///         and(
///             condition("name", matches("laptop")),
///             condition("price", gte(500)),
///             condition("category", in("electronics", "computers"))
///)
///)
///     .orderByAsc("price")
///     .build();
///```
///
/// ## Query Structure
///
/// The generated query follows this JSON structure:
/// ```json
///{
///   "$from": "entity_name",
///   "$where": { "conditions" },
///   "$select": ["field1", "field2"],
///   "$expand": ["relation1", "relation2"],
///   "$orderBy": { "field": "ASC|DESC" },
///   "$page": { "$index": 0, "$size": 10 }
///}
///```
///
/// ## Available Operations
///
/// | Category | Methods | Description |
/// |----------|---------|-------------|
/// | **Builder** | `from()`, `from(String entity)` | Create new query instances |
/// | **Source** | `from(String entity)` | Set the target entity |
/// | **Conditions** | `where(JsonNode...)` | Add WHERE conditions |
/// | **Selection** | `select(String...)` | Choose fields to return |
/// | **Expansion** | `expand(String...)` | Include related entities |
/// | **Ordering** | `orderByAsc(String)`, `orderByDesc(String)` | Sort results |
/// | **Pagination** | `pageNo(int)`, `pageSize(int)` | Control result pagination |
/// | **Execution** | `build()`, `prettyPrint()` | Generate final query |
///
/// ## Comparison Operators
///
/// | Operator | Method | Description | Example |
/// |----------|--------|-------------|---------|
/// | `$eq` | `eq(Object)` | Equal to | `eq("active")` |
/// | `$ne` | `ne(Object)` | Not equal to | `ne("inactive")` |
/// | `$gt` | `gt(Object)` | Greater than | `gt(18)` |
/// | `$lt` | `lt(Object)` | Less than | `lt(100)` |
/// | `$gte` | `gte(Object)` | Greater than or equal | `gte(80)` |
/// | `$lte` | `lte(Object)` | Less than or equal | `lte(10)` |
/// | `$in` | `in(Object...)` | In array of values | `in("A", "B", "C")` |
/// | `$nin` | `nin(Object...)` | Not in array | `nin("deleted", "archived")` |
/// | `$matches` | `matches(Object)` | Pattern matching | `matches("john.*")` |
///
/// ## Logical Operators
///
/// | Operator | Method | Description | Example |
/// |----------|--------|-------------|---------|
/// | `$and` | `and(JsonNode...)` | Logical AND | `and(cond1, cond2, cond3)` |
/// | `$or` | `or(JsonNode...)` | Logical OR | `or(cond1, cond2)` |
///
/// ## Best Practices
///
/// - **Use static imports**: `import static SpeedyQuery.*` for cleaner code
/// - **Use from pattern**: Always start with `from()`
/// - **Validate inputs**: Check for null/empty values before building
/// - **Use prettyPrint()**: For debugging and logging query structure
/// - **Optimize pagination**: Set reasonable page sizes for performance
/// - **Select specific fields**: Only request needed data to reduce payload
/// - **Use appropriate operators**: Choose the right comparison operator for your use case
///
/// @see [SpeedyClient]
/// @see [SpeedyQueryRequest]
public class SpeedyQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyQuery.class);

    private final ObjectNode root = CommonUtil.json().createObjectNode();
    private final ObjectNode where = CommonUtil.json().createObjectNode();
    private final ObjectNode orderBy = CommonUtil.json().createObjectNode();
    private final ArrayNode expand = CommonUtil.json().createArrayNode();
    private final Set<String> select = new HashSet<>();
    int pageNo = 0;
    int pageSize = 10;

    /// Creates a new SpeedyQuery from with the specified entity.
    ///
    /// This is the recommended way to start building a query. The entity name
    /// will be set as the `$from` field in the generated query.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("active", eq(true)))
    ///     .build();
    ///```
    ///
    /// @param entity the name of the entity to query
    /// @return a new SpeedyQuery from instance
    /// @throws IllegalArgumentException if entity is null or empty
    public static SpeedyQuery from(@NotNull String entity) {
        return new SpeedyQuery().fromEntity(entity);
    }

    /// Creates a new SpeedyQuery from without specifying an entity.
    ///
    /// Use this when you want to set the entity later using the `from()` method.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from()
    ///     .from("users")
    ///     .where(condition("active", eq(true)))
    ///     .build();
    ///```
    ///
    /// @return a new SpeedyQuery from instance
    public static SpeedyQuery from() {
        return new SpeedyQuery();
    }

    /// Converts a value to a JSON node with the specified condition operator.
    ///
    /// This is a utility method used internally by the comparison operators.
    ///
    /// @param value     the value to convert
    /// @param condition the condition operator (e.g., "$eq", "$gt")
    /// @return an ObjectNode with the condition and value
    /// @throws JsonProcessingException if JSON conversion fails
    private static ObjectNode toJsonNode(Object value, String condition) throws JsonProcessingException {
        ObjectNode jsonNodes = CommonUtil.json().createObjectNode();
        JsonNode jsonNode = CommonUtil.json().convertValue(value, JsonNode.class);
        jsonNodes.set(condition, jsonNode);
        return jsonNodes;
    }

    /// Creates an "equal to" condition.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("status", eq("active")))
    ///     .build();
    ///```
    ///
    /// @param value the value to compare against
    /// @return an ObjectNode representing the equality condition
    /// @throws JsonProcessingException if JSON conversion fails
    public static ObjectNode eq(@NotNull Object value) throws JsonProcessingException {
        return toJsonNode(value, "$eq");
    }

    /// Creates a "not equal to" condition.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("status", ne("inactive")))
    ///     .build();
    ///```
    ///
    /// @param value the value to compare against
    /// @return an ObjectNode representing the inequality condition
    /// @throws JsonProcessingException if JSON conversion fails
    public static ObjectNode ne(@NotNull Object value) throws JsonProcessingException {
        return toJsonNode(value, "$ne");
    }

    /// Creates a "greater than" condition.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("age", gt(18)))
    ///     .build();
    ///```
    ///
    /// @param value the value to compare against
    /// @return an ObjectNode representing the greater than condition
    /// @throws JsonProcessingException if JSON conversion fails
    public static ObjectNode gt(@NotNull Object value) throws JsonProcessingException {
        return toJsonNode(value, "$gt");
    }

    /// Creates a "less than" condition.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("products")
    ///     .where(condition("price", lt(100)))
    ///     .build();
    ///```
    ///
    /// @param value the value to compare against
    /// @return an ObjectNode representing the less than condition
    /// @throws JsonProcessingException if JSON conversion fails
    public static ObjectNode lt(@NotNull Object value) throws JsonProcessingException {
        return toJsonNode(value, "$lt");
    }

    /// Creates a "greater than or equal to" condition.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("score", gte(80)))
    ///     .build();
    ///```
    ///
    /// @param value the value to compare against
    /// @return an ObjectNode representing the greater than or equal condition
    /// @throws JsonProcessingException if JSON conversion fails
    public static ObjectNode gte(@NotNull Object value) throws JsonProcessingException {
        return toJsonNode(value, "$gte");
    }

    /// Creates a "less than or equal to" condition.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("products")
    ///     .where(condition("quantity", lte(10)))
    ///     .build();
    ///```
    ///
    /// @param value the value to compare against
    /// @return an ObjectNode representing the less than or equal condition
    /// @throws JsonProcessingException if JSON conversion fails
    public static ObjectNode lte(@NotNull Object value) throws JsonProcessingException {
        return toJsonNode(value, "$lte");
    }

    /// Creates an "in" condition to match against an array of values.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("role", in("admin", "moderator", "user")))
    ///     .build();
    ///```
    ///
    /// @param values the array of values to match against
    /// @return an ObjectNode representing the in condition
    /// @throws JsonProcessingException if JSON conversion fails
    public static ObjectNode in(@NotNull Object... values) throws JsonProcessingException {
        return toJsonNode(values, "$in");
    }

    /// Creates a "not in" condition to exclude an array of values.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("status", nin("deleted", "archived")))
    ///     .build();
    ///```
    ///
    /// @param values the array of values to exclude
    /// @return an ObjectNode representing the not in condition
    /// @throws JsonProcessingException if JSON conversion fails
    public static ObjectNode nin(@NotNull Object... values) throws JsonProcessingException {
        return toJsonNode(values, "$nin");
    }

    /// Creates a "matches" condition for pattern matching.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("name", matches("john.*")))
    ///     .build();
    ///```
    ///
    /// @param values the pattern to match against
    /// @return an ObjectNode representing the matches condition
    /// @throws JsonProcessingException if JSON conversion fails
    public static ObjectNode matches(@NotNull Object values) throws JsonProcessingException {
        return toJsonNode(values, "$matches");
    }

    /// Creates a condition object with a key-value pair.
    ///
    /// This method is used to create condition objects that can be used
    /// with logical operators (and, or).
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(
    ///         and(
    ///             condition("active", eq(true)),
    ///             condition("age", gte(18))
    ///)
    ///)
    ///     .build();
    ///```
    ///
    /// @param key   the field name
    /// @param value the condition value
    /// @return a JsonNode representing the condition
    public static JsonNode condition(@NotNull String key, @NotNull JsonNode value) {
        ObjectNode jsonNodes = CommonUtil.json().createObjectNode();
        jsonNodes.set(key, value);
        return jsonNodes;
    }

    /// Creates an OR logical operator with multiple conditions.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(
    ///         or(
    ///             condition("role", eq("admin")),
    ///             condition("role", eq("moderator"))
    ///)
    ///)
    ///     .build();
    ///```
    ///
    /// @param conditions the conditions to combine with OR logic
    /// @return an ObjectNode representing the OR condition
    public static ObjectNode or(@NotNull JsonNode... conditions) {
        ObjectNode andNode = CommonUtil.json().createObjectNode();
        andNode.set("$or", CommonUtil.json().createArrayNode());
        for (JsonNode condition : conditions) {
            ArrayNode and = (ArrayNode) andNode.get("$or");
            and.add(condition);
        }
        return andNode;
    }

    /// Creates an AND logical operator with multiple conditions.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(
    ///         and(
    ///             condition("active", eq(true)),
    ///             condition("age", gte(18)),
    ///             condition("verified", eq(true))
    ///)
    ///)
    ///     .build();
    ///```
    ///
    /// @param conditions the conditions to combine with AND logic
    /// @return an ObjectNode representing the AND condition
    public static ObjectNode and(@NotNull JsonNode... conditions) {
        ObjectNode andNode = CommonUtil.json().createObjectNode();
        andNode.set("$and", CommonUtil.json().createArrayNode());
        for (JsonNode condition : conditions) {
            ArrayNode and = (ArrayNode) andNode.get("$and");
            and.add(condition);
        }
        return andNode;
    }

    /// Sets the entity to query from.
    ///
    /// This method sets the `$from` field in the generated query, specifying
    /// which entity or table to query.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from()
    ///     .from("users")
    ///     .where(condition("active", eq(true)))
    ///     .build();
    ///```
    ///
    /// @param from the name of the entity to query
    /// @return this SpeedyQuery instance for method chaining
    /// @throws IllegalArgumentException if from is null or empty
    public SpeedyQuery fromEntity(@NotNull String from) {
        if (from == null || from.isEmpty()) {
            throw new IllegalArgumentException("The 'from' parameter cannot be null or empty.");
        }
        root.put("$from", from);
        return this;
    }

    /// Adds WHERE conditions to the query.
    ///
    /// This method accepts one or more condition objects created using the
    /// comparison operators (eq, ne, gt, etc.) or logical operators (and, or).
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(
    ///         and(
    ///             condition("active", eq(true)),
    ///             condition("age", gte(18))
    ///)
    ///)
    ///     .build();
    ///```
    ///
    /// @param conditionObjs one or more condition objects
    /// @return this SpeedyQuery instance for method chaining
    /// @throws IllegalArgumentException if any condition is null or empty
    public SpeedyQuery where(@NotNull JsonNode... conditionObjs) {
        for (JsonNode conditionObj : conditionObjs) {
            if (conditionObj == null || conditionObj.isEmpty()) {
                throw new IllegalArgumentException("The 'where' parameter cannot be null or empty.");
            }
            if (conditionObj.has("$and")) {
                where.removeAll();
                where.set("$and", conditionObj.get("$and"));
                break;
            }
            if (conditionObj.has("$or")) {
                where.removeAll();
                where.set("$or", conditionObj.get("$or"));
                break;
            }
            if (!conditionObj.isEmpty()) {
                String firstField = conditionObj.fieldNames().next();
                where.set(firstField, conditionObj.get(firstField));
            }
        }
        return this;
    }

    /// Adds ascending order by clause for the specified field.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .orderByAsc("name")
    ///     .orderByAsc("createdAt")
    ///     .build();
    ///```
    ///
    /// @param key the field name to order by
    /// @return this SpeedyQuery instance for method chaining
    /// @throws IllegalArgumentException if key is null
    public SpeedyQuery orderByAsc(@NotNull String key) {
        Objects.requireNonNull(key, "Key must not be null");
        orderBy.set(key, new TextNode("ASC"));
        return this;
    }

    /// Adds descending order by clause for the specified field.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .orderByDesc("createdAt")
    ///     .orderByDesc("lastLogin")
    ///     .build();
    ///```
    ///
    /// @param key the field name to order by
    /// @return this SpeedyQuery instance for method chaining
    /// @throws IllegalArgumentException if key is null
    public SpeedyQuery orderByDesc(@NotNull String key) {
        orderBy.set(key, new TextNode("DESC"));
        return this;
    }

    /// Adds a field to expand (include related entities).
    ///
    /// This method adds fields to the `$expand` array, which tells the API
    /// to include related entities in the response.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .expand("profile")
    ///     .expand("permissions")
    ///     .expand("department")
    ///     .build();
    ///```
    ///
    /// @param key the field name to expand
    /// @return this SpeedyQuery instance for method chaining
    /// @throws IllegalArgumentException if key is null or empty
    public SpeedyQuery expand(@NotNull String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Expand key cannot be null or empty.");
        }
        expand.add(key);
        return this;
    }

    /// Sets the page number for pagination (zero-based).
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .pageNo(2)  // Get the third page (0-based indexing)
    ///     .pageSize(20)
    ///     .build();
    ///```
    ///
    /// @param pageNo the page number (0-based)
    /// @return this SpeedyQuery instance for method chaining
    /// @throws IllegalArgumentException if pageNo is less than 0
    public SpeedyQuery pageNo(@NotNull int pageNo) {
        if (pageNo < 0) {
            throw new IllegalArgumentException("Page number must not be less than 0.");
        }
        this.pageNo = pageNo;
        return this;
    }

    /// Sets the page size for pagination.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .pageNo(0)
    ///     .pageSize(50)  // Get 50 records per page
    ///     .build();
    ///```
    ///
    /// @param pageSize the number of records per page
    /// @return this SpeedyQuery instance for method chaining
    /// @throws IllegalArgumentException if pageSize is less than 1
    public SpeedyQuery pageSize(@NotNull int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0.");
        }
        this.pageSize = pageSize;
        return this;
    }

    /// Builds and returns the final query as a JsonNode.
    ///
    /// This method assembles all the query components (where, orderBy, expand,
    /// select, pagination) into a complete JSON query object that can be
    /// sent to the Speedy API.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("active", eq(true)))
    ///     .select("id", "name", "email")
    ///     .orderByAsc("name")
    ///     .pageSize(20);
    ///
    /// JsonNode queryJson = query.build();
    ///```
    ///
    /// @return a JsonNode representing the complete query
    public JsonNode build() {
        if (!where.isEmpty()) {
            root.set("$where", where);
        }
        if (!orderBy.isEmpty()) {
            root.set("$orderBy", orderBy);
        }
        if (!expand.isEmpty()) {
            root.set("$expand", expand);
        }
        if (!select.isEmpty()) {
            ArrayNode arrayNode = CommonUtil.json().createArrayNode();
            select.forEach(arrayNode::add);
            root.set("$select", arrayNode);
        }
        ObjectNode pageNode = CommonUtil.json().createObjectNode();
        pageNode.put("$index", pageNo);
        pageNode.put("$size", pageSize);
        root.set("$page", pageNode);
        return root;
    }

    /// Pretty prints the query to the logger and returns this instance.
    ///
    /// This method is useful for debugging and logging the query structure
    /// before sending it to the API. It logs the formatted JSON query
    /// and returns the current instance for method chaining.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .where(condition("active", eq(true)))
    ///     .select("id", "name")
    ///     .prettyPrint()  // Logs the query structure
    ///     .build();
    ///```
    ///
    /// @return this SpeedyQuery instance for method chaining
    /// @throws JsonProcessingException if JSON serialization fails
    public SpeedyQuery prettyPrint() throws JsonProcessingException {
        ObjectMapper json = CommonUtil.json();// Using the existing ObjectMapper instance

        // Create a copy of the root node to avoid modifying the original
        ObjectNode rootCopy = root.deepCopy();
        rootCopy.set("$where", where);
        rootCopy.set("$orderBy", orderBy);
        rootCopy.set("$expand", expand);

        // Page information
        ObjectNode pageNode = json.createObjectNode();
        pageNode.put("$index", pageNo);
        pageNode.put("$size", pageSize);
        rootCopy.set("$page", pageNode);

        // Return the pretty-printed JSON string
        String output = json.writerWithDefaultPrettyPrinter().writeValueAsString(rootCopy);
        LOGGER.info("SpeedyQuery: {}", output);
        return this;
    }

    /// Gets the entity name from the query.
    ///
    /// @return the entity name set in the `$from` field
    public String getFrom() {
        return root.get("$from").asText();
    }

    /// Adds fields to select in the query results.
    ///
    /// This method adds fields to the `$select` array, which tells the API
    /// to only return the specified fields in the response.
    ///
    /// ## Example
    /// ```java
    /// import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
    ///
    /// SpeedyQuery query = from("users")
    ///     .select("id", "name", "email", "createdAt")
    ///     .where(condition("active", eq(true)))
    ///     .build();
    ///```
    ///
    /// @param select the field names to select
    /// @return this SpeedyQuery instance for method chaining
    /// @throws IllegalArgumentException if any select field is null
    public SpeedyQuery select(@NotNull String... select) {
        this.select.addAll(Arrays.asList(select));
        return this;
    }
}
