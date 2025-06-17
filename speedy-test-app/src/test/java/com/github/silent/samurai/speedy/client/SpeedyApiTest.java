package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
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

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.eq;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyApiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyApiTest.class);

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

    String createTest() throws Exception {
        SpeedyResponse speedyResponse = speedyClient.create("Category")
                .addField("name", "cat-client-1")
                .execute();

        assertFalse(speedyResponse.getPayload().isEmpty());
        JsonNode entityNode = speedyResponse.getPayload().get(0);

        assertTrue(entityNode.has("id"));
        assertTrue(entityNode.get("id").isTextual());
        assertNotNull(entityNode.get("id").asText());

        return entityNode.get("id").asText();
    }

    void updateTest(String id) throws Exception {
        SpeedyResponse speedyResponse = speedyClient.update("Category")
                .key("id", id)
                .field("name", "cat-CLIENT-updated-1")
                .execute();

        assertFalse(speedyResponse.getPayload().isEmpty());
        JsonNode entityNode = speedyResponse.getPayload();

        assertTrue(entityNode.isArray());
        assertFalse(entityNode.isEmpty());
        entityNode = entityNode.get(0);
        assertTrue(entityNode.has("id"));
        assertTrue(entityNode.get("id").isTextual());
        assertNotNull(entityNode.get("id").asText());

        assertTrue(entityNode.has("name"));
        assertEquals("cat-CLIENT-updated-1", entityNode.get("name").asText());
    }

    void deleteTest(String id) throws Exception {
        SpeedyResponse speedyResponse = speedyClient.delete("Category")
                .key("id", id)
                .execute();

        assertFalse(speedyResponse.getPayload().isEmpty());
        JsonNode entityNode = speedyResponse.getPayload().get(0);

        assertTrue(entityNode.has("id"));
        assertTrue(entityNode.get("id").isTextual());
        assertNotNull(entityNode.get("id").asText());
    }

    void getTest(String id, String name) throws Exception {
        SpeedyResponse speedyResponse = speedyClient.get("Category")
                .key("id", id)
                .execute();

        assertFalse(speedyResponse.getPayload().isEmpty());
        JsonNode entityNode = speedyResponse.getPayload().get(0);

        assertTrue(entityNode.has("id"));
        assertTrue(entityNode.get("id").isTextual());
        assertNotNull(entityNode.get("id").asText());
        assertEquals(id, entityNode.get("id").asText());

        assertTrue(entityNode.has("name"));
        assertEquals(name, entityNode.get("name").asText());
    }

    void query(String id, String name) throws Exception {
        SpeedyResponse speedyResponse = speedyClient.query(
                        SpeedyQuery.from("Category")
                                .where(condition("name", eq(name)))
                )
                .execute();

        assertFalse(speedyResponse.getPayload().isEmpty());
        JsonNode entityNode = speedyResponse.getPayload().get(0);

        assertTrue(entityNode.has("id"));
        assertTrue(entityNode.get("id").isTextual());
        assertNotNull(entityNode.get("id").asText());
        assertEquals(id, entityNode.get("id").asText());

        assertTrue(entityNode.has("name"));
        assertEquals(name, entityNode.get("name").asText());
    }

    @Test
    void normalTest() throws Exception {
        String id = createTest();

        getTest(id, "cat-client-1");

        updateTest(id);

        getTest(id, "cat-CLIENT-updated-1");

        query(id, "cat-CLIENT-updated-1");

        deleteTest(id);
    }
}

