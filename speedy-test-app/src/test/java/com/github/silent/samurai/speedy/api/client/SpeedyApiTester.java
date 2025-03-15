package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.silent.samurai.speedy.api.client.models.SpeedyCreateRequest;
import com.github.silent.samurai.speedy.api.client.models.SpeedyDeleteRequest;
import com.github.silent.samurai.speedy.api.client.models.SpeedyGetRequest;
import com.github.silent.samurai.speedy.api.client.models.SpeedyUpdateRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Getter
@Setter
public class SpeedyApiTester {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;

    public SpeedyApiTester(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.baseUrl = "/speedy/v1/";
    }

    // Initialize metadata (if required)
    public void init() {
        // pull metadata
    }

    public ResultActions create(SpeedyCreateRequest speedyCreateRequest) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(speedyCreateRequest.getBody());
        speedyCreateRequest.setBody(arrayNode);
        return createMany(speedyCreateRequest);
    }

    public ResultActions createMany(SpeedyCreateRequest speedyCreateRequest) {
        return invokeAPI(
                this.baseUrl + speedyCreateRequest.getEntity() + "/$create",
                HttpMethod.POST,
                speedyCreateRequest.getBody()
        );
    }

    public ResultActions update(SpeedyUpdateRequest speedyUpdateRequest) {
        String path = this.baseUrl + speedyUpdateRequest.getEntity() + "/$update";

        return invokeAPI(
                path,
                HttpMethod.PATCH,
                speedyUpdateRequest.getBody()
        );
    }

    public ResultActions delete(SpeedyDeleteRequest request) {
        String path = this.baseUrl + request.getEntity() + "/$delete";

        return invokeAPI(
                path,
                HttpMethod.DELETE,
                request.getPkToDelete()
        );
    }

    public ResultActions get(SpeedyGetRequest request) {
        String path = this.baseUrl + request.getEntity() + formatPrimaryKey(request.getPk());

        return invokeAPI(
                path,
                HttpMethod.GET,
                null
        );
    }

    public ResultActions query(SpeedyQuery speedyQuery) {
        String path = this.baseUrl + speedyQuery.getFrom() + "/$query/";
        JsonNode body = speedyQuery.build();

        return invokeAPI(
                path,
                HttpMethod.POST,
                body
        );
    }

    @SneakyThrows
    // Utility method to reduce code duplication in API invocations
    public ResultActions invokeAPI(String path, HttpMethod method, JsonNode body) {
        return mockMvc.perform(
                MockMvcRequestBuilders.request(method, path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
        );
    }

    // Helper method to format primary key fields
    private String formatPrimaryKey(JsonNode pk) {
        if (pk == null || !pk.fields().hasNext()) {
            return "";
        }

        Stream<Map.Entry<String, JsonNode>> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(pk.fields(), Spliterator.ORDERED),
                false
        );

        String formattedPk = stream
                .map(e -> String.format("%s='%s'", e.getKey(), e.getValue().asText()))
                .collect(Collectors.joining(","));

        return "(" + formattedPk + ")";
    }

    // Helper method to create common headers
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
