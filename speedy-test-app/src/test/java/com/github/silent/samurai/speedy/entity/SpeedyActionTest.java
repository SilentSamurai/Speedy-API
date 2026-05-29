package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyActionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @Test
    void virtualEntity_postCreate_shouldBeBlocked() {
        client.create("VirtualEntity")
                .field("name", "test")
                .execute()
                .expectBadRequest();
    }

    @Test
    void virtualEntity_putUpdate_shouldBeBlocked() {
        client.update("VirtualEntity")
                .key("id", "any-id")
                .field("name", "test")
                .execute()
                .expectBadRequest();
    }

    @Test
    void virtualEntity_delete_shouldBeBlocked() {
        client.delete("VirtualEntity")
                .key("id", "any-id")
                .execute()
                .expectBadRequest();
    }

    @Test
    void userCreate_withCreatedAtSupplied_valueIsPersisted() {
        String uniquePhone = "+91-" + Long.toString(System.nanoTime()).substring(4);
        String uniqueEmail = "user-" + System.nanoTime() + "@example.com";
        LocalDateTime suppliedCreatedAt = LocalDateTime.of(2000, 1, 1, 0, 0);

        SpeedyTestResult result = client.create("User")
                .field("name", "Action Test User")
                .field("phoneNo", uniquePhone)
                .field("email", uniqueEmail)
                .field("type", "DEFAULT")
                .field("createdAt", suppliedCreatedAt.toString())
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = result.jsonPath("$.payload[0].id");

        jakarta.persistence.EntityManager em = entityManagerFactory.createEntityManager();
        User persisted = em.createQuery(
                        "SELECT u FROM User u WHERE u.id = :id", User.class)
                .setParameter("id", id)
                .getSingleResult();

        assertNotNull(persisted.getCreatedAt(), "createdAt should be persisted");
        assertNotEquals(suppliedCreatedAt, persisted.getCreatedAt(),
                "User-supplied createdAt is overridden by PRE_INSERT handler which sets LocalDateTime.now()");
        em.close();
    }

    @Test
    void customerCreate_withCreatedBySupplied_valueIsNotPersisted() {
        String uniquePhone = "0123456789";
        String uniqueEmail = "customer-" + System.nanoTime() + "@example.com";

        SpeedyTestResult result = client.create("Customer")
                .field("name", "Action Test Customer")
                .field("phoneNo", uniquePhone)
                .field("email", uniqueEmail)
                .field("createdBy", "hacker")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = result.jsonPath("$.payload[0].id");

        jakarta.persistence.EntityManager em = entityManagerFactory.createEntityManager();
        Customer persisted = em.createQuery(
                        "SELECT c FROM Customer c WHERE c.id = :id", Customer.class)
                .setParameter("id", id)
                .getSingleResult();

        assertNull(persisted.getCreatedBy(),
                "User-supplied createdBy must NOT be persisted — field is @SpeedyAction(READ) only");

        em.close();
    }

    @Test
    void userGet_includesReadOnlyFields() {
        String uniquePhone = "+91-" + Long.toString(System.nanoTime()).substring(4);
        String uniqueEmail = "user-get-" + System.nanoTime() + "@example.com";

        SpeedyTestResult createResult = client.create("User")
                .field("name", "Get Timestamps User")
                .field("phoneNo", uniquePhone)
                .field("email", uniqueEmail)
                .field("type", "DEFAULT")
                .execute()
                .expectOk();

        String id = createResult.jsonPath("$.payload[0].id");

        client.get("User")
                .key("id", id)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", equalTo(id))
                .expectJsonPath("$.payload[0].name", equalTo("Get Timestamps User"))
                .expectJsonPath("$.payload[0].createdAt", notNullValue());
    }
}
