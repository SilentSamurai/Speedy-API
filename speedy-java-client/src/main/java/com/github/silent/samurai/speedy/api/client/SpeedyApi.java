package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.silent.samurai.speedy.api.client.models.SpeedyCreateRequest;
import com.github.silent.samurai.speedy.api.client.models.SpeedyDeleteRequest;
import com.github.silent.samurai.speedy.api.client.models.SpeedyGetRequest;
import com.github.silent.samurai.speedy.api.client.models.SpeedyUpdateRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SpeedyApi<T> {

    private static final String DEFAULT_API_PATH = "/speedy/v1/";

    private final HttpClient<T> httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;

    public SpeedyApi(HttpClient<T> httpClient) {
        this.httpClient = httpClient;
        this.baseUrl = DEFAULT_API_PATH;
    }

    public HttpClient<T> getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public SpeedyApi<T> setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return this;
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

    public T createMany(String entity, ArrayNode entities) throws Exception {
        SpeedyCreateRequest request = new SpeedyCreateRequest();
        request.setEntity(entity);
        request.setBody(entities);
        return createMany(request);
    }

    public T deleteMany(String entity, ArrayNode pks) throws Exception {
        SpeedyDeleteRequest request = new SpeedyDeleteRequest();
        request.setEntity(entity);
        request.setPkToDelete(pks);
        return delete(request);
    }

    public T delete(SpeedyDeleteRequest request) throws Exception {
        String path = this.baseUrl + request.getEntity() + "/$delete";

        return invokeAPI(
                path,
                HttpMethod.DELETE,
                request.getPkToDelete()
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

    public T count(SpeedyQuery speedyQuery) throws Exception {
        String path = this.baseUrl + speedyQuery.getFrom() + "/$count";
        JsonNode body = speedyQuery.build();

        return invokeAPI(
                path,
                HttpMethod.POST,
                body
        );
    }

    public T metadata() throws Exception {
        String path = this.baseUrl + "$metadata";

        return invokeAPI(
                path,
                HttpMethod.GET,
                null
        );
    }

    private T invokeAPI(String path, HttpMethod method, JsonNode body) throws Exception {
        return httpClient.invokeAPI(path, method, new LinkedMultiValueMap<>(), body, new HttpHeaders());
    }

    private String formatPrimaryKey(JsonNode pk) {
        if (pk == null || !pk.fields().hasNext()) {
            return "";
        }

        Stream<Map.Entry<String, JsonNode>> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(pk.fields(), Spliterator.ORDERED),
                false
        );

        String formattedPk = stream
                .map(e -> String.format("%s=%s", e.getKey(), e.getValue().asText()))
                .collect(Collectors.joining("&"));

        return "?" + formattedPk;
    }
}
