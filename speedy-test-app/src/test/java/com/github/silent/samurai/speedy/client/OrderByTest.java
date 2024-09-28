package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.ApiClient;
import com.github.silent.samurai.speedy.api.client.SpeedyApi;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.api.client.SpeedyRequest;
import com.github.silent.samurai.speedy.api.client.models.*;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderByTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderByTest.class);

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
    void testOrderByLocalDateAscending() throws Exception {
        // Step 1: Create multiple ValueTestEntities with different localDate values
        createEntity(LocalDate.now().minusDays(5), LocalTime.of(10, 0), Instant.now().minusSeconds(5000));
        createEntity(LocalDate.now(), LocalTime.of(12, 0), Instant.now());
        createEntity(LocalDate.now().plusDays(5), LocalTime.of(14, 0), Instant.now().plusSeconds(5000));

        // Step 2: Query and order entities by localDate ascending
        queryOrderByLocalDateAscending();
    }

    @Test
    void testOrderByLocalTimeDescending() throws Exception {
        // Step 1: Create multiple ValueTestEntities with different localTime values
        createEntity(LocalDate.now(), LocalTime.of(10, 0), Instant.now().minusSeconds(5000));
        createEntity(LocalDate.now(), LocalTime.of(12, 0), Instant.now());
        createEntity(LocalDate.now(), LocalTime.of(14, 0), Instant.now().plusSeconds(5000));

        // Step 2: Query and order entities by localTime descending
        queryOrderByLocalTimeDescending();
    }

    private void createEntity(LocalDate localDate, LocalTime localTime, Instant instantTime) throws Exception {
        SpeedyCreateRequest createRequest = SpeedyCreateRequest.builder("ValueTestEntity")
                .addField("localDateTime", LocalDateTime.of(localDate, localTime).toString())
                .addField("localDate", localDate.toString())
                .addField("localTime", localTime.toString())
                .addField("instantTime", instantTime.toString())
                .addField("booleanValue", true)
                .build();

        SpeedyResponse createResponse = speedyApi.create(createRequest);
        assertFalse(createResponse.getPayload().isEmpty());
    }

    private void queryOrderByLocalDateAscending() throws Exception {
        SpeedyQuery query = SpeedyRequest
                .query("ValueTestEntity")
                .$orderByAsc("localDate");

        SpeedyResponse queryResponse = speedyApi.query(query);

        assertFalse(queryResponse.getPayload().isEmpty());

        LocalDate previousDate = null;
        for (JsonNode entity : queryResponse.getPayload()) {
            LocalDate currentDate = LocalDate.parse(entity.get("localDate").asText());
            if (previousDate != null) {
                assertTrue(currentDate.isAfter(previousDate) || currentDate.isEqual(previousDate));
            }
            previousDate = currentDate;
        }
    }

    private void queryOrderByLocalTimeDescending() throws Exception {
        SpeedyQuery query = SpeedyRequest
                .query("ValueTestEntity")
                .$orderByDesc("localTime");  // false means descending order

        SpeedyResponse queryResponse = speedyApi.query(query);

        assertFalse(queryResponse.getPayload().isEmpty());

        LocalTime previousTime = null;
        for (JsonNode entity : queryResponse.getPayload()) {
            LocalTime currentTime = LocalTime.parse(entity.get("localTime").asText());
            if (previousTime != null) {
                assertTrue(currentTime.isBefore(previousTime) || currentTime.equals(previousTime));
            }
            previousTime = currentTime;
        }
    }
}
