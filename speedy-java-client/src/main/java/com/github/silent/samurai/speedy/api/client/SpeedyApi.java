package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.models.SpeedyCreateRequest;
import com.github.silent.samurai.speedy.models.SpeedyDeleteRequest;
import com.github.silent.samurai.speedy.models.SpeedyResponse;
import com.github.silent.samurai.speedy.models.SpeedyUpdateRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Getter
@Setter
public class SpeedyApi {

    private ApiClient apiClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl = "";

    public SpeedyApi(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.baseUrl = "/speedy/v1/";
    }

    public void init() {
        // pull metadata
    }

    public SpeedyResponse create(SpeedyCreateRequest speedyCreateRequest) throws Exception {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(speedyCreateRequest.getBody());
        speedyCreateRequest.setBody(arrayNode);
        return createMany(speedyCreateRequest);
    }


    public SpeedyResponse createMany(SpeedyCreateRequest speedyCreateRequest) throws Exception {
//        String body = new ObjectMapper().writeValueAsString(request);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {
                "application/json;charset=UTF-8"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
                "application/json;charset=UTF-8"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        ParameterizedTypeReference<SpeedyResponse> localReturnType = new ParameterizedTypeReference<>() {
        };
        return apiClient.invokeAPI(this.baseUrl + speedyCreateRequest.getEntity(),
                HttpMethod.POST,
                Collections.<String, Object>emptyMap(),
                queryParams,
                speedyCreateRequest.getBody(),
                headerParams,
                cookieParams,
                formParams,
                localVarAccept,
                localVarContentType,
                localVarAuthNames,
                localReturnType).getBody();

    }

    public SpeedyResponse update(SpeedyUpdateRequest speedyUpdateRequest) throws Exception {
//        String body = new ObjectMapper().writeValueAsString(entity);
        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {
                "application/json;charset=UTF-8"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
                "application/json;charset=UTF-8"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        ParameterizedTypeReference<SpeedyResponse> returnType = new ParameterizedTypeReference<>() {
        };


        Iterator<Map.Entry<String, JsonNode>> fields = speedyUpdateRequest.getPk().fields();

        Stream<Map.Entry<String, JsonNode>> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(fields, Spliterator.ORDERED),
                false);

        Optional<String> reduce = stream
                .map(e -> String.format("%s='%s'", e.getKey(), e.getValue().asText()))
                .reduce((a, b) -> a + "," + b);

        String path = this.baseUrl + speedyUpdateRequest.getEntity();
        if (reduce.isPresent()) {
            path = path + "(" + reduce.get() + ")";
        }


        return apiClient.invokeAPI(path,
                HttpMethod.PATCH,
                Collections.emptyMap(),
                queryParams,
                speedyUpdateRequest.getBody(),
                headerParams,
                cookieParams,
                formParams,
                localVarAccept,
                localVarContentType,
                localVarAuthNames,
                returnType).getBody();

    }


    public SpeedyResponse delete(SpeedyDeleteRequest request) {

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {
                "application/json;charset=UTF-8"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {
                "application/json;charset=UTF-8"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        ParameterizedTypeReference<SpeedyResponse> returnType = new ParameterizedTypeReference<>() {
        };


        Iterator<Map.Entry<String, JsonNode>> fields = request.getPk().fields();

        Stream<Map.Entry<String, JsonNode>> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(fields, Spliterator.ORDERED),
                false);

        Optional<String> reduce = stream
                .map(e -> String.format("%s='%s'", e.getKey(), e.getValue().asText()))
                .reduce((a, b) -> a + "," + b);

        String path = this.baseUrl + speedyUpdateRequest.getEntity();
        if (reduce.isPresent()) {
            path = path + "(" + reduce.get() + ")";
        }


        return apiClient.invokeAPI(path,
                HttpMethod.PATCH,
                Collections.emptyMap(),
                queryParams,
                speedyUpdateRequest.getBody(),
                headerParams,
                cookieParams,
                formParams,
                localVarAccept,
                localVarContentType,
                localVarAuthNames,
                returnType).getBody();
    }
}
