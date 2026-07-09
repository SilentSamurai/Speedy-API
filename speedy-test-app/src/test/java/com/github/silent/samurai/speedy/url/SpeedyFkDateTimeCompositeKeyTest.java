package com.github.silent.samurai.speedy.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.repositories.PriceHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Composite-key coverage for the *mixed FK + temporal* shape — an identifying foreign key plus an
/// effective-dating timestamp, the most common real-world composite key. Keys
/// {@link com.github.silent.samurai.speedy.entity.PriceHistory} on {@code productId} (VARCHAR FK →
/// {@code Product}) + {@code effectiveAt} (TIMESTAMP / {@code LocalDateTime}). This exercises what
/// neither {@code Order} (two String FKs, no temporal) nor {@code TypedCompositeKeyEntity} (plain
/// scalars, no FK) covers: FK-resolution and {@code TIMESTAMP}-column coercion in the *same* key.
/// <p>
/// The timestamp key rides through GET-by-key as a URL query param — the one path that parses a key
/// literal from the URL, which requires the strict ISO-8601 {@code T}-separated form
/// ({@code 2025-03-15T10:30:00}); {@code $query}/{@code $update}/{@code $delete} carry the key in a
/// (robust) request body. Driven by the {@link SpeedyTest} MockMvc facade. {@code product} rows 1–7
/// are seeded; {@link #cleanup()} clears created rows between tests.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyFkDateTimeCompositeKeyTest {

    private static final String ENTITY = "PriceHistory";
    /// ISO_LOCAL_DATE_TIME — the T-separated form GET-by-key requires in the URL.
    private static final String TS = "2025-03-15T10:30:00";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PriceHistoryRepository repository;

    private SpeedyTest speedy;

    @BeforeEach
    void setUp() {
        speedy = SpeedyTest.mockMvc(mvc);
    }

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    private SpeedyTestResult create(String productId, String effectiveAt, double price, String note) {
        return speedy.create(ENTITY)
                .field("productId", productId)
                .field("effectiveAt", effectiveAt)
                .field("price", price)
                .field("note", note)
                .execute()
                .expectOk();
    }

    @Test
    void create_thenGetByFkDateTimeKey_roundTrips() {
        create("1", TS, 9.99, "launch");

        SpeedyTestResult got = speedy.get(ENTITY)
                .key("productId", "1")
                .key("effectiveAt", TS)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1))
                .expectJsonPath("$.payload[0].productId", equalTo("1"))
                .expectJsonPath("$.payload[0].price", equalTo(9.99))
                .expectJsonPath("$.payload[0].note", equalTo("launch"));
        assertSameDateTime(TS, got.jsonPath("$.payload[0].effectiveAt"));
    }

    @Test
    void query_byBothKeyColumns_returnsRow() {
        create("1", TS, 9.99, "launch");

        SpeedyTestResult result = speedy.query(ENTITY)
                .where(
                        condition("productId", eq("1")),
                        condition("effectiveAt", eq(TS))
                )
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1));
        assertSameDateTime(TS, result.jsonPath("$.payload[0].effectiveAt"));
    }

    /// Filter on the FK key column alone — returns every effective-dated row for that product.
    @Test
    void query_byFkKeyColumnOnly_returnsMatches() {
        create("2", "2025-01-01T00:00:00", 1.0, "jan");
        create("2", "2025-06-01T12:00:00", 2.0, "jun");

        speedy.query(ENTITY)
                .where(condition("productId", eq("2")))
                .pageSize(100)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(2))
                .expectJsonPath("$.payload[*].note", hasItems("jan", "jun"));
    }

    @Test
    void patch_updatesNonKeyField_byFkDateTimeKey() {
        create("3", TS, 5.0, "orig");

        speedy.update(ENTITY)
                .key("productId", "3")
                .key("effectiveAt", TS)
                .field("price", 7.5)
                .execute()
                .expectOk();

        getByKey("3", TS)
                .expectJsonPath("$.payload[0].price", equalTo(7.5))
                .expectJsonPath("$.payload[0].note", equalTo("orig")); // PATCH leaves omitted note untouched
    }

    @Test
    void replace_fullReplace_nullsOmittedNote_byFkDateTimeKey() {
        create("4", TS, 5.0, "will-be-nulled");

        // PUT carries the key + price; the omitted nullable note is reset to null.
        speedy.replace(ENTITY)
                .key("productId", "4")
                .key("effectiveAt", TS)
                .field("price", 3.0)
                .execute()
                .expectOk();

        SpeedyTestResult got = getByKey("4", TS)
                .expectJsonPath("$.payload[0].price", equalTo(3.0));
        JsonNode note = firstRow(got).path("note");
        assertTrue(note.isMissingNode() || note.isNull(), "PUT must reset the omitted note to null");
    }

    @Test
    void delete_byFkDateTimeKey_removesRow() {
        create("5", TS, 5.0, "doomed");

        speedy.delete(ENTITY)
                .key("productId", "5")
                .key("effectiveAt", TS)
                .execute()
                .expectOk();

        speedy.get(ENTITY)
                .key("productId", "5")
                .key("effectiveAt", TS)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(0));
    }

    @Test
    void getByKey_nonExistentFkDateTimeKey_returnsEmpty() {
        speedy.get(ENTITY)
                .key("productId", "6")
                .key("effectiveAt", "1999-12-31T23:59:59")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(0));
    }

    /// A PATCH whose body omits the timestamp key column is an incomplete composite key → 400.
    @Test
    void update_incompleteKey_returns400() {
        create("7", TS, 5.0, "orig");

        speedy.update(ENTITY)
                .key("productId", "7") // effectiveAt deliberately omitted
                .field("price", 1.0)
                .execute()
                .expectBadRequest();
    }

    /// Characterises the one sharp edge: GET-by-key parses the timestamp from the *URL*, and that
    /// parser requires the strict ISO-8601 {@code T}-separated form. A space-separated timestamp for
    /// the *same instant* therefore cannot address the row — it is either rejected (400) or matches
    /// nothing. (Writes are unaffected: they carry the key in a request body, not the URL.)
    @Test
    void getByKey_spaceSeparatedTimestamp_doesNotRoundTrip() {
        create("1", TS, 9.99, "launch");

        boolean matched;
        try {
            SpeedyTestResult result = speedy.get(ENTITY)
                    .key("productId", "1")
                    .key("effectiveAt", "2025-03-15 10:30:00") // same instant, space instead of 'T'
                    .execute();
            Integer len = result.jsonPath("$.payload.length()", Integer.class);
            matched = len != null && len > 0;
        } catch (RuntimeException rejected) {
            matched = false; // rejected outright (400 / bad URL) — also "does not round-trip"
        }
        assertFalse(matched, "a space-separated timestamp must not address the ISO-T-keyed row");
    }

    /* -------------------------------------------------------------------- */
    /* Helpers                                                              */
    /* -------------------------------------------------------------------- */

    private SpeedyTestResult getByKey(String productId, String effectiveAt) {
        return speedy.get(ENTITY)
                .key("productId", productId)
                .key("effectiveAt", effectiveAt)
                .execute()
                .expectOk();
    }

    private JsonNode firstRow(SpeedyTestResult result) {
        try {
            return new ObjectMapper().readTree(result.responseBody()).path("payload").path(0);
        } catch (Exception e) {
            throw new IllegalStateException("Unparseable response body", e);
        }
    }

    /// Compares two ISO-8601 datetimes by value, tolerating trailing-zero formatting differences
    /// (e.g. {@code ...:00} vs {@code ...:00.000}) between what we sent and what the server echoes.
    private void assertSameDateTime(String expected, String actual) {
        assertFalse(actual == null || actual.isEmpty(), "effectiveAt missing from response");
        assertEquals(LocalDateTime.parse(expected), LocalDateTime.parse(actual));
    }
}
