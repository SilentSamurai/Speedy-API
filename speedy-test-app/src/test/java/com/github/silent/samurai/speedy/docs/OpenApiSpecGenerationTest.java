package com.github.silent.samurai.speedy.docs;

import com.github.silent.samurai.speedy.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the OpenAPI spec generated at runtime by {@link SpeedyOpenApiCustomizer} /
 * {@code OASGenerator} and served at {@code /v3/api-docs}.
 * <p>
 * Rather than diffing against the committed {@code api-docs.json} (which is a manually
 * regenerated snapshot and drifts as entities are added), these tests assert the structural
 * contract of the generated document with JsonPath: that each entity yields the expected
 * schemas, CRUD paths, key handling, response envelope and type mappings.
 * <p>
 * Anchored on stable test-app entities: {@code Category} (writable, single generated key),
 * {@code Company} (has an {@code int64} field) and {@code VirtualEntity} (read-only).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenApiSpecGenerationTest {

    private static final String JSON = "application/json;charset=UTF-8";

    @Autowired
    private MockMvc mvc;

    private ResultActions whenGetApiDocs() throws Exception {
        return mvc.perform(get("/v3/api-docs").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void generatesTheFourSchemasPerEntity() throws Exception {
        whenGetApiDocs()
                .andExpect(jsonPath("$.components.schemas.Category").exists())
                .andExpect(jsonPath("$.components.schemas.CategoryKey").exists())
                .andExpect(jsonPath("$.components.schemas.CreateCategoryRequest").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateCategoryRequest").exists());
    }

    @Test
    void keySchemaContainsOnlyKeyFields() throws Exception {
        whenGetApiDocs()
                .andExpect(jsonPath("$.components.schemas.CategoryKey.properties.id").exists())
                .andExpect(jsonPath("$.components.schemas.CategoryKey.properties.name").doesNotExist());
    }

    @Test
    void createRequestExcludesGeneratedKey_updateRequestKeepsItForIdentification() throws Exception {
        whenGetApiDocs()
                // generated PK is not insertable -> absent from the create request
                .andExpect(jsonPath("$.components.schemas.CreateCategoryRequest.properties.name").exists())
                .andExpect(jsonPath("$.components.schemas.CreateCategoryRequest.properties.id").doesNotExist())
                // key is not updatable but must stay in the update request so clients can identify the row
                .andExpect(jsonPath("$.components.schemas.UpdateCategoryRequest.properties.name").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateCategoryRequest.properties.id").exists());
    }

    @Test
    void writableEntityExposesAllCrudPaths() throws Exception {
        whenGetApiDocs()
                .andExpect(jsonPath("$.paths['/speedy/v1/Category'].get").exists())
                .andExpect(jsonPath("$.paths['/speedy/v1/Category/$query'].post").exists())
                .andExpect(jsonPath("$.paths['/speedy/v1/Category/$create'].post").exists())
                .andExpect(jsonPath("$.paths['/speedy/v1/Category/$update'].put").exists())
                .andExpect(jsonPath("$.paths['/speedy/v1/Category/$delete'].delete").exists());
    }

    @Test
    void readOnlyEntityOmitsWritePaths() throws Exception {
        whenGetApiDocs()
                // read endpoints are still generated
                .andExpect(jsonPath("$.paths['/speedy/v1/VirtualEntity'].get").exists())
                .andExpect(jsonPath("$.paths['/speedy/v1/VirtualEntity/$query'].post").exists())
                // write endpoints are suppressed for read-only entities
                .andExpect(jsonPath("$.paths['/speedy/v1/VirtualEntity/$create']").doesNotExist())
                .andExpect(jsonPath("$.paths['/speedy/v1/VirtualEntity/$update']").doesNotExist())
                .andExpect(jsonPath("$.paths['/speedy/v1/VirtualEntity/$delete']").doesNotExist());
    }

    @Test
    void getOperationExposesPrimaryKeyAsQueryParameter() throws Exception {
        whenGetApiDocs()
                .andExpect(jsonPath("$.paths['/speedy/v1/Category'].get.parameters[0].name").value("id"))
                .andExpect(jsonPath("$.paths['/speedy/v1/Category'].get.parameters[0].in").value("query"));
    }

    @Test
    void createRequestBodyIsAnArrayOfCreateRequestSchema() throws Exception {
        whenGetApiDocs()
                .andExpect(jsonPath("$.paths['/speedy/v1/Category/$create'].post.requestBody.content['" + JSON + "'].schema.type")
                        .value("array"))
                .andExpect(jsonPath("$.paths['/speedy/v1/Category/$create'].post.requestBody.content['" + JSON + "'].schema.items['$ref']")
                        .value("#/components/schemas/CreateCategoryRequest"));
    }

    @Test
    void responsesAreWrappedInThePagedPayloadEnvelope() throws Exception {
        String schema = "$.paths['/speedy/v1/Category'].get.responses['200'].content['" + JSON + "'].schema";
        whenGetApiDocs()
                .andExpect(jsonPath(schema + ".properties.payload.type").value("array"))
                .andExpect(jsonPath(schema + ".properties.payload.items['$ref']").value("#/components/schemas/Category"))
                .andExpect(jsonPath(schema + ".properties.pageSize.type").value("integer"))
                .andExpect(jsonPath(schema + ".properties.pageIndex.type").value("integer"));
    }

    @Test
    void mapsIntegerFieldToInt64() throws Exception {
        whenGetApiDocs()
                .andExpect(jsonPath("$.components.schemas.Company.properties.invoiceNo.type").value("integer"))
                .andExpect(jsonPath("$.components.schemas.Company.properties.invoiceNo.format").value("int64"));
    }
}
