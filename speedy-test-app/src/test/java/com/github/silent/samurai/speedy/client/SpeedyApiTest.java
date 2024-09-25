package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.ApiClient;
import com.github.silent.samurai.speedy.api.client.SpeedyApi;
import com.github.silent.samurai.speedy.models.*;
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
        SpeedyCreateRequest entity = SpeedyCreateRequestBuilder.builder("Category")
                .addField("name", "cat-client-1")
                .build();

        SpeedyResponse speedyResponse = speedyApi.create(entity);

        assertFalse(speedyResponse.getPayload().isEmpty());
        assertNotNull(speedyResponse.getPayload().get(0).get("id").asText());
//        assertEquals("1", jsonNode.get(0).get("id").asText());
    }

    @Test
    void updateTest() throws Exception {
        SpeedyUpdateRequest request = SpeedyUpdateRequest.builder("Category")
                .key("id", "1")
                .field("name", "cat-CLIENT-updated-1")
                .build();

        SpeedyResponse speedyResponse = speedyApi.update(request);

        assertFalse(speedyResponse.getPayload().isEmpty());
        assertNotNull(speedyResponse.getPayload().get("id").asText());
//        assertEquals("1", jsonNode.get(0).get("id").asText());
    }

    @Test
    void deleteTest() throws Exception {
        SpeedyDeleteRequest request = SpeedyDeleteRequest.builder("Category")
                .key("id", "1")
                .build();

        SpeedyResponse speedyResponse = speedyApi.delete(request);

        assertFalse(speedyResponse.getPayload().isEmpty());
        assertNotNull(speedyResponse.getPayload().get("id").asText());
//        assertEquals("1", jsonNode.get(0).get("id").asText());
    }
}

