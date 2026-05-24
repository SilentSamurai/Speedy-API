package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.entity.Company;
import com.github.silent.samurai.speedy.entity.CompanyStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CompanyEventTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private String uniquePhone() {
        String nanos = Long.toString(System.nanoTime());
        return "+44" + nanos.substring(Math.max(0, nanos.length() - 10));
    }

    @Test
    void testCompanyPreInsertEvent() {
        String email = "event-company-" + System.nanoTime() + "@example.com";

        SpeedyTestResult response = speedyClient.create("Company")
                .field("name", "Event Co 1")
                .field("address", "221B Baker Street")
                .field("email", email)
                .field("phone", uniquePhone())
                .field("currency", "GBP")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", greaterThan(0))
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        EntityManager em = entityManagerFactory.createEntityManager();
        Company created = em.createQuery("SELECT c FROM Company c WHERE c.email = :email", Company.class)
                .setParameter("email", email)
                .getSingleResult();

        assertNotNull(created.getCreatedAt(), "createdAt should be set by PRE_INSERT event");
        assertEquals(CompanyStatus.DRAFT, created.getStatus(), "Status should default to DRAFT in PRE_INSERT event");
        em.close();
    }

    @Test
    void testCompanyPreUpdateEvent() throws Exception {
        String email = "event-company-" + System.nanoTime() + "@example.com";

        SpeedyTestResult create = speedyClient.create("Company")
                .field("name", "Event Co 2")
                .field("address", "221B Baker Street")
                .field("email", email)
                .field("phone", uniquePhone())
                .field("currency", "GBP")
                .field("status", "DRAFT")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", greaterThan(0))
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = create.jsonPath("$.payload[0].id");

        EntityManager em = entityManagerFactory.createEntityManager();
        Company beforeUpdate = em.createQuery("SELECT c FROM Company c WHERE c.id = :id", Company.class)
                .setParameter("id", id)
                .getSingleResult();
        LocalDateTime initialUpdatedAt = beforeUpdate.getUpdatedAt();
        em.clear();

        Thread.sleep(1000);

        speedyClient.update("Company")
                .key("id", id)
                .field("name", "Event Co 2 Updated")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", greaterThan(0));

        Company afterUpdate = em.createQuery("SELECT c FROM Company c WHERE c.id = :id", Company.class)
                .setParameter("id", id)
                .getSingleResult();

        assertNotNull(afterUpdate.getUpdatedAt(), "updatedAt should be set by PRE_UPDATE event");
        if (initialUpdatedAt != null) {
            assertTrue(afterUpdate.getUpdatedAt().isAfter(initialUpdatedAt), "updatedAt should be newer than initial value");
        }
        em.close();
    }

    @Test
    void testCompanyPreDeleteEvent() {
        String email = "event-company-" + System.nanoTime() + "@example.com";

        SpeedyTestResult create = speedyClient.create("Company")
                .field("name", "Event Co 3")
                .field("address", "221B Baker Street")
                .field("email", email)
                .field("phone", uniquePhone())
                .field("currency", "GBP")
                .field("status", "DRAFT")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", greaterThan(0))
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = create.jsonPath("$.payload[0].id");

        speedyClient.delete("Company")
                .key("id", id)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", greaterThan(0));

        EntityManager em = entityManagerFactory.createEntityManager();
        List<Company> deleted = em.createQuery("SELECT c FROM Company c WHERE c.id = :id", Company.class)
                .setParameter("id", id)
                .getResultList();

        assertTrue(deleted.isEmpty(), "Company should be deleted");
        em.close();
    }
}
