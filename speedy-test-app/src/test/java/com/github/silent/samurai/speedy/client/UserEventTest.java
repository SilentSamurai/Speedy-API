package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import com.github.silent.samurai.speedy.entity.User;
import com.github.silent.samurai.speedy.util.SpeedyTestUtil;
import com.jayway.jsonpath.DocumentContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class UserEventTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserEventTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    SpeedyClient<SpeedyResponse> speedyClient;
    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        speedyClient = SpeedyClient.restTemplate(restTemplate, "http://localhost");
    }

    @Test
    void testUserPreInsertEvent() throws Exception {

        SpeedyResponse createResponse = speedyClient.create("User")
                .addField("name", "Test User")
                .addField("phoneNo", "1234567890")
                .addField("email", "test@example.com")
                .addField("type", "regular")
                .execute();

        assertFalse(createResponse.getPayload().isEmpty());
        JsonNode createdEntity = createResponse.getPayload().get(0);

        assertTrue(createdEntity.has("id"));
        String id = createdEntity.get("id").asText();
        assertNotNull(id);

        // Verify that the user was created
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        List<User> users = entityManager.createQuery("SELECT u FROM User u WHERE u.email = 'test@example.com'", User.class).getResultList();
        assertFalse(users.isEmpty(), "User should be created");

        User createdUser = users.get(0);
        assertNotNull(createdUser.getCreatedAt(), "createdAt should be set by PRE_INSERT event");

        // Verify that the createdAt field is recent (within the last few seconds)
        LocalDateTime now = LocalDateTime.now();
        assertTrue(createdUser.getCreatedAt().isBefore(now.plusSeconds(1)), "createdAt should be before now");
        assertTrue(createdUser.getCreatedAt().isAfter(now.minusSeconds(10)), "createdAt should be after a few seconds ago");
        entityManager.close();
    }

    @Test
    void testUserPreUpdateEvent() throws Exception {
        // First create a user

        SpeedyResponse createResponse = speedyClient.create("User")
                .addField("name", "Test User")
                .addField("phoneNo", "34534534534")
                .addField("email", "test2@example.com")
                .addField("type", "regular")
                .execute();

        assertFalse(createResponse.getPayload().isEmpty());
        JsonNode createdEntity = createResponse.getPayload().get(0);

        assertTrue(createdEntity.has("id"));
        String id = createdEntity.get("id").asText();
        assertNotNull(id);

        // Get the created user
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        List<User> users = entityManager.createQuery("SELECT u FROM User u WHERE u.email = 'test2@example.com'", User.class).getResultList();
        assertFalse(users.isEmpty(), "User should be created");
        User createdUser = users.get(0);
        entityManager.clear(); // Clear the entity manager to get fresh data
        // Store the initial updatedAt value
        LocalDateTime initialUpdatedAt = createdUser.getUpdatedAt();

        // Wait a bit to ensure time difference
        Thread.sleep(1000);

        // Update the user
        createdUser.setName("Updated Test User");

        SpeedyResponse updateResponse = speedyClient.update("User")
                .key("id", id)
                .field("name", "Updated Test User")
                .execute();

        // Verify that the user was updated

        List<User> updatedUsers = entityManager.createQuery("SELECT u FROM User u WHERE u.email = 'test2@example.com'", User.class).getResultList();
        assertFalse(updatedUsers.isEmpty(), "User should exist after update");

        User updatedUser = updatedUsers.get(0);
        assertEquals("Updated Test User", updatedUser.getName(), "Name should be updated");

        // Verify that the PRE_UPDATE event set the updatedAt field
        assertNotNull(updatedUser.getUpdatedAt(), "updatedAt should be set by PRE_UPDATE event");

        // If initialUpdatedAt was null, just check that updatedAt is set
        // If it wasn't null, check that it was updated
        if (initialUpdatedAt != null) {
            assertTrue(updatedUser.getUpdatedAt().isAfter(initialUpdatedAt), "updatedAt should be updated");
        }
        entityManager.close();
    }

    @Test
    @Disabled
        // TODO: support soft delete
    void testUserPreDeleteEvent() throws Exception {
        // First create a user
        SpeedyResponse createResponse = speedyClient.create("User")
                .addField("name", "Test User")
                .addField("phoneNo", "1234567890")
                .addField("email", "test3@example.com")
                .addField("type", "regular")
                .execute();

        assertFalse(createResponse.getPayload().isEmpty());
        JsonNode createdEntity = createResponse.getPayload().get(0);

        assertTrue(createdEntity.has("id"));
        String id = createdEntity.get("id").asText();
        assertNotNull(id);

        // Get the created user
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        List<User> users = entityManager.createQuery("SELECT u FROM User u WHERE u.email = 'test3@example.com'", User.class).getResultList();
        assertFalse(users.isEmpty(), "User should be created");
        User createdUser = users.get(0);
        String userId = createdUser.getId();
        entityManager.clear(); // Clear the entity manager to get fresh data

        // Delete the user
        SpeedyResponse deleteResponse = speedyClient.delete("User")
                .key("id", id)
                .execute();

        // Verify that the user still exists in the database but has deletedAt set

        List<User> deletedUsers = entityManager.createQuery("SELECT u FROM User u WHERE u.id = :userId", User.class)
                .setParameter("userId", userId)
                .getResultList();
        assertFalse(deletedUsers.isEmpty(), "User should still exist in database after 'deletion'");

        User deletedUser = deletedUsers.get(0);
        assertNotNull(deletedUser.getDeletedAt(), "deletedAt should be set by PRE_DELETE event");

        // Verify that the deletedAt field is recent
        LocalDateTime now = LocalDateTime.now();
        assertTrue(deletedUser.getDeletedAt().isBefore(now.plusSeconds(1)), "deletedAt should be before now");
        assertTrue(deletedUser.getDeletedAt().isAfter(now.minusSeconds(10)), "deletedAt should be after a few seconds ago");
        entityManager.close();
    }

    @Test
    void testCreateAndFetchProductWithCategoryAssociation() throws Exception {
        // Use an existing seeded Category with id "1" (as used elsewhere in tests)
        String uniqueName = "assoc-product-" + System.currentTimeMillis();

        SpeedyResponse createResponse = speedyClient.create("Product")
                .addField("name", uniqueName)
                .addField("description", "association test")
                .addField("category.id", "1")
                .execute();

        assertFalse(createResponse.getPayload().isEmpty());
        JsonNode created = createResponse.getPayload().get(0);
        assertTrue(created.has("id"));
        String productId = created.get("id").asText();
        assertNotNull(productId);

        // Fetch and verify nested association
        SpeedyResponse getResponse = speedyClient.get("Product")
                .key("id", productId)
                .execute();

        assertFalse(getResponse.getPayload().isEmpty());
        JsonNode fetched = getResponse.getPayload().get(0);
        assertEquals(productId, fetched.get("id").asText());
        assertEquals(uniqueName, fetched.get("name").asText());

        assertTrue(fetched.has("category"), "Product should contain nested category");
        JsonNode fetchedCategory = fetched.get("category");
        assertEquals("1", fetchedCategory.get("id").asText(), "Default category id should be '1'");

        // Verify PRE_INSERT Product event mutated description
        assertTrue(fetched.has("description"));
        assertEquals("created-by-event", fetched.get("description").asText());
    }

    @Test
    void testUpdateProductCategoryAssociation() throws Exception {
        // Create a Product initially associated with Category "1"
        String uniqueName = "assoc-product-update-" + System.currentTimeMillis();

        SpeedyResponse createProduct = speedyClient.create("Product")
                .addField("name", uniqueName)
                .addField("description", "association update test")
                .addField("category.id", "1")
                .execute();

        assertFalse(createProduct.getPayload().isEmpty());
        JsonNode productNode = createProduct.getPayload().get(0);
        String productId = productNode.get("id").asText();

        // Create a new Category to re-associate to
        String newCategoryName = "assoc-category-" + System.currentTimeMillis();
        SpeedyResponse createCategory = speedyClient.create("Category")
                .addField("name", newCategoryName)
                .execute();

        DocumentContext createdCategoryJson = SpeedyTestUtil.jsonPath(createCategory);
        String newCategoryId = createdCategoryJson.read("$.payload[0].id");
        assertNotNull(newCategoryId);

        // Update the Product's association
        SpeedyResponse updateResponse = speedyClient.update("Product")
                .key("id", productId)
                .field("category.id", newCategoryId)
                .execute();

        assertFalse(updateResponse.getPayload().isEmpty());

        // Fetch and verify new association
        SpeedyResponse getResponse = speedyClient.get("Product")
                .key("id", productId)
                .execute();
        DocumentContext jsonPath = SpeedyTestUtil.jsonPath(getResponse);

        // JSONPath assertions
        assertEquals(newCategoryId, jsonPath.read("$.payload[0].category.id"));
        assertEquals("updated-by-event", jsonPath.read("$.payload[0].description"));
    }
}
