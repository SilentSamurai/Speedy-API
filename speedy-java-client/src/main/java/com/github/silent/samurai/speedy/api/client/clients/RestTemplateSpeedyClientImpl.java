package com.github.silent.samurai.speedy.api.client.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.api.client.HttpClient;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import lombok.Getter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Getter
public class RestTemplateSpeedyClientImpl implements HttpClient<SpeedyResponse> {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestTemplateSpeedyClientImpl(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public SpeedyResponse invokeAPI(String path,
                                    HttpMethod method,
                                    MultiValueMap<String, String> queryParams,
                                    JsonNode body,
                                    HttpHeaders headerParams) throws Exception {
        // Build full URI with query parameters
        String uri = UriComponentsBuilder.fromPath(path)
                .queryParams(queryParams)
                .build()
                .toUriString();

        // Prepare the request entity
        HttpEntity<?> requestEntity = (body != null)
                ? new HttpEntity<>(body.toString(), headerParams)
                : new HttpEntity<>(headerParams);

        // Send the request
        ResponseEntity<SpeedyResponse> response = restTemplate.exchange(
                uri,
                method,
                requestEntity,
                SpeedyResponse.class
        );

        return response.getBody();
    }
}
