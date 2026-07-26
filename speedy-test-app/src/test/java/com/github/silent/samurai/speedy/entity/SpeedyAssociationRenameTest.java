package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Covers @SpeedyAssociation#name() on RenamedScalarFkEntity.catId -> "cat". A scalar FK is
// conventionally named after its column ("catId"), but the annotation turns it into an object
// reference, so without the rename it would be exposed -- and navigated -- as "catId.id". The
// rename has to hold across every surface the output property name feeds: the metamodel, the
// generated OpenAPI schemas, write payloads, read payloads, and $filter navigation.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyAssociationRenameTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SpeedyFactory speedyFactory;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private String createCategory() {
        return speedyClient.create("Category")
                .field("name", "cat-" + UUID.randomUUID())
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");
    }

    private JsonNode schemas() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return MAPPER.readTree(json).path("components").path("schemas");
    }

    @Test
    void metamodel_exposesRenamedProperty_notTheJavaFieldName() throws NotFoundException {
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata field = metaModel.findFieldMetadata("RenamedScalarFkEntity", "cat");

        assertThat(field.isAssociation()).isTrue();
        assertThat(field.getAssociationMetadata().getName()).isEqualTo("Category");
        assertThat(field.getAssociatedFieldMetadata().getOutputPropertyName()).isEqualTo("id");
        // The DB column is untouched -- only the exposed property name changes.
        assertThat(field.getDbColumnName()).isEqualTo("cat_id");
    }

    @Test
    void metamodel_javaFieldName_isNoLongerAddressable() {
        MetaModel metaModel = speedyFactory.getMetaModel();

        assertThatThrownBy(() -> metaModel.findFieldMetadata("RenamedScalarFkEntity", "catId"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void openApi_schemasExposeRenamedPropertyAsRefToTarget() throws Exception {
        JsonNode schemas = schemas();

        // Every generated schema the field qualifies for -- it's serializable, insertable and
        // updatable, so all but the key-only schema carry it. The response schema refs the full
        // target entity while the request schemas ref its key-only form (OASGenerator.ENTITY_NAME
        // vs ENTITY_KEY in SpeedyOpenApiCustomizer#createSchemas).
        Map<String, String> expectedRefBySchema = Map.of(
                "RenamedScalarFkEntity", "#/components/schemas/Category",
                "CreateRenamedScalarFkEntityRequest", "#/components/schemas/CategoryKey",
                "UpdateRenamedScalarFkEntityRequest", "#/components/schemas/CategoryKey");

        for (Map.Entry<String, String> expected : expectedRefBySchema.entrySet()) {
            String schemaName = expected.getKey();
            JsonNode properties = schemas.path(schemaName).path("properties");
            assertThat(properties.has("cat"))
                    .as("%s should expose the renamed property 'cat'", schemaName)
                    .isTrue();
            assertThat(properties.has("catId"))
                    .as("%s should not expose the Java field name 'catId'", schemaName)
                    .isFalse();

            // An association renders as a $ref, never as the scalar column's own type -- the whole
            // reason the "catId" name read wrong in the first place.
            assertThat(properties.path("cat").path("$ref").asText())
                    .as("%s.properties.cat should $ref the target entity", schemaName)
                    .isEqualTo(expected.getValue());
        }
    }

    @Test
    void create_andRead_useRenamedProperty() {
        String categoryId = createCategory();

        String sourceId = speedyClient.create("RenamedScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("cat.id", categoryId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.get("RenamedScalarFkEntity")
                .key("id", sourceId)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].cat.id", categoryId)
                .expectJsonPathDoesNotExist("$.payload[0].catId");
    }

    @Test
    void query_filterNavigatesThroughRenamedProperty() {
        String categoryId = createCategory();

        speedyClient.create("RenamedScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("cat.id", categoryId)
                .execute()
                .expectOk();

        speedyClient.query("RenamedScalarFkEntity")
                .where(condition("cat.id", eq(categoryId)))
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id");
    }

    @Test
    void metamodel_renameDoesNotLeakIntoOtherEntities() throws NotFoundException {
        // ScalarFkEntity leaves its @SpeedyAssociation fields unrenamed -- name() is opt-in, and a
        // blank default must keep the Java field name.
        MetaModel metaModel = speedyFactory.getMetaModel();
        List<String> properties = metaModel.findEntityMetadata("ScalarFkEntity").getAllFields()
                .stream()
                .map(FieldMetadata::getOutputPropertyName)
                .collect(Collectors.toList());

        assertThat(properties).contains("ref", "refByName", "catRef", "numRef");
    }
}
