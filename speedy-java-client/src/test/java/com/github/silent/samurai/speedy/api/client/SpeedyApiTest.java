package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.silent.samurai.speedy.models.SpeedyEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.match.JsonPathRequestMatchers;
import org.springframework.test.web.reactive.server.JsonPathAssertions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpeedyApiTest {

    @Mock
    RestTemplate restTemplate;

    ObjectMapper objectMapper;
    SpeedyApi speedyApi;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ApiClient apiClient = new ApiClient(restTemplate);
        speedyApi = new SpeedyApi(apiClient, "Resource");
    }

    @Test
    void create() throws Exception {

        ObjectNode entity = SpeedyEntityBuilder.builder()
                .addField("id", "1")
                .build();

        ArrayNode response = SpeedyEntityBuilder.builder()
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
            assertSame(entity, requestEntity.getBody().get(0));
            return ResponseEntity.of(Optional.of(response));
        });

        JsonNode jsonNode = speedyApi.create(entity);

        assertEquals("1", jsonNode.get(0).get("id").asText());
    }


}