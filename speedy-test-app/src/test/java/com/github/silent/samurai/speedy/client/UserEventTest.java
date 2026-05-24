package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
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
import org.springframework.test.web.servlet.MockMvc;

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
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void testUserPreInsertEvent() throws Exception {

        SpeedyTestResult createResponse = speedyClient.create("User")
                .field("name", "Test User")
                .field("phoneNo", "1234567890")
                .field("email", "test@example.com")
                .field("type", "regular")
                .execute()
                .expectOk();

        String id = createResponse.jsonPath("$.payload[0].id");
        assertNotNull(id);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        List<User> users = entityManager.createQuery("SELECT u FROM User u WHERE u.email = 'test@example.com'", User.class).getResultList();
        assertFalse(users.isEmpty(), "User should be created");

        User createdUser = users.get(0);
        assertNotNull(createdUser.getCreatedAt(), "createdAt should be set by PRE_INSERT event");

        LocalDateTime now = LocalDateTime.now();
        assertTrue(createdUser.getCreatedAt().isBefore(now.plusSeconds(1)), "createdAt should be before now");
        assertTrue(createdUser.getCreatedAt().isAfter(now.minusSeconds(10)), "createdAt should be after a few seconds ago");
        entityManager.close();
    }

    @Test
    void testUserPreUpdateEvent() throws Exception {

        SpeedyTestResult createResponse = speedyClient.create("User")
                .field("name", "Test User")
                .field("phoneNo", "34534534534")
                .field("email", "test2@example.com")
                .field("type", "regular")
                .execute()
                .expectOk();

        String id = createResponse.jsonPath("$.payload[0].id");
        assertNotNull(id);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        List<User> users = entityManager.createQuery("SELECT u FROM User u WHERE u.email = 'test2@example.com'", User.class).getResultList();
        assertFalse(users.isEmpty(), "User should be created");
        User createdUser = users.get(0);
        entityManager.clear();
        LocalDateTime initialUpdatedAt = createdUser.getUpdatedAt();

        Thread.sleep(1000);

        SpeedyTestResult updateResponse = speedyClient.update("User")
                .key("id", id)
                .field("name", "Updated Test User")
                .execute()
                .expectOk();

        List<User> updatedUsers = entityManager.createQuery("SELECT u FROM User u WHERE u.email = 'test2@example.com'", User.class).getResultList();
        assertFalse(updatedUsers.isEmpty(), "User should exist after update");

        User updatedUser = updatedUsers.get(0);
        assertEquals("Updated Test User", updatedUser.getName(), "Name should be updated");

        assertNotNull(updatedUser.getUpdatedAt(), "updatedAt should be set by PRE_UPDATE event");

        if (initialUpdatedAt != null) {
            assertTrue(updatedUser.getUpdatedAt().isAfter(initialUpdatedAt), "updatedAt should be updated");
        }
        entityManager.close();
    }

    @Test
    @Disabled
    void testUserPreDeleteEvent() throws Exception {
        SpeedyTestResult createResponse = speedyClient.create("User")
                .field("name", "Test User")
                .field("phoneNo", "1234567890")
                .field("email", "test3@example.com")
                .field("type", "regular")
                .execute()
                .expectOk();

        String id = createResponse.jsonPath("$.payload[0].id");
        assertNotNull(id);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        List<User> users = entityManager.createQuery("SELECT u FROM User u WHERE u.email = 'test3@example.com'", User.class).getResultList();
        assertFalse(users.isEmpty(), "User should be created");
        User createdUser = users.get(0);
        String userId = createdUser.getId();
        entityManager.clear();

        speedyClient.delete("User")
                .key("id", id)
                .execute()
                .expectOk();

        List<User> deletedUsers = entityManager.createQuery("SELECT u FROM User u WHERE u.id = :userId", User.class)
                .setParameter("userId", userId)
                .getResultList();
        assertFalse(deletedUsers.isEmpty(), "User should still exist in database after 'deletion'");

        User deletedUser = deletedUsers.get(0);
        assertNotNull(deletedUser.getDeletedAt(), "deletedAt should be set by PRE_DELETE event");

        LocalDateTime now = LocalDateTime.now();
        assertTrue(deletedUser.getDeletedAt().isBefore(now.plusSeconds(1)), "deletedAt should be before now");
        assertTrue(deletedUser.getDeletedAt().isAfter(now.minusSeconds(10)), "deletedAt should be after a few seconds ago");
        entityManager.close();
    }

    @Test
    void testCreateAndFetchProductWithCategoryAssociation() throws Exception {
        String uniqueName = "assoc-product-" + System.currentTimeMillis();

        SpeedyTestResult createResponse = speedyClient.create("Product")
                .field("name", uniqueName)
                .field("description", "association test")
                .field("category.id", "1")
                .execute()
                .expectOk();

        String productId = createResponse.jsonPath("$.payload[0].id");
        assertNotNull(productId);

        SpeedyTestResult getResponse = speedyClient.get("Product")
                .key("id", productId)
                .execute()
                .expectOk();

        assertEquals(productId, getResponse.jsonPath("$.payload[0].id"));
        assertEquals(uniqueName, getResponse.jsonPath("$.payload[0].name"));
        assertEquals("1", getResponse.jsonPath("$.payload[0].category.id"));
        assertEquals("created-by-event", getResponse.jsonPath("$.payload[0].description"));
    }

    @Test
    void testUpdateProductCategoryAssociation() throws Exception {
        String uniqueName = "assoc-product-update-" + System.currentTimeMillis();

        SpeedyTestResult createProduct = speedyClient.create("Product")
                .field("name", uniqueName)
                .field("description", "association update test")
                .field("category.id", "1")
                .execute()
                .expectOk();

        String productId = createProduct.jsonPath("$.payload[0].id");

        String newCategoryName = "assoc-category-" + System.currentTimeMillis();
        SpeedyTestResult createCategory = speedyClient.create("Category")
                .field("name", newCategoryName)
                .execute()
                .expectOk();

        DocumentContext createdCategoryJson = SpeedyTestUtil.jsonPath(createCategory.responseBody());
        String newCategoryId = createdCategoryJson.read("$.payload[0].id");
        assertNotNull(newCategoryId);

        speedyClient.update("Product")
                .key("id", productId)
                .field("category.id", newCategoryId)
                .execute()
                .expectOk();

        SpeedyTestResult getResponse = speedyClient.get("Product")
                .key("id", productId)
                .execute()
                .expectOk();
        DocumentContext jsonPath = SpeedyTestUtil.jsonPath(getResponse.responseBody());

        assertEquals(newCategoryId, jsonPath.read("$.payload[0].category.id"));
        assertEquals("updated-by-event", jsonPath.read("$.payload[0].description"));
    }
}
