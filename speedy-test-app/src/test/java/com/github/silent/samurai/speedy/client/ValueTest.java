package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.ApiClient;
import com.github.silent.samurai.speedy.api.client.SpeedyApi;
import com.github.silent.samurai.speedy.api.client.SpeedyRequest;
import com.github.silent.samurai.speedy.api.client.models.*;
import com.github.silent.samurai.speedy.entity.ValueTestEntity;
import com.github.silent.samurai.speedy.repositories.ValueTestRepository;
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

import javax.persistence.EntityManagerFactory;

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
    ApiClient defaultClient;
    SpeedyApi speedyApi;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
        speedyApi = new SpeedyApi(defaultClient);
    }

    @Test
    void normalTest() throws Exception {

        Class<ValueTestEntity> valueTestEntityClass = ValueTestEntity.class;

        SpeedyCreateRequest createRequest = SpeedyRequest.create("ValueTestEntity")
                .addField("localDateTime", LocalDateTime.now())
                .addField("localDate", LocalDate.now())
                .addField("localTime", LocalTime.now())
                .addField("instantTime", Instant.now())
                .addField("zonedDateTime", ZonedDateTime.now())
                .addField("booleanValue", true)
                .build();

        SpeedyResponse createResponse = speedyApi.create(createRequest);

        assertFalse(createResponse.getPayload().isEmpty());
        JsonNode createdEntity = createResponse.getPayload().get(0);

        assertTrue(createdEntity.has("id"));
        String id = createdEntity.get("id").asText();
        assertNotNull(id);

        // Step 2: Get and verify the created entity
        SpeedyGetRequest getRequest = SpeedyRequest
                .get("ValueTestEntity")
                .key("id", id)
                .build();

        SpeedyResponse getResponse = speedyApi.get(getRequest);
        JsonNode fetchedEntity = getResponse.getPayload().get(0);

        assertTrue(fetchedEntity.has("localDateTime"));
        assertTrue(fetchedEntity.has("booleanValue"));
        assertEquals(true, fetchedEntity.get("booleanValue").asBoolean());

        // Step 3: Update the entity
        SpeedyUpdateRequest updateRequest = SpeedyRequest
                .update("ValueTestEntity")
                .key("id", id)
                .field("booleanValue", false)  // Update the boolean field
                .field("localDateTime", LocalDateTime.now().plusDays(1).toString()) // Update the date
                .build();

        SpeedyResponse updateResponse = speedyApi.update(updateRequest);
        JsonNode updatedEntity = updateResponse.getPayload();

        assertTrue(updatedEntity.has("booleanValue"));
        assertFalse(updatedEntity.get("booleanValue").asBoolean());

        // Step 4: Query the entity by ID and verify the updated value
        SpeedyQuery query = SpeedyRequest
                .query("ValueTestEntity")
                .$where(
                        $condition("id", $eq(id))
                );

        SpeedyResponse queryResponse = speedyApi.query(query);
        JsonNode queriedEntity = queryResponse.getPayload().get(0);

        assertTrue(queriedEntity.has("booleanValue"));
        assertFalse(queriedEntity.get("booleanValue").asBoolean());
        assertTrue(queriedEntity.has("localDateTime"));

        // Step 5: Delete the entity
        SpeedyDeleteRequest deleteRequest = SpeedyRequest.delete("ValueTestEntity")
                .key("id", id)
                .build();

        SpeedyResponse deleteResponse = speedyApi.delete(deleteRequest);
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
    }

    private void createEntity(LocalDate localDate, LocalTime localTime, Instant instantTime) throws Exception {
        SpeedyCreateRequest createRequest = SpeedyCreateRequest.builder("ValueTestEntity")
                .addField("localDateTime", LocalDateTime.of(localDate, localTime))
                .addField("localDate", localDate)
                .addField("localTime", localTime)
                .addField("instantTime", instantTime)
                .addField("booleanValue", true)
                .build();

        SpeedyResponse createResponse = speedyApi.create(createRequest);
        assertFalse(createResponse.getPayload().isEmpty());
    }

    private void queryLocalDateGreaterThan(LocalDate date) throws Exception {
        SpeedyQuery query = SpeedyQuery.builder("ValueTestEntity")
                .$where($condition("localDate", $gt(date.toString())));

        SpeedyResponse queryResponse = speedyApi.query(query);

        assertFalse(queryResponse.getPayload().isEmpty());
        for (JsonNode entity : queryResponse.getPayload()) {
            assertTrue(LocalDate.parse(entity.get("localDate").asText()).isAfter(date));
        }
    }

    private void queryLocalDateLessThan(LocalDate date) throws Exception {
        SpeedyQuery query = SpeedyQuery.builder("ValueTestEntity")
                .$where($condition("localDate", $lt(date.toString())));

        SpeedyResponse queryResponse = speedyApi.query(query);

        assertFalse(queryResponse.getPayload().isEmpty());
        for (JsonNode entity : queryResponse.getPayload()) {
            assertTrue(LocalDate.parse(entity.get("localDate").asText()).isBefore(date));
        }
    }

    private void queryLocalTimeGreaterThan(LocalTime time) throws Exception {
        SpeedyQuery query = SpeedyQuery.builder("ValueTestEntity")
                .$where($condition("localTime", $gt(time.toString())));

        SpeedyResponse queryResponse = speedyApi.query(query);

        assertFalse(queryResponse.getPayload().isEmpty());
        for (JsonNode entity : queryResponse.getPayload()) {
            assertTrue(LocalTime.parse(entity.get("localTime").asText()).isAfter(time));
        }
    }

    private void queryInstantTimeLessThan(Instant instant) throws Exception {
        SpeedyQuery query = SpeedyQuery.builder("ValueTestEntity")
                .$where($condition("instantTime", $lt(instant.toString())));

        SpeedyResponse queryResponse = speedyApi.query(query);

        assertFalse(queryResponse.getPayload().isEmpty());
        for (JsonNode entity : queryResponse.getPayload()) {
            assertTrue(Instant.parse(entity.get("instantTime").asText()).isBefore(instant));
        }
    }
}
