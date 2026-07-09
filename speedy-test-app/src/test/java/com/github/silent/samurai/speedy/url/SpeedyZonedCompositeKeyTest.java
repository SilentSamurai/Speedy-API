package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.repositories.SensorReadingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Composite-key coverage for a *zoned* temporal key column — {@code Instant}, which Speedy maps to
/// {@code TIMESTAMP_WITH_ZONE} / {@code ZONED_DATE_TIME}, a different codec + URL-parse path than the
/// zone-less {@code LocalDateTime} key in {@code SpeedyFkDateTimeCompositeKeyTest}.
/// {@link com.github.silent.samurai.speedy.entity.SensorReading} keys on {@code sensorId} (VARCHAR)
/// + {@code recordedAt} ({@code Instant}). Because a zoned value is parsed from the GET-by-key URL
/// via {@code ZonedDateTime.parse}, the {@code Z} (UTC) form round-trips, whereas a numeric
/// {@code +00:00} offset does not (the URL layer turns {@code +} into a space). Driven by the
/// {@link SpeedyTest} MockMvc facade; {@link #cleanup()} clears created rows between tests.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyZonedCompositeKeyTest {

    private static final String ENTITY = "SensorReading";
    /// UTC instant in the {@code Z} form the GET-by-key URL parser accepts.
    private static final String TS_Z = "2025-03-15T10:30:00Z";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SensorReadingRepository repository;

    private SpeedyTest speedy;

    @BeforeEach
    void setUp() {
        speedy = SpeedyTest.mockMvc(mvc);
    }

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    private SpeedyTestResult create(String sensorId, String recordedAt, double temp, String label) {
        return speedy.create(ENTITY)
                .field("sensorId", sensorId)
                .field("recordedAt", recordedAt)
                .field("temperature", temp)
                .field("label", label)
                .execute()
                .expectOk();
    }

    @Test
    void create_thenGetByZonedKey_roundTrips() {
        create("S1", TS_Z, 21.5, "ok");

        SpeedyTestResult got = speedy.get(ENTITY)
                .key("sensorId", "S1")
                .key("recordedAt", TS_Z)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1))
                .expectJsonPath("$.payload[0].sensorId", equalTo("S1"))
                .expectJsonPath("$.payload[0].temperature", equalTo(21.5));
        assertSameInstant(TS_Z, got.jsonPath("$.payload[0].recordedAt"));
    }

    @Test
    void query_byBothKeyColumns_returnsRow() {
        create("S2", TS_Z, 21.5, "ok");

        SpeedyTestResult result = speedy.query(ENTITY)
                .where(
                        condition("sensorId", eq("S2")),
                        condition("recordedAt", eq(TS_Z))
                )
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1));
        assertSameInstant(TS_Z, result.jsonPath("$.payload[0].recordedAt"));
    }

    @Test
    void patch_updatesNonKeyField_byZonedKey() {
        create("S3", TS_Z, 21.5, "orig");

        speedy.update(ENTITY)
                .key("sensorId", "S3")
                .key("recordedAt", TS_Z)
                .field("temperature", 30.0)
                .execute()
                .expectOk();

        getByKey("S3", TS_Z)
                .expectJsonPath("$.payload[0].temperature", equalTo(30.0))
                .expectJsonPath("$.payload[0].label", equalTo("orig")); // PATCH leaves omitted label untouched
    }

    @Test
    void delete_byZonedKey_removesRow() {
        create("S4", TS_Z, 21.5, "doomed");

        speedy.delete(ENTITY)
                .key("sensorId", "S4")
                .key("recordedAt", TS_Z)
                .execute()
                .expectOk();

        speedy.get(ENTITY)
                .key("sensorId", "S4")
                .key("recordedAt", TS_Z)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(0));
    }

    @Test
    void getByKey_nonExistentZonedKey_returnsEmpty() {
        speedy.get(ENTITY)
                .key("sensorId", "S5")
                .key("recordedAt", "1999-12-31T23:59:59Z")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(0));
    }

    /// A PATCH whose body omits the zoned-timestamp key column is an incomplete composite key → 400.
    @Test
    void update_incompleteKey_returns400() {
        create("S6", TS_Z, 21.5, "orig");

        speedy.update(ENTITY)
                .key("sensorId", "S6") // recordedAt deliberately omitted
                .field("temperature", 1.0)
                .execute()
                .expectBadRequest();
    }

    /// Characterises the zoned URL edge: the same instant written as a numeric {@code +00:00} offset
    /// instead of {@code Z} cannot address the row via GET-by-key — the URL layer decodes {@code +}
    /// to a space which the key parser strips, so {@code ZonedDateTime.parse} fails → 400. Writes are
    /// unaffected (key in body).
    @Test
    void getByKey_numericOffsetInsteadOfZ_isRejected() {
        create("S1", TS_Z, 21.5, "ok");

        speedy.get(ENTITY)
                .key("sensorId", "S1")
                .key("recordedAt", "2025-03-15T10:30:00+00:00") // same instant, numeric offset
                .execute()
                .expectBadRequest();
    }

    /* -------------------------------------------------------------------- */
    /* Helpers                                                              */
    /* -------------------------------------------------------------------- */

    private SpeedyTestResult getByKey(String sensorId, String recordedAt) {
        return speedy.get(ENTITY)
                .key("sensorId", sensorId)
                .key("recordedAt", recordedAt)
                .execute()
                .expectOk();
    }

    /// Compares two ISO-8601 instants by value, tolerating {@code Z} vs {@code +00:00} formatting.
    private void assertSameInstant(String expected, String actual) {
        assertFalse(actual == null || actual.isEmpty(), "recordedAt missing from response");
        assertEquals(toInstant(expected), toInstant(actual));
    }

    private Instant toInstant(String s) {
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException notZulu) {
            return OffsetDateTime.parse(s).toInstant();
        }
    }
}
