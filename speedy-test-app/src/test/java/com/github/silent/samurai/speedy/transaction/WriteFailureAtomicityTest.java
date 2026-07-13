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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Every bulk write is a single all-or-nothing transaction: if one item fails validation or a
/// lifecycle event, the whole request is rejected and nothing is committed. These tests exercise
/// that rollback across create/update/delete and confirm the failing item's HTTP status is preserved.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class WriteFailureAtomicityTest {

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
    void createValidatorFailure_failsWholeRequest() {
        String successfulName = uniqueName("validator-success");

        // A generic validator exception (IllegalStateException) surfaces as a 409 for the whole request.
        client.createMany("Category", List.of(category(successfulName), category("validator-runtime-trigger")))
                .expectStatus(409);

        // All-or-nothing: the otherwise-valid item is not committed when another item fails.
        assertFalse(categoryRepository.findByName(successfulName).isPresent());
        assertFalse(categoryRepository.findByName("validator-runtime-trigger").isPresent());
    }

    @Test
    void createCustomValidatorHttpStatus_failsWholeRequestWithThatStatus() {
        String successfulName = uniqueName("validator-http-success");

        // The custom validator's own HTTP status (422) is preserved for the whole request.
        client.createMany("Category", List.of(category(successfulName), category("validator-http-runtime-trigger")))
                .expectStatus(422);

        assertFalse(categoryRepository.findByName(successfulName).isPresent());
        assertFalse(categoryRepository.findByName("validator-http-runtime-trigger").isPresent());
    }

    @Test
    void updateEventFailure_failsWholeRequest() {
        String successfulName = uniqueName("update-success");
        String failingName = uniqueName("update-failure");
        String successfulId = createCategory(successfulName);
        String failingId = createCategory(failingName);
        String updatedName = uniqueName("updated");

        client.updateMany("Category")
                .item(item -> item.key("id", successfulId).field("name", updatedName))
                .item(item -> item.key("id", failingId).field("name", "generic-update-error-trigger"))
                .execute()
                .expectStatus(500);

        // All-or-nothing: neither row is updated when one item's PRE_UPDATE event fails.
        assertFalse(categoryRepository.findByName(updatedName).isPresent());
        assertTrue(categoryRepository.findByName(successfulName).isPresent());
        assertTrue(categoryRepository.findByName(failingName).isPresent());
    }

    @Test
    void deleteEventFailure_failsWholeRequest() {
        String firstName = uniqueName("delete-first");
        String secondName = uniqueName("delete-second");
        String firstId = createCategory(firstName);
        String secondId = createCategory(secondName);
        EntityEvents.throwOnNextCategoryDelete.set(true);

        client.deleteMany("Category")
                .items(List.of(key(firstId), key(secondId)))
                .execute()
                .expectStatus(500);

        // All-or-nothing: neither row is deleted when one item's PRE_DELETE event fails.
        assertTrue(categoryRepository.findByName(firstName).isPresent());
        assertTrue(categoryRepository.findByName(secondName).isPresent());
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
