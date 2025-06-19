package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * Interface for HTTP client operations used by SpeedyApi.
 * This allows users to plug in their own HTTP client implementation.
 */
public interface HttpClient<T> {

    /**
     * Invoke an HTTP API call with the given parameters.
     *
     * @param path         The sub-path of the HTTP URL
     * @param method       The request method
     * @param queryParams  The query parameters
     * @param body         The request body object
     * @param headerParams The header parameters
     * @return ResponseEntity with the response
     * @throws RestClientException if the HTTP call fails
     */
    T invokeAPI(String path,
                HttpMethod method,
                MultiValueMap<String, String> queryParams,
                JsonNode body,
                HttpHeaders headerParams) throws RestClientException, JsonProcessingException, Exception;

} 