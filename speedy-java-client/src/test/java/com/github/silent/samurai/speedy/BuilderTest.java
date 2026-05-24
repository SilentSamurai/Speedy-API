package com.github.silent.samurai.speedy;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.api.client.HttpClient;
import com.github.silent.samurai.speedy.api.client.SpeedyApi;
import com.github.silent.samurai.speedy.api.client.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;

class BuilderTest {

    private SpeedyApi<String> speedyApi;
    private TestHttpClient httpClient;

    static class TestHttpClient implements HttpClient<String> {
        String lastPath;
        HttpMethod lastMethod;
        JsonNode lastBody;

        @Override
        public String invokeAPI(String path, HttpMethod method,
                                MultiValueMap<String, String> queryParams,
                                JsonNode body, HttpHeaders headerParams) {
            this.lastPath = path;
            this.lastMethod = method;
            this.lastBody = body;
            return "ok";
        }
    }

    @BeforeEach
    void setUp() {
        httpClient = new TestHttpClient();
        speedyApi = new SpeedyApi<>(httpClient);
    }

    @Test
    void createBuilderShouldSendCorrectPath() throws Exception {
        SpeedyCreateRequest request = new SpeedyCreateRequest();
        request.setEntity("User");

        speedyApi.create(request);

        assertTrue(httpClient.lastPath.endsWith("/User/$create"));
        assertEquals(HttpMethod.POST, httpClient.lastMethod);
    }

    @Test
    void getBuilderShouldSendCorrectPath() throws Exception {
        SpeedyGetRequest request = new SpeedyGetRequest();
        request.setEntity("User");

        speedyApi.get(request);

        assertTrue(httpClient.lastPath.contains("/User"));
        assertEquals(HttpMethod.GET, httpClient.lastMethod);
        assertNull(httpClient.lastBody);
    }

    @Test
    void updateBuilderShouldSendCorrectPath() throws Exception {
        SpeedyUpdateRequest request = new SpeedyUpdateRequest();
        request.setEntity("User");
        request.setBody(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());

        speedyApi.update(request);

        assertTrue(httpClient.lastPath.endsWith("/User/$update"));
        assertEquals(HttpMethod.PATCH, httpClient.lastMethod);
    }

    @Test
    void deleteBuilderShouldSendCorrectPath() throws Exception {
        SpeedyDeleteRequest request = new SpeedyDeleteRequest();
        request.setEntity("User");
        request.setPkToDelete(new com.fasterxml.jackson.databind.ObjectMapper().createArrayNode());

        speedyApi.delete(request);

        assertTrue(httpClient.lastPath.endsWith("/User/$delete"));
        assertEquals(HttpMethod.DELETE, httpClient.lastMethod);
    }

    @Test
    void metadataShouldSendCorrectPath() throws Exception {
        speedyApi.metadata();

        assertTrue(httpClient.lastPath.endsWith("$metadata"));
        assertEquals(HttpMethod.GET, httpClient.lastMethod);
    }

    @Test
    void setBaseUrlShouldNormalizeTrailingSlash() {
        speedyApi.setBaseUrl("/custom/api");
        assertEquals("/custom/api/", speedyApi.getBaseUrl());

        speedyApi.setBaseUrl("/custom/api/");
        assertEquals("/custom/api/", speedyApi.getBaseUrl());
    }
}
