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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Getter
@Setter
public class SpeedyApi<T> {

    private final HttpClient<T> httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;

    /**
     * Constructor that takes an HttpClient interface.
     * This allows users to plug in their own HTTP client implementation.
     *
     * @param httpClient the HTTP client implementation
     */
    public SpeedyApi(HttpClient<T> httpClient) {
        this.httpClient = httpClient;
        this.baseUrl = "/speedy/v1/";
    }

    public T create(SpeedyCreateRequest speedyCreateRequest) throws Exception {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(speedyCreateRequest.getBody());
        speedyCreateRequest.setBody(arrayNode);
        return createMany(speedyCreateRequest);
    }

    public T createMany(SpeedyCreateRequest speedyCreateRequest) throws Exception {
        return invokeAPI(
                this.baseUrl + speedyCreateRequest.getEntity() + "/$create",
                HttpMethod.POST,
                speedyCreateRequest.getBody()
        );
    }

    public T update(SpeedyUpdateRequest speedyUpdateRequest) throws Exception {
        String path = this.baseUrl + speedyUpdateRequest.getEntity() + "/$update";

        return invokeAPI(
                path,
                HttpMethod.PATCH,
                speedyUpdateRequest.getBody()
        );
    }

    public T delete(SpeedyDeleteRequest request) throws Exception {
        String path = this.baseUrl + request.getEntity() + "/$delete";

        return invokeAPI(
                path,
                HttpMethod.DELETE,
                request.getPkToDelete()
        );
    }

    public T get(SpeedyGetRequest request) throws Exception {
        String path = this.baseUrl + request.getEntity() + formatPrimaryKey(request.getPk());

        return invokeAPI(
                path,
                HttpMethod.GET,
                null
        );
    }

    public T query(SpeedyQuery speedyQuery) throws Exception {
        String path = this.baseUrl + speedyQuery.getFrom() + "/$query/";
        JsonNode body = speedyQuery.build();

        return invokeAPI(
                path,
                HttpMethod.POST,
                body
        );
    }

    // Utility method to reduce code duplication in API invocations
    private T invokeAPI(String path, HttpMethod method, JsonNode body) throws Exception {
        return httpClient.invokeAPI(path, method, new LinkedMultiValueMap<>(), body, new HttpHeaders());
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
                .collect(Collectors.joining("&"));

        return "?" + formattedPk;
    }

    // Helper method to create common headers
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
