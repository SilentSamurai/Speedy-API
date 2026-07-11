package com.github.silent.samurai.speedy.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.events.EntityEvents;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class PerEntityUnexpectedFailureTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CategoryRepository categoryRepository;

    private SpeedyTest client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mvc);
        EntityEvents.throwOnNextCategoryDelete.set(false);
    }

    @AfterEach
    void cleanUp() {
        EntityEvents.throwOnNextCategoryDelete.set(false);
    }

    @Test
    void createRecordsGenericValidatorFailureAndCommitsTheOtherItem() {
        String successfulName = uniqueName("validator-success");

        client.createMany("Category", List.of(category(successfulName), category("validator-runtime-trigger")))
                .expectStatus(207)
                .expectJsonPath("$.succeeded[*]", hasSize(1))
                .expectJsonPath("$.failed[*]", hasSize(1))
                .expectJsonPath("$.failed[0].index", equalTo(1))
                .expectJsonPath("$.failed[0].status", equalTo(500));

        assertTrue(categoryRepository.findByName(successfulName).isPresent());
        assertFalse(categoryRepository.findByName("validator-runtime-trigger").isPresent());
    }

    @Test
    void createPreservesCustomValidatorHttpRuntimeStatus() {
        String successfulName = uniqueName("validator-http-success");

        client.createMany("Category", List.of(category(successfulName), category("validator-http-runtime-trigger")))
                .expectStatus(207)
                .expectJsonPath("$.succeeded[*]", hasSize(1))
                .expectJsonPath("$.failed[0].index", equalTo(1))
                .expectJsonPath("$.failed[0].status", equalTo(422));

        assertTrue(categoryRepository.findByName(successfulName).isPresent());
        assertFalse(categoryRepository.findByName("validator-http-runtime-trigger").isPresent());
    }

    @Test
    void updateRecordsUnexpectedEventFailureAndCommitsTheOtherItem() {
        String successfulName = uniqueName("update-success");
        String failingName = uniqueName("update-failure");
        String successfulId = createCategory(successfulName);
        String failingId = createCategory(failingName);
        String updatedName = uniqueName("updated");

        client.updateMany("Category")
                .item(item -> item.key("id", successfulId).field("name", updatedName))
                .item(item -> item.key("id", failingId).field("name", "generic-update-error-trigger"))
                .execute()
                .expectStatus(207)
                .expectJsonPath("$.succeeded[*]", hasSize(1))
                .expectJsonPath("$.failed[*]", hasSize(1))
                .expectJsonPath("$.failed[0].index", equalTo(1))
                .expectJsonPath("$.failed[0].status", equalTo(500));

        assertTrue(categoryRepository.findByName(updatedName).isPresent());
        assertTrue(categoryRepository.findByName(failingName).isPresent());
    }

    @Test
    void deleteRecordsUnexpectedEventFailureAndCommitsTheOtherItem() {
        String firstName = uniqueName("delete-first");
        String secondName = uniqueName("delete-second");
        String firstId = createCategory(firstName);
        String secondId = createCategory(secondName);
        EntityEvents.throwOnNextCategoryDelete.set(true);

        client.deleteMany("Category")
                .items(List.of(key(firstId), key(secondId)))
                .execute()
                .expectStatus(207)
                .expectJsonPath("$.succeeded[*]", hasSize(1))
                .expectJsonPath("$.failed[*]", hasSize(1))
                .expectJsonPath("$.failed[0].index", equalTo(0))
                .expectJsonPath("$.failed[0].status", equalTo(500));

        assertTrue(categoryRepository.findByName(firstName).isPresent());
        assertFalse(categoryRepository.findByName(secondName).isPresent());
    }

    private String createCategory(String name) {
        SpeedyTestResult result = client.create("Category")
                .field("name", name)
                .execute()
                .expectOk();
        return result.jsonPath("$.payload[0].id");
    }

    private ObjectNode category(String name) {
        ObjectNode category = mapper.createObjectNode();
        category.put("name", name);
        return category;
    }

    private ObjectNode key(String id) {
        ObjectNode key = mapper.createObjectNode();
        key.put("id", id);
        return key;
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
