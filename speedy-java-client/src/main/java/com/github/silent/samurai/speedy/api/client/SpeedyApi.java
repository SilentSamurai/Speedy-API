package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class SpeedyApi {
    private final String resource;
    private ApiClient apiClient;
    private ObjectMapper objectMapper = new ObjectMapper();

    public SpeedyApi(ApiClient apiClient, String resource) {
        this.apiClient = apiClient;
        this.resource = resource;
        this.apiClient.setBasePath(this.apiClient.getBasePath() + "/speedy/v1/" + resource);
    }

    public void init() {
        // pull metadata
    }

    public JsonNode create(ObjectNode entity) throws Exception {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(entity);
        return createMany(arrayNode);
    }


    public JsonNode createMany(ArrayNode request) throws Exception {
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

        ParameterizedTypeReference<JsonNode> localReturnType = new ParameterizedTypeReference<>() {
        };
        return apiClient.invokeAPI(this.apiClient.getBasePath(),
                HttpMethod.POST, Collections.<String, Object>emptyMap(),
                queryParams,
                request,
                headerParams,
                cookieParams,
                formParams,
                localVarAccept,
                localVarContentType,
                localVarAuthNames,
                localReturnType).getBody();

    }

    public JsonNode update(ObjectNode entity) throws Exception {
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

        ParameterizedTypeReference<JsonNode> localReturnType = new ParameterizedTypeReference<>() {
        };
        return apiClient.invokeAPI(this.apiClient.getBasePath(),
                HttpMethod.POST, Collections.<String, Object>emptyMap(),
                queryParams,
                entity,
                headerParams,
                cookieParams,
                formParams,
                localVarAccept,
                localVarContentType,
                localVarAuthNames,
                localReturnType).getBody();

    }


}
