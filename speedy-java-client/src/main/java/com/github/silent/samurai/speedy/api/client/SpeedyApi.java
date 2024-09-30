package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.silent.samurai.speedy.api.client.models.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Getter
@Setter
public class SpeedyApi {

    private final ApiClient apiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;

    public SpeedyApi(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.baseUrl = "/speedy/v1/";
    }

    // Initialize metadata (if required)
    public void init() {
        // pull metadata
    }

    public SpeedyResponse create(SpeedyCreateRequest speedyCreateRequest) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(speedyCreateRequest.getBody());
        speedyCreateRequest.setBody(arrayNode);
        return createMany(speedyCreateRequest);
    }

    public SpeedyResponse createMany(SpeedyCreateRequest speedyCreateRequest) {
        return invokeAPI(
                this.baseUrl + speedyCreateRequest.getEntity() + "/$create",
                HttpMethod.POST,
                speedyCreateRequest.getBody()
        );
    }

    public SpeedyResponse update(SpeedyUpdateRequest speedyUpdateRequest) {
        String path = this.baseUrl + speedyUpdateRequest.getEntity() + "/$update";

        return invokeAPI(
                path,
                HttpMethod.PATCH,
                speedyUpdateRequest.getBody()
        );
    }

    public SpeedyResponse delete(SpeedyDeleteRequest request) {
        String path = this.baseUrl + request.getEntity() + "/$delete";

        return invokeAPI(
                path,
                HttpMethod.DELETE,
                request.getPkToDelete()
        );
    }

    public SpeedyResponse get(SpeedyGetRequest request) {
        String path = this.baseUrl + request.getEntity() + formatPrimaryKey(request.getPk());

        return invokeAPI(
                path,
                HttpMethod.GET,
                null
        );
    }

    public SpeedyResponse query(SpeedyQuery speedyQuery) {
        String path = this.baseUrl + speedyQuery.getFrom() + "/$query/";
        JsonNode body = speedyQuery.build();

        return invokeAPI(
                path,
                HttpMethod.POST,
                body
        );
    }

    // Utility method to reduce code duplication in API invocations
    private SpeedyResponse invokeAPI(String path,
                                     HttpMethod method,
                                     JsonNode body) {

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        final HttpHeaders headerParams = createHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();

        final String[] localVarAccepts = {"application/json;charset=UTF-8"};
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {"application/json;charset=UTF-8"};
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        ParameterizedTypeReference<SpeedyResponse> returnType = new ParameterizedTypeReference<>() {
        };

        return apiClient.invokeAPI(
                path, method, Collections.emptyMap(), queryParams, body,
                headerParams, cookieParams, formParams, localVarAccept, localVarContentType,
                new String[]{}, returnType).getBody();
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
