package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import com.github.silent.samurai.speedy.entity.Company;
import com.github.silent.samurai.speedy.util.SpeedyTestUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.eq;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CompanyEnumTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    SpeedyClient<SpeedyResponse> speedyClient;

    // store last created email for DB assertions
    private String lastEmail;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        speedyClient = SpeedyClient.restTemplate(restTemplate, "http://localhost");
    }

    private String createCompany(String name, String status) throws Exception {
        String nanos = Long.toString(System.nanoTime());
        String phone = "+44" + nanos.substring(Math.max(0, nanos.length() - 10)); // <= 13 chars, unique per run
        String email = "company-" + nanos + "@example.com";
        SpeedyResponse response = speedyClient.create("Company")
                .addField("name", name)
                .addField("address", "221B Baker Street")
                .addField("email", email)
                .addField("phone", phone)
                .addField("currency", "GBP")
                .addField("status", status)
                .execute();

        // Assert id is present and capture it via the fluent JSONPath API
        var idPath = SpeedyTestUtil.assertThat(response)
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[0].id", String.class).is(not(emptyString()))
                .path("$.payload[0].id", String.class);
        String id = idPath.get();
        lastEmail = email;
        return id;
    }

    private void assertCompany(String id, String expectedStatus) throws Exception {
        SpeedyResponse get = speedyClient.get("Company")
                .key("id", id)
                .execute();
        SpeedyTestUtil.assertThat(get)
                .path("$.payload[0].status", String.class).is(equalTo(expectedStatus));
    }

    private void updateCompanyStatus(String id, String newStatus) throws Exception {
        SpeedyResponse upd = speedyClient.update("Company")
                .key("id", id)
                .field("status", newStatus)
                .execute();
        SpeedyTestUtil.assertThat(upd)
                .path("$.payload[0].status", String.class).is(equalTo(newStatus));
    }

    private void queryByStatus(String status, String expectedId) throws Exception {
        SpeedyResponse query = speedyClient.query(
                SpeedyQuery.from("Company").where(condition("status", eq(status)))
        ).execute();
        SpeedyTestUtil.assertThat(query)
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[*].id", Iterable.class).is(hasItem(expectedId))
                .path("$.payload[*].status", Iterable.class).is(hasItem(status));
    }

    @Test
    void testEnumCrudAndQuery() throws Exception {
        // Create with DRAFT
        String id = createCompany("enum-co-1", "DRAFT");
        assertCompany(id, "DRAFT");

        // Assert PRE_INSERT event set createdAt via JPA query
        EntityManager em = entityManagerFactory.createEntityManager();
        Company created = em.createQuery("SELECT c FROM Company c WHERE c.email = :email", Company.class)
                .setParameter("email", lastEmail)
                .getSingleResult();
        assertNotNull(created.getCreatedAt(), "createdAt should be set by PRE_INSERT event");

        // Update to PENDING
        updateCompanyStatus(id, "PENDING");
        assertCompany(id, "PENDING");

        // Update to READY
        updateCompanyStatus(id, "READY");
        assertCompany(id, "READY");

        // Assert PRE_UPDATE event set updatedAt via JPA query
        em.clear();
        Company updated = em.createQuery("SELECT c FROM Company c WHERE c.email = :email", Company.class)
                .setParameter("email", lastEmail)
                .getSingleResult();
        assertNotNull(updated.getUpdatedAt(), "updatedAt should be set by PRE_UPDATE event");

        // Query by READY
        queryByStatus("READY", id);

        // Update to BLOCKED
        updateCompanyStatus(id, "BLOCKED");
        assertCompany(id, "BLOCKED");
        queryByStatus("BLOCKED", id);

        // Finally, COMPLETE
        updateCompanyStatus(id, "COMPLETE");
        assertCompany(id, "COMPLETE");
        queryByStatus("COMPLETE", id);

        // Cleanup
        SpeedyResponse del = speedyClient.delete("Company").key("id", id).execute();
        SpeedyTestUtil.assertThat(del)
                .path("$.payload[0].id", String.class).is(equalTo(id));
        em.close();
    }
}
