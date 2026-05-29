package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.Company;
import com.github.silent.samurai.speedy.events.EntityEvents;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class EventPostTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
        EntityEvents.POST_UPDATE_FIRED = false;
    }

    @Test
    void postInsertEvent_firesAndEntityPersisted() {
        String categoryName = "POST_INSERT_DB_" + System.nanoTime();

        SpeedyTestResult result = client.create("Category")
                .field("name", categoryName)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = result.jsonPath("$.payload[0].id");

        assertTrue(EntityEvents.POST_INSERT_CATEGORIES.containsKey(id),
                "POST_INSERT handler should track category ID in map. ID: " + id);

        EntityManager em = entityManagerFactory.createEntityManager();
        Category persisted = em.find(Category.class, id);
        assertNotNull(persisted, "Category should exist in DB after create");
        assertEquals(categoryName, persisted.getName(),
                "Category name should match what was submitted");
        em.close();
    }

    @Test
    void postUpdateEvent_firesAndEntityUpdated() {
        String email = "post-update-" + System.nanoTime() + "@example.com";
        String phone = "+44" + Long.toString(System.nanoTime()).substring(4);
        String updatedName = "POST_UPDATE Flag Co Renamed " + System.nanoTime();

        SpeedyTestResult createResult = client.create("Company")
                .field("name", "POST_UPDATE Flag Co")
                .field("address", "123 Test St")
                .field("email", email)
                .field("phone", phone)
                .field("currency", "GBP")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = createResult.jsonPath("$.payload[0].id");

        client.update("Company")
                .key("id", id)
                .field("name", updatedName)
                .execute()
                .expectOk();

        assertTrue(EntityEvents.POST_UPDATE_FIRED,
                "POST_UPDATE event flag should be set after company update");

        EntityManager em = entityManagerFactory.createEntityManager();
        Company updated = em.find(Company.class, id);
        assertNotNull(updated, "Company should exist in DB after update");
        assertEquals(updatedName, updated.getName(),
                "Company name should reflect the update");
        em.close();
    }

    @Test
    void postDeleteEvent_firesAndEntityRemoved() {
        String email = "post-delete-" + System.nanoTime() + "@example.com";
        String phone = "+44" + Long.toString(System.nanoTime()).substring(4);

        SpeedyTestResult createResult = client.create("Company")
                .field("name", "POST_DELETE Counter Co")
                .field("address", "123 Test St")
                .field("email", email)
                .field("phone", phone)
                .field("currency", "GBP")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = createResult.jsonPath("$.payload[0].id");

        int counterBefore = EntityEvents.POST_DELETE_COUNTER.get();

        client.delete("Company")
                .key("id", id)
                .execute()
                .expectOk();

        int counterAfter = EntityEvents.POST_DELETE_COUNTER.get();

        assertTrue(counterAfter > counterBefore,
                "POST_DELETE_COUNTER should have incremented. Before: " + counterBefore + ", After: " + counterAfter);

        EntityManager em = entityManagerFactory.createEntityManager();
        List<Company> deleted = em.createQuery(
                        "SELECT c FROM Company c WHERE c.id = :id", Company.class)
                .setParameter("id", id)
                .getResultList();
        assertTrue(deleted.isEmpty(), "Company should be deleted from DB");
        em.close();
    }

    @Test
    void speedyEntityParameter_receivesEntityDataCorrectly() {
        String categoryName = "SpeedyEntity_DB_" + System.nanoTime();

        SpeedyTestResult result = client.create("Category")
                .field("name", categoryName)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = result.jsonPath("$.payload[0].id");

        assertTrue(EntityEvents.POST_INSERT_CATEGORIES.containsKey(id),
                "SpeedyEntity parameter should pass entity ID to handler. ID: " + id);

        EntityManager em = entityManagerFactory.createEntityManager();
        Category persisted = em.find(Category.class, id);
        assertNotNull(persisted, "Category should exist in DB after SpeedyEntity-handled create");
        assertEquals(categoryName, persisted.getName(),
                "Category name should match submitted value");
        em.close();
    }
}
