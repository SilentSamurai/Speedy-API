package com.github.silent.samurai.speedy.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.repositories.TypedCompositeKeyEntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.in;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Composite-key coverage for *non-String, non-FK* key columns. The other composite-key entities
/// key on String columns — {@code Order} on two String FKs, and {@code PriceHistory} on an FK plus a
/// timestamp — so numeric key handling was untested until this entity.
/// {@link com.github.silent.samurai.speedy.entity.TypedCompositeKeyEntity} keys on
/// {@code numId} (Long/BIGINT) + {@code effectiveDate} (LocalDate/DATE) + {@code code}
/// (String/VARCHAR), all plain scalars. These tests round-trip that key through GET-by-key (typed
/// values as query params), {@code $query} filters (incl. {@code $in} on the numeric column),
/// PATCH/PUT and DELETE, plus the negative paths (incomplete key, non-existent key) — driven by the
/// {@link SpeedyTest} MockMvc facade. Each test owns a distinct {@code numId}; {@link #cleanup()}
/// clears the table between tests since the H2 context is shared.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyTypedCompositeKeyTest {

    private static final String ENTITY = "TypedCompositeKeyEntity";
    private static final String DATE = "2025-03-15";
    private static final String CODE = "ABC-1";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TypedCompositeKeyEntityRepository repository;

    private SpeedyTest speedy;

    @BeforeEach
    void setUp() {
        speedy = SpeedyTest.mockMvc(mvc);
    }

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    private SpeedyTestResult create(long numId, String date, String code) {
        return speedy.create(ENTITY)
                .field("numId", numId)
                .field("effectiveDate", date)
                .field("code", code)
                .field("quantity", 5)
                .field("amount", 12.5)
                .field("active", true)
                .field("description", "seed")
                .execute()
                .expectOk();
    }

    @Test
    void create_thenGetByTypedCompositeKey_roundTrips() {
        create(100L, DATE, CODE);

        speedy.get(ENTITY)
                .key("numId", 100L)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1))
                .expectJsonPath("$.payload[0].numId", equalTo(100))
                .expectJsonPath("$.payload[0].effectiveDate", equalTo(DATE))
                .expectJsonPath("$.payload[0].code", equalTo(CODE))
                .expectJsonPath("$.payload[0].quantity", equalTo(5))
                .expectJsonPath("$.payload[0].amount", equalTo(12.5))
                .expectJsonPath("$.payload[0].active", equalTo(true));
    }

    @Test
    void query_byAllTypedKeyColumns_returnsRow() {
        create(200L, DATE, CODE);

        speedy.query(ENTITY)
                .where(
                        condition("numId", eq(200L)),
                        condition("effectiveDate", eq(DATE)),
                        condition("code", eq(CODE))
                )
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1))
                .expectJsonPath("$.payload[0].numId", equalTo(200));
    }

    @Test
    void query_byNumericKeyColumnOnly_returnsMatches() {
        create(300L, DATE, "X-1");
        create(300L, DATE, "X-2");

        speedy.query(ENTITY)
                .where(condition("numId", eq(300L)))
                .pageSize(100)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[*].code", hasItems("X-1", "X-2"))
                .expectJsonPath("$.payload[*].numId", everyItem(equalTo(300)));
    }

    /// {@code $in} on the *numeric* key column — the numeric analogue of the String-key {@code $in}
    /// coverage on {@code Order}. A non-matching value in the set is simply ignored.
    @Test
    void query_withInOnNumericKeyColumn_returnsMatches() {
        create(700L, DATE, "IN-A");
        create(700L, DATE, "IN-B");

        speedy.query(ENTITY)
                .where(condition("numId", in(700L, 701L))) // 701 never created
                .pageSize(100)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[*].code", hasItems("IN-A", "IN-B"))
                .expectJsonPath("$.payload[*].numId", everyItem(equalTo(700)));
    }

    @Test
    void patch_updatesNonKeyField_byTypedKey() {
        create(400L, DATE, CODE);

        speedy.update(ENTITY)
                .key("numId", 400L)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .field("quantity", 99)
                .execute()
                .expectOk();

        getByKey(400L, DATE, CODE)
                .expectJsonPath("$.payload[0].quantity", equalTo(99))
                .expectJsonPath("$.payload[0].amount", equalTo(12.5)); // PATCH leaves omitted amount untouched
    }

    @Test
    void replace_fullReplace_nullsOmittedField_byTypedKey() {
        create(500L, DATE, CODE);

        // PUT carries the keys + a subset of fields; the omitted nullable description is reset to null.
        speedy.replace(ENTITY)
                .key("numId", 500L)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .field("quantity", 7)
                .field("amount", 1.0)
                .field("active", false)
                .execute()
                .expectOk();

        SpeedyTestResult got = getByKey(500L, DATE, CODE)
                .expectJsonPath("$.payload[0].quantity", equalTo(7))
                .expectJsonPath("$.payload[0].active", equalTo(false));
        JsonNode description = firstRow(got).path("description");
        assertTrue(description.isMissingNode() || description.isNull(),
                "PUT must reset the omitted description to null");
    }

    @Test
    void delete_byTypedCompositeKey_removesRow() {
        create(600L, DATE, CODE);

        speedy.delete(ENTITY)
                .key("numId", 600L)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .execute()
                .expectOk();

        speedy.get(ENTITY)
                .key("numId", 600L)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(0));
    }

    @Test
    void getByKey_nonExistentTypedKey_returnsEmpty() {
        speedy.get(ENTITY)
                .key("numId", 999L)
                .key("effectiveDate", DATE)
                .key("code", "NOPE")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(0));
    }

    /* -------------------------------------------------------------------- */
    /* Negative — addressing an incomplete or non-existent composite key    */
    /* -------------------------------------------------------------------- */

    /// A PATCH ({@code $update}) whose body omits a key column is an incomplete composite key and
    /// must be rejected with 400 — mirrors the String-key {@code Order} behaviour.
    @Test
    void update_incompleteTypedKey_returns400() {
        create(800L, DATE, CODE);

        speedy.update(ENTITY)
                .key("numId", 800L) // effectiveDate + code deliberately omitted
                .field("quantity", 1)
                .execute()
                .expectBadRequest();
    }

    /// Updating a well-formed but non-existent composite key addresses a specific row → 404.
    @Test
    void update_nonExistentTypedKey_returns404() {
        speedy.update(ENTITY)
                .key("numId", 901L)
                .key("effectiveDate", DATE)
                .key("code", "GHOST")
                .field("quantity", 1)
                .execute()
                .expectNotFound();
    }

    /// Deleting a well-formed but non-existent composite key → 404 (unlike GET-by-key, which is
    /// empty). Same distinction the String-key {@code Order} tests draw.
    @Test
    void delete_nonExistentTypedKey_returns404() {
        speedy.delete(ENTITY)
                .key("numId", 902L)
                .key("effectiveDate", DATE)
                .key("code", "GHOST")
                .execute()
                .expectNotFound();
    }

    /* -------------------------------------------------------------------- */
    /* Helpers                                                              */
    /* -------------------------------------------------------------------- */

    private SpeedyTestResult getByKey(long numId, String date, String code) {
        return speedy.get(ENTITY)
                .key("numId", numId)
                .key("effectiveDate", date)
                .key("code", code)
                .execute()
                .expectOk();
    }

    /// First payload row as a JsonNode, for the null/absent assertions MockMvc JSONPath handles
    /// awkwardly ({@code path()} yields a MissingNode when a field is absent, NullNode when null).
    private JsonNode firstRow(SpeedyTestResult result) {
        try {
            return JSON.readTree(result.responseBody()).path("payload").path(0);
        } catch (Exception e) {
            throw new IllegalStateException("Unparseable response body", e);
        }
    }
}
