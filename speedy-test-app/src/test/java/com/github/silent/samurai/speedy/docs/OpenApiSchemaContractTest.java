package com.github.silent.samurai.speedy.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the schemas in the generated OpenAPI document (i.e., the POJOs the
 * {@code openapi-generator-maven-plugin} will produce from it) are structurally correct and
 * supported by Speedy.
 * <p>
 * Expectations are driven from Speedy's own {@code MetaModel} — the source of truth for the
 * entity structure — so the test covers <i>every</i> entity automatically (not hand-picked
 * anchors) and stays correct as entities are added/removed. For each entity it asserts:
 * <ul>
 *   <li>the four generated schemas ({@code Entity}, {@code EntityKey}, {@code CreateEntityRequest},
 *       {@code UpdateEntityRequest}) expose exactly the fields Speedy says belong in each;</li>
 *   <li>every property is a Speedy-supported type — a primitive matching
 *       {@link OASGenerator#basicSchema} for the field's {@code ValueType}, or an association
 *       rendered as a {@code $ref} / array-of-{@code $ref};</li>
 *   <li>every {@code $ref} in the document resolves to a defined schema (no POJO would reference
 *       a missing model).</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenApiSchemaContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SpeedyFactory speedyFactory;

    private JsonNode document;
    private JsonNode schemas;
    private Collection<EntityMetadata> entities;

    @BeforeEach
    void loadGeneratedSpec() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        this.document = MAPPER.readTree(json);
        this.schemas = document.path("components").path("schemas");
        this.entities = speedyFactory.getMetaModel().getAllEntityMetadata();
    }

    @Test
    void everyEntitySchemaExposesExactlyTheFieldsSpeedyDefines() {
        for (EntityMetadata entity : entities) {
            String name = entity.getName();
            assertSchemaProperties(name, entity, FieldMetadata::isSerializable);
            assertSchemaProperties(name + "Key", entity, KeyFieldMetadata.class::isInstance);
            assertSchemaProperties("Create" + name + "Request", entity, FieldMetadata::isInsertable);
            // key fields are not updatable but must remain so clients can identify the row
            assertSchemaProperties("Update" + name + "Request", entity,
                    field -> field.isUpdatable() || field instanceof KeyFieldMetadata);
        }
    }

    @Test
    void everyPropertyUsesATypeSupportedBySpeedy() {
        for (EntityMetadata entity : entities) {
            Map<String, FieldMetadata> fieldsByProperty = entity.getAllFields().stream()
                    .collect(Collectors.toMap(FieldMetadata::getOutputPropertyName, f -> f, (a, b) -> a));

            for (String schemaName : schemaNamesOf(entity)) {
                JsonNode properties = schemas.path(schemaName).path("properties");
                Iterator<Map.Entry<String, JsonNode>> it = properties.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> property = it.next();
                    FieldMetadata field = fieldsByProperty.get(property.getKey());
                    assertThat(field)
                            .as("schema %s exposes property '%s' with no matching Speedy field",
                                    schemaName, property.getKey())
                            .isNotNull();
                    assertSupportedType(schemaName, property.getKey(), field, property.getValue());
                }
            }
        }
    }

    @Test
    void everyRefResolvesToADefinedSchema() {
        Set<String> definedSchemas = new HashSet<>();
        schemas.fieldNames().forEachRemaining(definedSchemas::add);

        List<String> refs = new ArrayList<>();
        collectRefs(document, refs);

        assertThat(refs).as("expected the generated spec to contain $ref links").isNotEmpty();
        for (String ref : refs) {
            assertThat(ref).as("only local schema refs are supported").startsWith(SCHEMA_REF_PREFIX);
            String target = ref.substring(SCHEMA_REF_PREFIX.length());
            assertThat(definedSchemas).as("dangling $ref '%s' — no such schema is generated", ref)
                    .contains(target);
        }
    }

    @Test
    void nonInsertableNonKeyFieldsAreExcludedFromCreateRequest() {
        int checked = 0;
        for (EntityMetadata entity : entities) {
            Set<String> createProps = propertyNames("Create" + entity.getName() + "Request");
            for (FieldMetadata field : entity.getAllFields()) {
                // database-generated / non-insertable columns (e.g. @Generated, insertable=false)
                if (field instanceof KeyFieldMetadata || field.isInsertable()) {
                    continue;
                }
                assertThat(createProps)
                        .as("non-insertable field '%s' must not appear in Create%sRequest",
                                field.getOutputPropertyName(), entity.getName())
                        .doesNotContain(field.getOutputPropertyName());
                checked++;
            }
        }
        assertThat(checked)
                .as("test corpus must contain at least one non-insertable non-key field, "
                        + "otherwise this exclusion is never actually exercised")
                .isGreaterThan(0);
    }

    @Test
    void nonUpdatableNonKeyFieldsAreExcludedFromUpdateRequest() {
        int checked = 0;
        for (EntityMetadata entity : entities) {
            Set<String> updateProps = propertyNames("Update" + entity.getName() + "Request");
            for (FieldMetadata field : entity.getAllFields()) {
                // key fields stay (needed to identify the row); other non-updatable fields must not
                if (field instanceof KeyFieldMetadata || field.isUpdatable()) {
                    continue;
                }
                assertThat(updateProps)
                        .as("non-updatable field '%s' must not appear in Update%sRequest",
                                field.getOutputPropertyName(), entity.getName())
                        .doesNotContain(field.getOutputPropertyName());
                checked++;
            }
        }
        assertThat(checked)
                .as("test corpus must contain at least one non-updatable non-key field, "
                        + "otherwise this exclusion is never actually exercised")
                .isGreaterThan(0);
    }

    @Test
    void requiredFieldsAreMarkedRequiredInRequestSchemasOnly() {
        int checked = 0;
        for (EntityMetadata entity : entities) {
            // request schemas list required fields; the response schema ({Entity}) never does
            assertThat(requiredNames(entity.getName()))
                    .as("response schema '%s' must not declare required fields", entity.getName())
                    .isEmpty();

            for (String schemaName : List.of(entity.getName() + "Key",
                    "Create" + entity.getName() + "Request",
                    "Update" + entity.getName() + "Request")) {
                Set<String> present = propertyNames(schemaName);
                Set<String> required = requiredNames(schemaName);
                // a field can only be required if it is actually present in that schema
                assertThat(present).as("required[] of '%s' must be a subset of its properties", schemaName)
                        .containsAll(required);

                for (FieldMetadata field : entity.getAllFields()) {
                    String property = field.getOutputPropertyName();
                    if (!present.contains(property)) {
                        continue;
                    }
                    if (field.isRequired()) {
                        assertThat(required)
                                .as("required field '%s' must be listed required in %s", property, schemaName)
                                .contains(property);
                        checked++;
                    } else {
                        assertThat(required)
                                .as("non-required field '%s' must not be listed required in %s", property, schemaName)
                                .doesNotContain(property);
                    }
                }
            }
        }
        assertThat(checked)
                .as("test corpus must contain at least one required field in a request schema, "
                        + "otherwise required[] generation is never actually exercised")
                .isGreaterThan(0);
    }

    private Set<String> propertyNames(String schemaName) {
        Set<String> names = new HashSet<>();
        schemas.path(schemaName).path("properties").fieldNames().forEachRemaining(names::add);
        return names;
    }

    private Set<String> requiredNames(String schemaName) {
        Set<String> names = new HashSet<>();
        JsonNode required = schemas.path(schemaName).path("required");
        if (required.isArray()) {
            required.forEach(node -> names.add(node.asText()));
        }
        return names;
    }

    private void assertSchemaProperties(String schemaName, EntityMetadata entity,
                                        Predicate<FieldMetadata> belongsInSchema) {
        JsonNode schema = schemas.path(schemaName);
        assertThat(schema.isObject())
                .as("entity %s should generate schema '%s'", entity.getName(), schemaName)
                .isTrue();

        Set<String> expected = entity.getAllFields().stream()
                .filter(belongsInSchema)
                .map(FieldMetadata::getOutputPropertyName)
                .collect(Collectors.toSet());

        Set<String> actual = new HashSet<>();
        schema.path("properties").fieldNames().forEachRemaining(actual::add);

        assertThat(actual)
                .as("properties of generated schema '%s'", schemaName)
                .isEqualTo(expected);
    }

    private void assertSupportedType(String schemaName, String property, FieldMetadata field, JsonNode node) {
        if (field.isAssociation()) {
            boolean directRef = node.has("$ref");
            boolean arrayOfRef = "array".equals(node.path("type").asText())
                    && node.path("items").has("$ref");
            assertThat(directRef || arrayOfRef)
                    .as("association %s.%s must be a $ref or an array of $ref, was %s",
                            schemaName, property, node)
                    .isTrue();
            return;
        }
        Schema<?> expected = OASGenerator.basicSchema(field.getValueType());
        assertThat(node.path("type").asText())
                .as("type of %s.%s (ValueType %s)", schemaName, property, field.getValueType())
                .isEqualTo(expected.getType());
        if (expected.getFormat() != null) {
            assertThat(node.path("format").asText())
                    .as("format of %s.%s (ValueType %s)", schemaName, property, field.getValueType())
                    .isEqualTo(expected.getFormat());
        }
    }

    private List<String> schemaNamesOf(EntityMetadata entity) {
        String name = entity.getName();
        return List.of(name, name + "Key", "Create" + name + "Request", "Update" + name + "Request");
    }

    private void collectRefs(JsonNode node, List<String> out) {
        if (node.isObject()) {
            JsonNode ref = node.get("$ref");
            if (ref != null && ref.isTextual()) {
                out.add(ref.asText());
            }
            node.fields().forEachRemaining(e -> collectRefs(e.getValue(), out));
        } else if (node.isArray()) {
            node.forEach(child -> collectRefs(child, out));
        }
    }
}
