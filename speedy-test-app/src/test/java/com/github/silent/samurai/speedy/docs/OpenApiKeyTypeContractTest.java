package com.github.silent.samurai.speedy.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that a primary key keeps <i>one</i> type everywhere it surfaces in the generated
 * OpenAPI document — so the {@code openapi-generator-maven-plugin} emits the same Java type for
 * that key in every POJO it produces, and an id read from one call can be passed to the next
 * without a cast or a re-parse.
 * <p>
 * The declared type is taken from Speedy's own {@code MetaModel} (via
 * {@link OASGenerator#basicSchema}), and every entity is walked, so the test covers new entities
 * automatically. It asserts the key type is preserved:
 * <ul>
 *   <li>across the component schemas — {@code {Entity}}, {@code {Entity}Key},
 *       {@code Create{Entity}Request}, {@code Update{Entity}Request};</li>
 *   <li>across every operation body that carries the key — bulk create ({@code $create}), bulk
 *       update/replace ({@code $update} PATCH + PUT), bulk delete ({@code $delete}), GET-by-key
 *       and {@code $query} — including that each still refers to the expected schema through an
 *       array, since the generated client's {@code List<>} call sites depend on it;</li>
 *   <li>on the GET-by-key query parameters;</li>
 *   <li>through foreign keys: an association is a {@code $ref} to the target's schema, and that
 *       schema carries the <i>target's</i> key type. This matters because an association field's
 *       own {@code ValueType} is always {@code TEXT} (associations resolve to
 *       {@code ColumnType.VARCHAR}) regardless of what its target key really is — rendering an FK
 *       from its own type would silently turn a numeric key into a string.</li>
 * </ul>
 *
 * @see OpenApiSchemaContractTest for the complementary field-set/type contract of each schema
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenApiKeyTypeContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
    private static final String JSON_MEDIA_TYPE = "application/json;charset=UTF-8";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SpeedyFactory speedyFactory;

    private JsonNode schemas;
    private JsonNode paths;
    private Collection<EntityMetadata> entities;

    @BeforeEach
    void loadGeneratedSpec() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode document = MAPPER.readTree(json);
        this.schemas = document.path("components").path("schemas");
        this.paths = document.path("paths");
        this.entities = speedyFactory.getMetaModel().getAllEntityMetadata();
    }

    @Test
    void primaryKeyKeepsItsDeclaredTypeInEveryGeneratedSchema() {
        for (EntityMetadata entity : entities) {
            for (KeyFieldMetadata key : entity.getKeyFields()) {
                // {Entity}Key and Update{Entity}Request always carry the whole key: the first is
                // the key itself, the second needs it to identify the row being updated.
                assertKeyProperty(keySchema(entity), entity, key, true);
                assertKeyProperty(updateRequestSchema(entity), entity, key, true);
                // the response schema carries it whenever the key is serializable, and the create
                // request only when the client supplies it (a generated key is not insertable)
                assertKeyProperty(entity.getName(), entity, key, key.isSerializable());
                assertKeyProperty(createRequestSchema(entity), entity, key, key.isInsertable());
            }
        }
    }

    @Test
    void everyOperationBodyRefersToASchemaCarryingTheKeyWithItsDeclaredType() {
        for (EntityMetadata entity : entities) {
            for (BodyLocation body : keyCarryingBodies(entity)) {
                String schemaName = arrayItemRefAt(body);
                assertThat(schemaName)
                        .as("%s must be an array of '%s'", body, body.expectedSchema())
                        .isEqualTo(body.expectedSchema());
                assertKeyTypesOf(entity, schemaName, body.toString());
            }
        }
    }

    @Test
    void getByKeyQueryParametersUseTheDeclaredKeyType() {
        for (EntityMetadata entity : entities) {
            String path = basePath(entity);
            Map<String, JsonNode> parameters = parametersOf(path, "get");
            for (KeyFieldMetadata key : entity.getKeyFields()) {
                String property = key.getOutputPropertyName();
                JsonNode parameter = parameters.get(property);
                assertThat(parameter)
                        .as("GET %s must expose key '%s' as a parameter", path, property)
                        .isNotNull();
                assertThat(parameter.path("in").asText())
                        .as("key parameter '%s' of GET %s", property, path)
                        .isEqualTo("query");
                assertThat(OpenApiType.of(parameter.path("schema")))
                        .as("type of key parameter '%s' of GET %s (ValueType %s)",
                                property, path, key.getValueType())
                        .isEqualTo(declaredType(key));
            }
        }
    }

    @Test
    void foreignKeyPropertiesReferenceTheTargetsSchema() {
        int checked = 0;
        for (EntityMetadata entity : entities) {
            for (FieldMetadata association : associationsOf(entity)) {
                String target = association.getAssociationMetadata().getName();
                for (String schemaName : schemaNamesOf(entity)) {
                    JsonNode property = propertyNode(schemaName, association.getOutputPropertyName());
                    if (!property.isObject()) {
                        continue;
                    }
                    // request schemas reference the target's key; the response schema references
                    // the target entity itself
                    String expected = schemaName.equals(entity.getName()) ? target : keySchemaName(target);
                    assertThat(refOf(property, association, schemaName))
                            .as("association %s.%s must reference '%s'",
                                    schemaName, association.getOutputPropertyName(), expected)
                            .isEqualTo(expected);
                    checked++;
                }
            }
        }
        assertThat(checked)
                .as("test corpus must contain at least one association, otherwise foreign-key "
                        + "generation is never actually exercised")
                .isGreaterThan(0);
    }

    @Test
    void foreignKeyRefsCarryTheTargetPrimaryKeyWithTheTargetsDeclaredType() {
        int divergentlyTyped = 0;
        for (EntityMetadata entity : entities) {
            for (FieldMetadata association : associationsOf(entity)) {
                EntityMetadata target = association.getAssociationMetadata();
                for (String schemaName : schemaNamesOf(entity)) {
                    JsonNode property = propertyNode(schemaName, association.getOutputPropertyName());
                    if (!property.isObject()) {
                        continue;
                    }
                    String referenced = refOf(property, association, schemaName);
                    assertKeyTypesOf(target, referenced, String.format(
                            "foreign key %s.%s", schemaName, association.getOutputPropertyName()));
                }
                if (!association.isCollection()
                        && association.getValueType() != association.getAssociatedFieldMetadata().getValueType()) {
                    divergentlyTyped++;
                }
            }
        }
        assertThat(divergentlyTyped)
                .as("test corpus must contain at least one foreign key whose own ValueType differs "
                        + "from its target key's (e.g. an association onto a numeric key), otherwise "
                        + "rendering an FK from its own type would pass this test unnoticed")
                .isGreaterThan(0);
    }

    @Test
    void everyForeignKeyResolvesToAKeyFieldOfItsTargetEntity() {
        for (EntityMetadata entity : entities) {
            for (FieldMetadata association : associationsOf(entity)) {
                if (association.isCollection()) {
                    // the far side of a collection is the owning FK field, not a key
                    continue;
                }
                FieldMetadata associated = association.getAssociatedFieldMetadata();
                assertThat(associated)
                        .as("association %s.%s must resolve to a key field",
                                entity.getName(), association.getOutputPropertyName())
                        .isInstanceOf(KeyFieldMetadata.class);
                assertThat(association.getAssociationMetadata().getKeyFieldNames())
                        .as("association %s.%s must resolve to a key of its target %s",
                                entity.getName(), association.getOutputPropertyName(),
                                association.getAssociationMetadata().getName())
                        .contains(associated.getOutputPropertyName());
            }
        }
    }

    @Test
    void theTestCorpusCoversKeysOfMoreThanOneType() {
        Set<ValueType> keyTypes = entities.stream()
                .flatMap(entity -> entity.getKeyFields().stream())
                .map(FieldMetadata::getValueType)
                .collect(Collectors.toSet());

        assertThat(keyTypes)
                .as("key-type preservation is only meaningfully tested when the corpus keys on more "
                        + "than strings — see TypedCompositeKeyEntity (Long + LocalDate + String)")
                .contains(ValueType.TEXT, ValueType.INT, ValueType.DATE);

        Set<ValueType> foreignKeyTargetTypes = entities.stream()
                .flatMap(entity -> associationsOf(entity).stream())
                .filter(association -> !association.isCollection())
                .map(association -> association.getAssociatedFieldMetadata().getValueType())
                .collect(Collectors.toSet());

        assertThat(foreignKeyTargetTypes)
                .as("at least one association must target a non-string key — see "
                        + "ScalarFkEntity.numRef -> AutoGenIdentityEntity.id (Long)")
                .contains(ValueType.INT);
    }

    /// The declared OpenAPI type of a field, i.e. the type the generated POJO's property gets.
    private OpenApiType declaredType(FieldMetadata field) {
        return OpenApiType.of(OASGenerator.basicSchema(field.getValueType()));
    }

    private void assertKeyProperty(String schemaName, EntityMetadata entity, KeyFieldMetadata key,
                                   boolean mustBePresent) {
        JsonNode property = propertyNode(schemaName, key.getOutputPropertyName());
        if (!property.isObject()) {
            assertThat(mustBePresent)
                    .as("schema '%s' of entity %s must carry key property '%s'",
                            schemaName, entity.getName(), key.getOutputPropertyName())
                    .isFalse();
            return;
        }
        assertThat(OpenApiType.of(property))
                .as("type of %s.%s (ValueType %s)", schemaName, key.getOutputPropertyName(), key.getValueType())
                .isEqualTo(declaredType(key));
    }

    /// Asserts that {@code schemaName} carries {@code entity}'s key with the type Speedy declares
    /// for it. A {@code Create{Entity}Request} legitimately omits a generated key, so a missing
    /// key property is only a failure for the schemas that exist to identify a row.
    private void assertKeyTypesOf(EntityMetadata entity, String schemaName, String context) {
        JsonNode properties = schemas.path(schemaName).path("properties");
        assertThat(properties.isObject())
                .as("%s -> schema '%s' is not defined in the document", context, schemaName)
                .isTrue();

        boolean mustCarryEveryKey = schemaName.equals(keySchema(entity))
                || schemaName.equals(updateRequestSchema(entity));
        for (KeyFieldMetadata key : entity.getKeyFields()) {
            String property = key.getOutputPropertyName();
            JsonNode node = properties.path(property);
            if (!node.isObject()) {
                assertThat(mustCarryEveryKey)
                        .as("%s -> schema '%s' must carry key property '%s'", context, schemaName, property)
                        .isFalse();
                continue;
            }
            assertThat(OpenApiType.of(node))
                    .as("%s -> type of %s.%s (ValueType %s)", context, schemaName, property, key.getValueType())
                    .isEqualTo(declaredType(key));
        }
    }

    /// Every operation body that carries {@code entity}'s key, with the schema it must refer to.
    private List<BodyLocation> keyCarryingBodies(EntityMetadata entity) {
        String base = basePath(entity);
        List<BodyLocation> bodies = new ArrayList<>();
        bodies.add(new BodyLocation(base, "get", false, entity.getName()));
        bodies.add(new BodyLocation(endpointPath(entity, SpeedyEndpoint.QUERY), "post", false, entity.getName()));
        if (entity.isReadOnly()) {
            return bodies;
        }
        String create = endpointPath(entity, SpeedyEndpoint.CREATE);
        bodies.add(new BodyLocation(create, "post", true, createRequestSchema(entity)));
        bodies.add(new BodyLocation(create, "post", false, keySchema(entity)));

        // $update exposes PATCH (partial update) and PUT (full replace) over the same schemas
        String update = endpointPath(entity, SpeedyEndpoint.UPDATE);
        bodies.add(new BodyLocation(update, "patch", true, updateRequestSchema(entity)));
        bodies.add(new BodyLocation(update, "patch", false, entity.getName()));
        bodies.add(new BodyLocation(update, "put", true, updateRequestSchema(entity)));
        bodies.add(new BodyLocation(update, "put", false, entity.getName()));

        String delete = endpointPath(entity, SpeedyEndpoint.DELETE);
        bodies.add(new BodyLocation(delete, "delete", true, keySchema(entity)));
        bodies.add(new BodyLocation(delete, "delete", false, keySchema(entity)));
        return bodies;
    }

    /// Resolves the component schema an operation body is an array of. Bulk bodies and the
    /// response envelope's {@code payload} are always plain arrays (never {@code oneOf} — see
    /// issue #97), so the item {@code $ref} is where the key type lands in the generated client.
    private String arrayItemRefAt(BodyLocation body) {
        JsonNode operation = paths.path(body.path()).path(body.method());
        assertThat(operation.isObject())
                .as("expected operation %s in the generated spec", body)
                .isTrue();

        JsonNode array = body.request()
                ? operation.path("requestBody").path("content").path(JSON_MEDIA_TYPE).path("schema")
                : operation.path("responses").path("200").path("content").path(JSON_MEDIA_TYPE)
                .path("schema").path("properties").path("payload");

        assertThat(array.path("type").asText())
                .as("%s must be an array", body)
                .isEqualTo("array");
        return refTargetOf(array.path("items"), body.toString());
    }

    /// The schema an association property points at — directly, or through the array items of a
    /// collection association.
    private String refOf(JsonNode property, FieldMetadata association, String schemaName) {
        String context = String.format("association %s.%s", schemaName, association.getOutputPropertyName());
        if (association.isCollection()) {
            assertThat(property.path("type").asText())
                    .as("collection %s must be an array", context)
                    .isEqualTo("array");
            return refTargetOf(property.path("items"), context);
        }
        return refTargetOf(property, context);
    }

    private String refTargetOf(JsonNode node, String context) {
        JsonNode ref = node.path("$ref");
        assertThat(ref.isTextual())
                .as("%s must reference a component schema, was %s", context, node)
                .isTrue();
        assertThat(ref.asText())
                .as("%s must reference a local schema", context)
                .startsWith(SCHEMA_REF_PREFIX);
        return ref.asText().substring(SCHEMA_REF_PREFIX.length());
    }

    private Map<String, JsonNode> parametersOf(String path, String method) {
        Map<String, JsonNode> byName = new LinkedHashMap<>();
        paths.path(path).path(method).path("parameters")
                .forEach(parameter -> byName.put(parameter.path("name").asText(), parameter));
        return byName;
    }

    private Set<FieldMetadata> associationsOf(EntityMetadata entity) {
        return entity.getAllFields().stream()
                .filter(FieldMetadata::isAssociation)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private JsonNode propertyNode(String schemaName, String property) {
        return schemas.path(schemaName).path("properties").path(property);
    }

    private List<String> schemaNamesOf(EntityMetadata entity) {
        return List.of(entity.getName(), keySchema(entity),
                createRequestSchema(entity), updateRequestSchema(entity));
    }

    private String basePath(EntityMetadata entity) {
        return String.format("%s/%s", SpeedyConstants.URI, entity.getName());
    }

    private String endpointPath(EntityMetadata entity, SpeedyEndpoint endpoint) {
        return String.format("%s/%s", basePath(entity), endpoint.suffix());
    }

    private String keySchema(EntityMetadata entity) {
        return keySchemaName(entity.getName());
    }

    private String keySchemaName(String entityName) {
        return entityName + "Key";
    }

    private String createRequestSchema(EntityMetadata entity) {
        return "Create" + entity.getName() + "Request";
    }

    private String updateRequestSchema(EntityMetadata entity) {
        return "Update" + entity.getName() + "Request";
    }

    /// A request or 200-response body of one operation, and the component schema its array items
    /// must refer to.
    private record BodyLocation(String path, String method, boolean request, String expectedSchema) {
        @Override
        public String toString() {
            return String.format("%s %s (%s body)", method.toUpperCase(), path, request ? "request" : "response");
        }
    }

    /// The {@code type}/{@code format} pair of a schema node — what decides the Java type the
    /// generator emits for a property.
    private record OpenApiType(String type, String format) {
        static OpenApiType of(Schema<?> schema) {
            return new OpenApiType(schema.getType(), schema.getFormat());
        }

        static OpenApiType of(JsonNode node) {
            return new OpenApiType(textOrNull(node, "type"), textOrNull(node, "format"));
        }

        private static String textOrNull(JsonNode node, String field) {
            JsonNode value = node.path(field);
            return value.isTextual() ? value.asText() : null;
        }

        @Override
        public String toString() {
            return format == null ? String.valueOf(type) : type + "/" + format;
        }
    }
}
