package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.models.SpeedyCreateRequestBuilder;
import com.github.silent.samurai.speedy.models.SpeedyCreateRequest;
import com.github.silent.samurai.speedy.models.SpeedyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpeedyApiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyApiTest.class);

    @Mock
    RestTemplate restTemplate;

    ObjectMapper objectMapper;
    SpeedyApi speedyApi;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ApiClient apiClient = new ApiClient(restTemplate);
        speedyApi = new SpeedyApi(apiClient);
    }

    @Test
    void create() throws Exception {


        SpeedyCreateRequest entity = SpeedyCreateRequestBuilder.builder("Resource")
                .addField("id", "1")
                .build();

        SpeedyCreateRequest response = SpeedyCreateRequestBuilder.builder("Resource")
                .addField("id", "1")
                .wrapInArray();

        Mockito.when(
                restTemplate.<JsonNode>exchange(
                        Mockito.any(RequestEntity.class),
                        Mockito.any(ParameterizedTypeReference.class)
                )
        ).thenAnswer(invocationOnMock -> {
            RequestEntity<JsonNode> requestEntity = invocationOnMock.getArgument(0);
            requestEntity.getMethod().matches("POST");
            LOGGER.info("Request: {}", requestEntity);
            assertSame(entity, requestEntity.getBody().get(0));
            return ResponseEntity.of(Optional.of(response));
        });

        SpeedyResponse speedyResponse = speedyApi.create(entity);

        assertEquals("1", speedyResponse.getPayload().get(0).get("id").asText());
    }


}