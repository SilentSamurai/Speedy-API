package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderByTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderByTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void testOrderByLocalDateAscending() throws Exception {
        createEntity(LocalDate.now().minusDays(5), LocalTime.of(10, 0), Instant.now().minusSeconds(5000));
        createEntity(LocalDate.now(), LocalTime.of(12, 0), Instant.now());
        createEntity(LocalDate.now().plusDays(5), LocalTime.of(14, 0), Instant.now().plusSeconds(5000));

        queryOrderByLocalDateAscending();
    }

    @Test
    void testOrderByLocalTimeDescending() throws Exception {
        createEntity(LocalDate.now(), LocalTime.of(10, 0), Instant.now().minusSeconds(5000));
        createEntity(LocalDate.now(), LocalTime.of(12, 0), Instant.now());
        createEntity(LocalDate.now(), LocalTime.of(14, 0), Instant.now().plusSeconds(5000));

        queryOrderByLocalTimeDescending();
    }

    private void createEntity(LocalDate localDate, LocalTime localTime, Instant instantTime) throws Exception {
        speedyClient.create("ValueTestEntity")
                .field("localDateTime", LocalDateTime.of(localDate, localTime).toString())
                .field("localDate", localDate.toString())
                .field("localTime", localTime.toString())
                .field("instantTime", instantTime.toString())
                .field("booleanValue", true)
                .execute()
                .expectOk();
    }

    private void queryOrderByLocalDateAscending() throws Exception {
        SpeedyTestResult queryResponse = speedyClient.query("ValueTestEntity")
                .orderByAsc("localDate")
                .execute()
                .expectOk();

        String responseBody = queryResponse.responseBody();
        int size = MAPPER.readTree(responseBody).at("/payload").size();
        LocalDate previousDate = null;
        for (int i = 0; i < size; i++) {
            LocalDate currentDate = LocalDate.parse(
                    MAPPER.readTree(responseBody).at("/payload/" + i + "/localDate").asText());
            if (previousDate != null) {
                assertTrue(currentDate.isAfter(previousDate) || currentDate.isEqual(previousDate));
            }
            previousDate = currentDate;
        }
    }

    private void queryOrderByLocalTimeDescending() throws Exception {
        SpeedyTestResult queryResponse = speedyClient.query("ValueTestEntity")
                .orderByDesc("localTime")
                .execute()
                .expectOk();

        String responseBody = queryResponse.responseBody();
        int size = MAPPER.readTree(responseBody).at("/payload").size();
        LocalTime previousTime = null;
        for (int i = 0; i < size; i++) {
            LocalTime currentTime = LocalTime.parse(
                    MAPPER.readTree(responseBody).at("/payload/" + i + "/localTime").asText());
            if (previousTime != null) {
                assertTrue(currentTime.isBefore(previousTime) || currentTime.equals(previousTime));
            }
            previousTime = currentTime;
        }
    }
}
