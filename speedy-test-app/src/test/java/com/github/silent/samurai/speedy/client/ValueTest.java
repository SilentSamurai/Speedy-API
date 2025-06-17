package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import com.github.silent.samurai.speedy.entity.ValueTestEntity;
import com.github.silent.samurai.speedy.repositories.ValueTestRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.time.*;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ValueTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValueTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    ValueTestRepository valueTestRepository;
    SpeedyClient<SpeedyResponse> speedyClient;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        speedyClient = SpeedyClient.restTemplate(restTemplate, "http://localhost");
    }

    @Test
    void normalTest() throws Exception {
        Class<ValueTestEntity> valueTestEntityClass = ValueTestEntity.class;

        SpeedyResponse createResponse = speedyClient.create("ValueTestEntity")
                .addField("localDateTime", LocalDateTime.now())
                .addField("localDate", LocalDate.now())
                .addField("localTime", LocalTime.now())
                .addField("instantTime", Instant.now())
                .addField("zonedDateTime", ZonedDateTime.now())
                .addField("booleanValue", true)
                .execute();

        assertFalse(createResponse.getPayload().isEmpty());
        JsonNode createdEntity = createResponse.getPayload().get(0);

        assertTrue(createdEntity.has("id"));
        String id = createdEntity.get("id").asText();
        assertNotNull(id);

        // Step 2: Get and verify the created entity
        SpeedyResponse getResponse = speedyClient.get("ValueTestEntity")
                .key("id", id)
                .execute();

        JsonNode fetchedEntity = getResponse.getPayload().get(0);

        assertTrue(fetchedEntity.has("localDateTime"));
        assertTrue(fetchedEntity.has("booleanValue"));
        assertTrue(fetchedEntity.get("booleanValue").asBoolean());

        // Step 3: Update the entity
        SpeedyResponse updateResponse = speedyClient.update("ValueTestEntity")
                .key("id", id)
                .field("booleanValue", false)  // Update the boolean field
                .field("localDateTime", LocalDateTime.now().plusDays(1).toString()) // Update the date
                .execute();

        JsonNode updatedEntity = updateResponse.getPayload();

        assertTrue(updatedEntity.isArray());
        assertFalse(updatedEntity.isEmpty());
        updatedEntity = updatedEntity.get(0);

        assertTrue(updatedEntity.has("booleanValue"));
        assertFalse(updatedEntity.get("booleanValue").asBoolean());

        // Step 4: Query the entity by ID and verify the updated value
        SpeedyResponse queryResponse = speedyClient.query(
                        SpeedyQuery.from("ValueTestEntity")
                                .where(condition("id", eq(id)))
                )
                .execute();

        JsonNode queriedEntity = queryResponse.getPayload().get(0);

        assertTrue(queriedEntity.has("booleanValue"));
        assertFalse(queriedEntity.get("booleanValue").asBoolean());
        assertTrue(queriedEntity.has("localDateTime"));

        // Step 5: Delete the entity
        SpeedyResponse deleteResponse = speedyClient.delete("ValueTestEntity")
                .key("id", id)
                .execute();

        JsonNode deletedEntity = deleteResponse.getPayload().get(0);

        assertTrue(deletedEntity.has("id"));
        assertEquals(id, deletedEntity.get("id").asText());
    }

    @Test
    void testQueryWithGreaterThanAndLessThan() throws Exception {
        // Step 1: Create multiple ValueTestEntities
        createEntity(LocalDate.now().minusDays(5), LocalTime.of(10, 0), Instant.now().minusSeconds(5000));
        createEntity(LocalDate.now(), LocalTime.of(12, 0), Instant.now());
        createEntity(LocalDate.now().plusDays(5), LocalTime.of(14, 0), Instant.now().plusSeconds(5000));

        // Step 2: Query entities where localDate > today
        queryLocalDateGreaterThan(LocalDate.now());

        // Step 3: Query entities where localDate < today
        queryLocalDateLessThan(LocalDate.now());

        // Step 4: Query entities where localTime > 11:00
        queryLocalTimeGreaterThan(LocalTime.of(11, 0));

        // Step 5: Query entities where instantTime < now
        queryInstantTimeLessThan(Instant.now());

        query_double_value(1.5430434);
        query_double_value(0.000023);
        query_double_value(0.4545);
        query_double_value(909999.094);
        query_double_value(909999.5);
    }

    private void createEntity(LocalDate localDate, LocalTime localTime, Instant instantTime) throws Exception {
        SpeedyResponse createResponse = speedyClient.create("ValueTestEntity")
                .addField("localDateTime", LocalDateTime.of(localDate, localTime))
                .addField("localDate", localDate)
                .addField("localTime", localTime)
                .addField("instantTime", instantTime)
                .addField("booleanValue", true)
                .addField("doubleValue", 1.5430434)
                .execute();

        assertFalse(createResponse.getPayload().isEmpty());
    }

    private void queryLocalDateGreaterThan(LocalDate date) throws Exception {
        SpeedyResponse queryResponse = speedyClient.query(
                        SpeedyQuery.from("ValueTestEntity")
                                .where(condition("localDate", gt(date.toString())))
                )
                .execute();

        assertFalse(queryResponse.getPayload().isEmpty());
        for (JsonNode entity : queryResponse.getPayload()) {
            assertTrue(LocalDate.parse(entity.get("localDate").asText()).isAfter(date));
        }
    }

    private void queryLocalDateLessThan(LocalDate date) throws Exception {
        SpeedyResponse queryResponse = speedyClient.query(
                        SpeedyQuery.from("ValueTestEntity")
                                .where(condition("localDate", lt(date.toString())))
                )
                .execute();

        assertFalse(queryResponse.getPayload().isEmpty());
        for (JsonNode entity : queryResponse.getPayload()) {
            assertTrue(LocalDate.parse(entity.get("localDate").asText()).isBefore(date));
        }
    }

    private void queryLocalTimeGreaterThan(LocalTime time) throws Exception {
        SpeedyResponse queryResponse = speedyClient.query(
                        SpeedyQuery.from("ValueTestEntity")
                                .where(condition("localTime", gt(time.toString())))
                )
                .execute();

        assertFalse(queryResponse.getPayload().isEmpty());
        for (JsonNode entity : queryResponse.getPayload()) {
            assertTrue(LocalTime.parse(entity.get("localTime").asText()).isAfter(time));
        }
    }

    private void queryInstantTimeLessThan(Instant instant) throws Exception {
        SpeedyResponse queryResponse = speedyClient.query(
                        SpeedyQuery.from("ValueTestEntity")
                                .where(condition("instantTime", lt(instant.toString())))
                )
                .execute();

        assertFalse(queryResponse.getPayload().isEmpty());
        for (JsonNode entity : queryResponse.getPayload()) {
            assertTrue(Instant.parse(entity.get("instantTime").asText()).isBefore(instant));
        }
    }

    private void query_double_value(Double doubleValue) throws Exception {
        SpeedyResponse createResponse = speedyClient.create("ValueTestEntity")
                .addField("localDateTime", LocalDateTime.now())
                .addField("localDate", LocalDate.now())
                .addField("localTime", LocalTime.now())
                .addField("instantTime", Instant.now())
                .addField("booleanValue", true)
                .addField("doubleValue", doubleValue)
                .execute();

        assertFalse(createResponse.getPayload().isEmpty());
        JsonNode payload = createResponse.getPayload();

        SpeedyResponse queryResponse = speedyClient.query(
                        SpeedyQuery.from("ValueTestEntity")
                                .where(condition("doubleValue", eq(doubleValue)))
                )
                .execute();

        assertFalse(queryResponse.getPayload().isEmpty());
        for (JsonNode entity : queryResponse.getPayload()) {
            assertEquals(entity.get("doubleValue").asDouble(), doubleValue);
        }
    }
}
