package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ValueTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void normalTest() {
        SpeedyTestResult createResponse = speedyClient.create("ValueTestEntity")
                .field("localDateTime", LocalDateTime.now())
                .field("localDate", LocalDate.now())
                .field("localTime", LocalTime.now())
                .field("instantTime", Instant.now())
                .field("booleanValue", true)
                .execute()
                .expectOk();

        String id = createResponse.jsonPath("$.payload[0].id");
        assertNotNull(id);

        // Step 2: Get and verify the created entity
        SpeedyTestResult getResponse = speedyClient.get("ValueTestEntity")
                .key("id", id)
                .execute()
                .expectOk();

        assertNotNull(getResponse.jsonPath("$.payload[0].localDateTime"));
        boolean boolVal = getResponse.jsonPath("$.payload[0].booleanValue", Boolean.class);
        assertTrue(boolVal);

        // Step 3: Update the entity
        SpeedyTestResult updateResponse = speedyClient.update("ValueTestEntity")
                .key("id", id)
                .field("booleanValue", false)
                .field("localDateTime", LocalDateTime.now().plusDays(1).toString())
                .execute()
                .expectOk();

        boolean updatedBool = updateResponse.jsonPath("$.payload[0].booleanValue", Boolean.class);
        assertFalse(updatedBool);

        // Step 4: Query the entity by ID and verify the updated value
        SpeedyTestResult queryResponse = speedyClient.query("ValueTestEntity")
                .where(condition("id", eq(id)))
                .execute()
                .expectOk();

        boolean queriedBool = queryResponse.jsonPath("$.payload[0].booleanValue", Boolean.class);
        assertFalse(queriedBool);
        assertNotNull(queryResponse.jsonPath("$.payload[0].localDateTime"));

        // Step 5: Delete the entity
        SpeedyTestResult deleteResponse = speedyClient.delete("ValueTestEntity")
                .key("id", id)
                .execute()
                .expectOk();

        assertEquals(id, deleteResponse.jsonPath("$.payload[0].id"));
    }

    @Test
    void testQueryWithGreaterThanAndLessThan() throws Exception {
        createEntity(LocalDate.now().minusDays(5), LocalTime.of(10, 0), Instant.now().minusSeconds(5000));
        createEntity(LocalDate.now(), LocalTime.of(12, 0), Instant.now());
        createEntity(LocalDate.now().plusDays(5), LocalTime.of(14, 0), Instant.now().plusSeconds(5000));

        queryLocalDateGreaterThan(LocalDate.now());
        queryLocalDateLessThan(LocalDate.now());
        queryLocalTimeGreaterThan(LocalTime.of(11, 0));
        queryInstantTimeLessThan(Instant.now());

        query_double_value(1.5430434);
        query_double_value(0.000023);
        query_double_value(0.4545);
        query_double_value(909999.094);
        query_double_value(909999.5);
    }

    private void createEntity(LocalDate localDate, LocalTime localTime, Instant instantTime) {
        speedyClient.create("ValueTestEntity")
                .field("localDateTime", LocalDateTime.of(localDate, localTime))
                .field("localDate", localDate)
                .field("localTime", localTime)
                .field("instantTime", instantTime)
                .field("booleanValue", true)
                .field("doubleValue", 1.5430434)
                .execute()
                .expectOk();
    }

    private void queryLocalDateGreaterThan(LocalDate date) throws Exception {
        SpeedyTestResult queryResponse = speedyClient.query("ValueTestEntity")
                .where(condition("localDate", gt(date.toString())))
                .execute()
                .expectOk();

        String responseBody = queryResponse.responseBody();
        int size = MAPPER.readTree(responseBody).at("/payload").size();
        assertTrue(size > 0);
        for (int i = 0; i < size; i++) {
            String val = MAPPER.readTree(responseBody).at("/payload/" + i + "/localDate").asText();
            assertTrue(LocalDate.parse(val).isAfter(date));
        }
    }

    private void queryLocalDateLessThan(LocalDate date) throws Exception {
        SpeedyTestResult queryResponse = speedyClient.query("ValueTestEntity")
                .where(condition("localDate", lt(date.toString())))
                .execute()
                .expectOk();

        String responseBody = queryResponse.responseBody();
        int size = MAPPER.readTree(responseBody).at("/payload").size();
        assertTrue(size > 0);
        for (int i = 0; i < size; i++) {
            String val = MAPPER.readTree(responseBody).at("/payload/" + i + "/localDate").asText();
            assertTrue(LocalDate.parse(val).isBefore(date));
        }
    }

    private void queryLocalTimeGreaterThan(LocalTime time) throws Exception {
        SpeedyTestResult queryResponse = speedyClient.query("ValueTestEntity")
                .where(condition("localTime", gt(time.toString())))
                .execute()
                .expectOk();

        String responseBody = queryResponse.responseBody();
        int size = MAPPER.readTree(responseBody).at("/payload").size();
        assertTrue(size > 0);
        for (int i = 0; i < size; i++) {
            String val = MAPPER.readTree(responseBody).at("/payload/" + i + "/localTime").asText();
            assertTrue(LocalTime.parse(val).isAfter(time));
        }
    }

    private void queryInstantTimeLessThan(Instant instant) throws Exception {
        SpeedyTestResult queryResponse = speedyClient.query("ValueTestEntity")
                .where(condition("instantTime", lt(instant.toString())))
                .execute()
                .expectOk();

        String responseBody = queryResponse.responseBody();
        int size = MAPPER.readTree(responseBody).at("/payload").size();
        assertTrue(size > 0);
        for (int i = 0; i < size; i++) {
            String val = MAPPER.readTree(responseBody).at("/payload/" + i + "/instantTime").asText();
            assertTrue(Instant.parse(val).isBefore(instant));
        }
    }

    private void query_double_value(Double doubleValue) throws Exception {
        speedyClient.create("ValueTestEntity")
                .field("localDateTime", LocalDateTime.now())
                .field("localDate", LocalDate.now())
                .field("localTime", LocalTime.now())
                .field("instantTime", Instant.now())
                .field("booleanValue", true)
                .field("doubleValue", doubleValue)
                .execute()
                .expectOk();

        SpeedyTestResult queryResponse = speedyClient.query("ValueTestEntity")
                .where(condition("doubleValue", eq(doubleValue)))
                .execute()
                .expectOk();

        String responseBody = queryResponse.responseBody();
        int size = MAPPER.readTree(responseBody).at("/payload").size();
        assertTrue(size > 0);
        for (int i = 0; i < size; i++) {
            double val = MAPPER.readTree(responseBody).at("/payload/" + i + "/doubleValue").asDouble();
            assertEquals(val, doubleValue);
        }
    }
}
