package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import com.github.silent.samurai.speedy.entity.Company;
import com.github.silent.samurai.speedy.entity.CompanyStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.github.silent.samurai.speedy.util.SpeedyTestUtil;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests verifying PRE_INSERT, PRE_UPDATE and PRE_DELETE events for Company entity.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CompanyEventTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SpeedyClient<SpeedyResponse> speedyClient;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        speedyClient = SpeedyClient.restTemplate(restTemplate, "http://localhost");
    }

    private String uniquePhone() {
        String nanos = Long.toString(System.nanoTime());
        return "+44" + nanos.substring(Math.max(0, nanos.length() - 10));
    }

    @Test
    void testCompanyPreInsertEvent() throws Exception {
        String email = "event-company-" + System.nanoTime() + "@example.com";

        SpeedyResponse response = speedyClient.create("Company")
                .addField("name", "Event Co 1")
                .addField("address", "221B Baker Street")
                .addField("email", email)
                .addField("phone", uniquePhone())
                .addField("currency", "GBP")
                // intentionally NOT setting status to let event default it
                .execute();

        SpeedyTestUtil.assertThat(response)
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[0].id", String.class).is(not(isEmptyOrNullString()));

        String id = SpeedyTestUtil.assertThat(response)
                .path("$.payload[0].id", String.class)
                .get();

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

        // create company first
        SpeedyResponse create = speedyClient.create("Company")
                .addField("name", "Event Co 2")
                .addField("address", "221B Baker Street")
                .addField("email", email)
                .addField("phone", uniquePhone())
                .addField("currency", "GBP")
                .addField("status", "DRAFT")
                .execute();

        SpeedyTestUtil.assertThat(create)
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[0].id", String.class).is(not(isEmptyOrNullString()));

        String id = SpeedyTestUtil.assertThat(create)
                .path("$.payload[0].id", String.class)
                .get();

        EntityManager em = entityManagerFactory.createEntityManager();
        Company beforeUpdate = em.createQuery("SELECT c FROM Company c WHERE c.id = :id", Company.class)
                .setParameter("id", id)
                .getSingleResult();
        LocalDateTime initialUpdatedAt = beforeUpdate.getUpdatedAt();
        em.clear();

        // wait a bit to observe timestamp difference
        Thread.sleep(1000);

        // perform update triggering PRE_UPDATE event
        SpeedyResponse upd = speedyClient.update("Company")
                .key("id", id)
                .field("name", "Event Co 2 Updated")
                .execute();

        SpeedyTestUtil.assertThat(upd)
                .path("$.payload.length()", Integer.class).is(greaterThan(0));

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
    void testCompanyPreDeleteEvent() throws Exception {
        String email = "event-company-" + System.nanoTime() + "@example.com";

        // create company
        SpeedyResponse create = speedyClient.create("Company")
                .addField("name", "Event Co 3")
                .addField("address", "221B Baker Street")
                .addField("email", email)
                .addField("phone", uniquePhone())
                .addField("currency", "GBP")
                .addField("status", "DRAFT")
                .execute();

        SpeedyTestUtil.assertThat(create)
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[0].id", String.class).is(not(isEmptyOrNullString()));

        String id = SpeedyTestUtil.assertThat(create)
                .path("$.payload[0].id", String.class)
                .get();

        // delete company (soft delete, expect deletedAt to be set)
        SpeedyResponse del = speedyClient.delete("Company")
                .key("id", id)
                .execute();

        SpeedyTestUtil.assertThat(del)
                .path("$.payload.length()", Integer.class).is(greaterThan(0));

        EntityManager em = entityManagerFactory.createEntityManager();
        List<Company> deleted = em.createQuery("SELECT c FROM Company c WHERE c.id = :id", Company.class)
                .setParameter("id", id)
                .getResultList();

        assertTrue(deleted.isEmpty(), "Company should be deleted");
        em.close();
    }
}
