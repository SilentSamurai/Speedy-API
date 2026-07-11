package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/// Integration tests for soft-delete support (issue #116): default hiding, the $deleted read
/// modes, the $restore/$purge endpoints, and the secure-by-default gating.
///
/// {@code User} is soft-delete enabled with viewing + hard delete allowed;
/// {@code SoftDeleteRestrictedEntity} is soft-delete enabled with both gates OFF; {@code Company}
/// is a hard-delete entity used to assert $restore/$purge are rejected where soft delete is off.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SoftDeleteTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private static final java.util.concurrent.atomic.AtomicInteger SEQ =
            new java.util.concurrent.atomic.AtomicInteger();

    private String createUser() {
        // Unique 13-digit phone starting with '8' so it never collides with the fixed 10/11-digit
        // phones used by other User tests, plus a per-call sequence to stay unique within this run.
        long n = System.nanoTime();
        String phone = "8" + String.format("%011d", (n + SEQ.incrementAndGet()) % 100_000_000_000L);
        SpeedyTestResult res = speedyClient.create("User")
                .field("name", "SoftDelete User")
                .field("phoneNo", phone)
                .field("email", "sd-" + n + "-" + SEQ.get() + "@example.com")
                .field("type", "regular")
                .execute()
                .expectOk();
        String id = res.jsonPath("$.payload[0].id");
        assertNotNull(id);
        return id;
    }

    private boolean rowExists(String id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            List<User> rows = em.createQuery("SELECT u FROM User u WHERE u.id = :id", User.class)
                    .setParameter("id", id)
                    .getResultList();
            return !rows.isEmpty();
        } finally {
            em.close();
        }
    }

    @Test
    void softDeleteHidesRowFromDefaultReads() {
        String id = createUser();

        speedyClient.delete("User").key("id", id).execute().expectOk();

        // Default GET-by-key excludes the soft-deleted row.
        speedyClient.get("User").key("id", id).execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", is(0));

        // But the row still physically exists with deletedAt set.
        assertTrue(rowExists(id), "soft-deleted row should still exist in the database");
    }

    @Test
    void secondDeleteOnSoftDeletedRowReturns404() {
        String id = createUser();
        speedyClient.delete("User").key("id", id).execute().expectOk();

        // The soft-deleted row is "not found" for a subsequent delete.
        speedyClient.delete("User").key("id", id).execute().expectNotFound();
    }

    @Test
    void onlyDeletedShowsRecycleBinView() {
        String id = createUser();
        speedyClient.delete("User").key("id", id).execute().expectOk();

        speedyClient.get("User").key("id", id).onlyDeleted().execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", is(1))
                .expectJsonPath("$.payload[0].id", is(id))
                .expectJsonPath("$.payload[0].deletedAt", not(nullValue()));
    }

    @Test
    void includeDeletedShowsLiveAndDeleted() {
        String id = createUser();
        speedyClient.delete("User").key("id", id).execute().expectOk();

        speedyClient.get("User").key("id", id).includeDeleted().execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", is(1))
                .expectJsonPath("$.payload[0].id", is(id));
    }

    @Test
    void queryWithDeletedOnlyWorksViaBody() {
        String id = createUser();
        speedyClient.delete("User").key("id", id).execute().expectOk();

        speedyClient.query("User")
                .where(condition("id", eq(id)))
                .onlyDeleted()
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", is(1))
                .expectJsonPath("$.payload[0].id", is(id));
    }

    @Test
    void restoreBringsRowBack() {
        String id = createUser();
        speedyClient.delete("User").key("id", id).execute().expectOk();

        // Hidden by default before restore.
        speedyClient.get("User").key("id", id).execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", is(0));

        speedyClient.restore("User").key("id", id).execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", is(1));

        // Visible again with the marker cleared.
        speedyClient.get("User").key("id", id).execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", is(1))
                .expectJsonPath("$.payload[0].id", is(id))
                .expectJsonPath("$.payload[0].deletedAt", nullValue());
    }

    @Test
    void purgeRemovesRowPermanently() {
        String id = createUser();
        speedyClient.delete("User").key("id", id).execute().expectOk();
        assertTrue(rowExists(id), "row should still exist after soft delete");

        speedyClient.purge("User").key("id", id).execute().expectOk();

        assertFalse(rowExists(id), "row should be permanently removed after purge");
    }

    @Test
    void restoreOnNonSoftDeletedRowReturns404() {
        String id = createUser();
        // Row is live (never deleted); restore has nothing to un-delete.
        speedyClient.restore("User").key("id", id).execute().expectNotFound();
    }

    @Test
    void viewDeletedIsForbiddenWhenEntityDoesNotAllowIt() {
        // SoftDeleteRestrictedEntity has allowViewDeleted = false.
        speedyClient.get("SoftDeleteRestrictedEntity").includeDeleted().execute()
                .expectStatus(403);
        speedyClient.get("SoftDeleteRestrictedEntity").onlyDeleted().execute()
                .expectStatus(403);
    }

    @Test
    void purgeIsForbiddenWhenEntityDoesNotAllowHardDelete() {
        String id = speedyClient.create("SoftDeleteRestrictedEntity")
                .field("name", "restricted-" + System.nanoTime())
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.purge("SoftDeleteRestrictedEntity").key("id", id).execute()
                .expectStatus(403);
    }

    @Test
    void restoreAndPurgeRejectedForHardDeleteEntity() {
        // Company is a hard-delete entity: soft-delete lifecycle endpoints are not applicable.
        speedyClient.restore("Company").key("id", "does-not-matter").execute()
                .expectBadRequest();
        speedyClient.purge("Company").key("id", "does-not-matter").execute()
                .expectBadRequest();
    }
}
