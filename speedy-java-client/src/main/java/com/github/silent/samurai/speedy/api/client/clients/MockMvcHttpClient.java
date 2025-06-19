package com.github.silent.samurai.speedy.api.client.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.api.client.HttpClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.MultiValueMap;

/**
 * MockMvc implementation of HttpClient for testing purposes.
 * This client uses Spring's MockMvc to simulate HTTP requests without starting a full web server.
 */
public class MockMvcHttpClient implements HttpClient<ResultActions> {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a new MockMvcHttpClient with the specified MockMvc instance.
     *
     * @param mockMvc the MockMvc instance to use for HTTP requests
     */
    public MockMvcHttpClient(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Override
    public ResultActions invokeAPI(String path,
                                   HttpMethod method,
                                   MultiValueMap<String, String> queryParams,
                                   JsonNode body,
                                   HttpHeaders headerParams) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.request(method, path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
        );
    }
} 