package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// # JsonPropertyTest
///
/// Integration tests validating that {@code @JsonProperty} on entity
/// fields flows through the entire request pipeline: metadata generation,
/// serialization, deserialization, URL query parsing, and expansion.
///
/// ## Covered Paths
/// - {@link com.github.silent.samurai.speedy.jpa.impl.processors.JpaMetaModelProcessorV2#findOutputName}
///   returns the {@code @JsonProperty} value instead of the Java field name
/// - Metadata {@code outputProperty} reflects the overridden name
/// - JSON response keys use the overridden name, not the Java field name
/// - Incoming JSON body (create/update) accepts the overridden name
/// - URL query params accept the overridden name
/// - {@code $select} and {@code $expand} accept the overridden name
/// - Association fields respect the override
///
/// ## Entity Under Test
/// {@link JsonPropertyEntity} with three {@code @JsonProperty}-annotated
/// fields: {@code custom_name}, {@code custom_cost},
/// and {@code custom_category} (a {@code @ManyToOne} to
/// {@link Category}).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class JsonPropertyTest {

    private final ObjectMapper mapper = CommonUtil.json();

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    /// Verifies the `/$metadata` endpoint exposes `@JsonProperty` values
    /// as `outputProperty`, not the raw Java field names. Checks that
    /// `custom_name`, `custom_cost`, and `custom_category` are present
    /// while `name`, `cost`, and `category` are absent.
    @Test
    void metadataEndpoint_outputPropertyUsesJsonPropertyValue() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstants.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='JsonPropertyEntity')]").exists())
                .andExpect(jsonPath("$[?(@.name=='JsonPropertyEntity')].fields[?(@.outputProperty=='custom_name')]").exists())
                .andExpect(jsonPath("$[?(@.name=='JsonPropertyEntity')].fields[?(@.outputProperty=='custom_cost')]").exists())
                .andExpect(jsonPath("$[?(@.name=='JsonPropertyEntity')].fields[?(@.outputProperty=='custom_category')]").exists())
                .andExpect(jsonPath("$[?(@.name=='JsonPropertyEntity')].fields[?(@.outputProperty=='name')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.name=='JsonPropertyEntity')].fields[?(@.outputProperty=='cost')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.name=='JsonPropertyEntity')].fields[?(@.outputProperty=='category')]").doesNotExist())
                .andReturn();
    }

    /// Creates an entity using the overridden field names in the JSON body,
    /// then retrieves via GET and asserts the response payload contains
    /// keys matching {@code custom_name} and {@code custom_cost}, while
    /// the raw Java field names {@code name} and {@code cost} are absent.
    @Test
    void createAndGet_usesOverriddenNamesInResponse() {
        String createdId = speedyClient.create("JsonPropertyEntity")
                .field("custom_name", "widget")
                .field("custom_cost", 150)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.get("JsonPropertyEntity")
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id")
                .expectJsonPathExists("$.payload[0].custom_name")
                .expectJsonPathExists("$.payload[0].custom_cost")
                .expectJsonPathDoesNotExist("$.payload[0].name")
                .expectJsonPathDoesNotExist("$.payload[0].cost");
    }

    /// Ensures URL query parameters (GET `?custom_name=...`) accept the
    /// `@JsonProperty` name. The {@link SpeedyTest#get(String)} builder
    /// appends {@code .key()} values as query-string params.
    @Test
    void urlGetQueryParam_usesOverriddenName() {
        speedyClient.create("JsonPropertyEntity")
                .field("custom_name", "unique-widget")
                .field("custom_cost", 200)
                .execute()
                .expectOk();

        speedyClient.get("JsonPropertyEntity")
                .key("custom_name", "unique-widget")
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].custom_name");
    }

    /// Posts a `$query` with `$select: ["custom_name", "custom_cost"]` and
    /// asserts the response payload returns the overridden field names
    /// while the Java names are not present.
    @Test
    void querySelect_usesOverriddenName() {
        speedyClient.create("JsonPropertyEntity")
                .field("custom_name", "gadget")
                .field("custom_cost", 300)
                .execute()
                .expectOk();

        speedyClient.query("JsonPropertyEntity")
                .select("custom_name", "custom_cost")
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].custom_name")
                .expectJsonPathExists("$.payload[0].custom_cost")
                .expectJsonPathDoesNotExist("$.payload[0].name")
                .expectJsonPathDoesNotExist("$.payload[0].cost");
    }

    /// Posts a `$query` with a `$where` condition referencing
    /// {@code custom_name} and verifies the filter correctly matches
    /// the created entity.
    @Test
    void queryWhere_usesOverriddenName() {
        speedyClient.create("JsonPropertyEntity")
                .field("custom_name", "where-test")
                .field("custom_cost", 400)
                .execute()
                .expectOk();

        ObjectMapper m = CommonUtil.json();
        com.fasterxml.jackson.databind.node.ObjectNode condition = m.createObjectNode();
        condition.put("custom_name", "where-test");

        speedyClient.query("JsonPropertyEntity")
                .where(condition)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].custom_name");
    }

    /// Creates an entity, updates the {@code custom_name} field via
    /// `$update`, then fetches by ID to confirm the new value persisted
    /// under the overridden key.
    @Test
    void update_usesOverriddenName() {
        String id = speedyClient.create("JsonPropertyEntity")
                .field("custom_name", "old-name")
                .field("custom_cost", 500)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.update("JsonPropertyEntity")
                .key("id", id)
                .field("custom_name", "new-name")
                .execute()
                .expectOk();

        speedyClient.get("JsonPropertyEntity")
                .key("id", id)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].custom_name", "new-name");
    }

    /// Creates an entity with a `@ManyToOne` association set via
    /// {@code custom_category}, then fetches with `$expand` and asserts
    /// the expanded association appears under the overridden name
    /// {@code custom_category} rather than the Java field name
    /// {@code category}.
    @Test
    void associationField_usesOverriddenName() {
        String catId = speedyClient.create("Category")
                .field("name", "test-cat-" + UUID.randomUUID())
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        String entityId = speedyClient.create("JsonPropertyEntity")
                .field("custom_name", "associated-item")
                .field("custom_cost", 600)
                .field("custom_category", mapper.createObjectNode().put("id", catId))
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.get("JsonPropertyEntity")
                .key("id", entityId)
                .expand("custom_category")
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].custom_category")
                .expectJsonPathDoesNotExist("$.payload[0].category");
    }
}
