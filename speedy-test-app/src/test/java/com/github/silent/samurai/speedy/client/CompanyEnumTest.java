package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import com.github.silent.samurai.speedy.entity.Company;
import com.github.silent.samurai.speedy.util.SpeedyTestUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CompanyEnumTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    SpeedyTest speedyClient;

    private String lastEmail;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private String createCompany(String name, String status) throws Exception {
        String nanos = Long.toString(System.nanoTime());
        String phone = "+44" + nanos.substring(Math.max(0, nanos.length() - 10));
        String email = "company-" + nanos + "@example.com";
        SpeedyTestResult result = speedyClient.create("Company")
                .field("name", name)
                .field("address", "221B Baker Street")
                .field("email", email)
                .field("phone", phone)
                .field("currency", "GBP")
                .field("status", status)
                .execute();

        var idPath = SpeedyTestUtil.assertThat(result.responseBody())
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[0].id", String.class).is(not(emptyString()))
                .path("$.payload[0].id", String.class);
        String id = idPath.get();
        lastEmail = email;
        return id;
    }

    private void assertCompany(String id, String expectedStatus) throws Exception {
        SpeedyTestResult get = speedyClient.get("Company")
                .key("id", id)
                .execute();
        SpeedyTestUtil.assertThat(get.responseBody())
                .path("$.payload[0].status", String.class).is(equalTo(expectedStatus));
    }

    private void updateCompanyStatus(String id, String newStatus) throws Exception {
        SpeedyTestResult upd = speedyClient.update("Company")
                .key("id", id)
                .field("status", newStatus)
                .execute();
        SpeedyTestUtil.assertThat(upd.responseBody())
                .path("$.payload[0].status", String.class).is(equalTo(newStatus));
    }

    private void queryByStatus(String status, String expectedId) throws Exception {
        SpeedyTestResult query = speedyClient.query("Company")
                .where(condition("status", eq(status)))
                .execute();
        SpeedyTestUtil.assertThat(query.responseBody())
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[*].id", Iterable.class).is(hasItem(expectedId))
                .path("$.payload[*].status", Iterable.class).is(hasItem(status));
    }

    @Test
    void testEnumCrudAndQuery() throws Exception {
        String id = createCompany("enum-co-1", "DRAFT");
        assertCompany(id, "DRAFT");

        EntityManager em = entityManagerFactory.createEntityManager();
        Company created = em.createQuery("SELECT c FROM Company c WHERE c.email = :email", Company.class)
                .setParameter("email", lastEmail)
                .getSingleResult();
        assertNotNull(created.getCreatedAt(), "createdAt should be set by PRE_INSERT event");

        updateCompanyStatus(id, "PENDING");
        assertCompany(id, "PENDING");

        updateCompanyStatus(id, "READY");
        assertCompany(id, "READY");

        em.clear();
        Company updated = em.createQuery("SELECT c FROM Company c WHERE c.email = :email", Company.class)
                .setParameter("email", lastEmail)
                .getSingleResult();
        assertNotNull(updated.getUpdatedAt(), "updatedAt should be set by PRE_UPDATE event");

        queryByStatus("READY", id);

        updateCompanyStatus(id, "BLOCKED");
        assertCompany(id, "BLOCKED");
        queryByStatus("BLOCKED", id);

        updateCompanyStatus(id, "COMPLETE");
        assertCompany(id, "COMPLETE");
        queryByStatus("COMPLETE", id);

        SpeedyTestResult delResult = speedyClient.delete("Company").key("id", id).execute();
        SpeedyTestUtil.assertThat(delResult.responseBody())
                .path("$.payload[0].id", String.class).is(equalTo(id));
        em.close();
    }
}
