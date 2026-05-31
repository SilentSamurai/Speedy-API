package com.github.silent.samurai.speedy.client.spring;

import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;
import com.github.silent.samurai.speedy.client.transport.SpeedyTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestTemplateTransportTest {

    @Mock
    private RestTemplate restTemplate;

    private SpeedyTransport transport;

    @BeforeEach
    void setUp() {
        transport = new RestTemplateTransport(restTemplate);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("default constructor should not throw")
        void defaultConstructorShouldNotThrow() {
            assertDoesNotThrow((Executable) RestTemplateTransport::new);
        }

        @Test
        @DisplayName("custom RestTemplate constructor should accept non-null")
        void customRestTemplateShouldBeAccepted() {
            RestTemplateTransport transport = new RestTemplateTransport(restTemplate);
            assertNotNull(transport);
        }
    }

    @Nested
    @DisplayName("send() success response")
    class SendSuccess {

        @Test
        @DisplayName("should return SpeedyRawResponse with correct status and body")
        void shouldReturnCorrectStatusAndBody() throws IOException {
            String responseBody = "{\"payload\":[{\"id\":\"1\"}]}";
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("X-Custom", "value");

            ResponseEntity<String> responseEntity = new ResponseEntity<>(
                    responseBody, responseHeaders, HttpStatus.OK);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    any(HttpEntity.class), eq(String.class)))
                    .thenReturn(responseEntity);

            SpeedyRequest request = new SpeedyRequest("GET", "http://localhost/test", Map.of(), null);

            SpeedyRawResponse response = transport.send(request);

            assertEquals(200, response.statusCode());
            assertEquals(responseBody, response.body());
            assertTrue(response.is2xx());
            assertFalse(response.is4xx());
            assertFalse(response.is5xx());
            assertEquals(List.of("value"), response.headers().get("X-Custom"));
        }

        @Test
        @DisplayName("should handle 201 Created response")
        void shouldHandleCreatedResponse() throws IOException {
            ResponseEntity<String> responseEntity = new ResponseEntity<>(
                    "{\"id\":\"new\"}", HttpStatus.CREATED);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    any(HttpEntity.class), eq(String.class)))
                    .thenReturn(responseEntity);

            SpeedyRequest request = new SpeedyRequest("POST", "http://localhost/test", Map.of(), "{}");

            SpeedyRawResponse response = transport.send(request);

            assertEquals(201, response.statusCode());
            assertTrue(response.is2xx());
        }

        @Test
        @DisplayName("should handle 204 No Content with null body")
        void shouldHandleNoContent() throws IOException {
            ResponseEntity<String> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    any(HttpEntity.class), eq(String.class)))
                    .thenReturn(responseEntity);

            SpeedyRequest request = new SpeedyRequest("DELETE", "http://localhost/test", Map.of(), null);

            SpeedyRawResponse response = transport.send(request);

            assertEquals(204, response.statusCode());
            assertNull(response.body());
            assertTrue(response.is2xx());
        }
    }

    @Nested
    @DisplayName("send() error response")
    class SendError {

        @Test
        @DisplayName("should catch 4xx and return response with status intact")
        void shouldCatch4xxAndReturnResponse() throws IOException {
            String errorBody = "{\"message\":\"Bad Request\"}";
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.add("X-Error", "true");

            HttpStatusCodeException exception = mock(HttpStatusCodeException.class);
            when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
            when(exception.getResponseBodyAsString()).thenReturn(errorBody);
            when(exception.getResponseHeaders()).thenReturn(errorHeaders);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    any(HttpEntity.class), eq(String.class)))
                    .thenThrow(exception);

            SpeedyRequest request = new SpeedyRequest("GET", "http://localhost/test", Map.of(), null);

            SpeedyRawResponse response = transport.send(request);

            assertEquals(400, response.statusCode());
            assertEquals(errorBody, response.body());
            assertTrue(response.is4xx());
            assertFalse(response.is2xx());
            assertEquals(List.of("true"), response.headers().get("X-Error"));
        }

        @Test
        @DisplayName("should catch 5xx and return response with status intact")
        void shouldCatch5xxAndReturnResponse() throws IOException {
            String errorBody = "{\"message\":\"Internal Error\"}";

            HttpStatusCodeException exception = mock(HttpStatusCodeException.class);
            when(exception.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
            when(exception.getResponseBodyAsString()).thenReturn(errorBody);
            when(exception.getResponseHeaders()).thenReturn(null);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    any(HttpEntity.class), eq(String.class)))
                    .thenThrow(exception);

            SpeedyRequest request = new SpeedyRequest("GET", "http://localhost/test", Map.of(), null);

            SpeedyRawResponse response = transport.send(request);

            assertEquals(500, response.statusCode());
            assertEquals(errorBody, response.body());
            assertTrue(response.is5xx());
            assertFalse(response.is2xx());
        }

        @Test
        @DisplayName("should handle 404 Not Found")
        void shouldHandleNotFound() throws IOException {
            HttpStatusCodeException exception = mock(HttpStatusCodeException.class);
            when(exception.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
            when(exception.getResponseBodyAsString()).thenReturn(null);
            when(exception.getResponseHeaders()).thenReturn(null);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    any(HttpEntity.class), eq(String.class)))
                    .thenThrow(exception);

            SpeedyRequest request = new SpeedyRequest("GET", "http://localhost/missing", Map.of(), null);

            SpeedyRawResponse response = transport.send(request);

            assertEquals(404, response.statusCode());
            assertTrue(response.is4xx());
        }
    }

    @Nested
    @DisplayName("send() network failure")
    class SendNetworkFailure {

        @Test
        @DisplayName("should propagate RestClientException")
        void shouldPropagateRestClientException() {
            RestClientException cause = new RestClientException("Connection refused");

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    any(HttpEntity.class), eq(String.class)))
                    .thenThrow(cause);

            SpeedyRequest request = new SpeedyRequest("GET", "http://localhost/test", Map.of(), null);

            RestClientException thrown = assertThrows(RestClientException.class,
                    () -> transport.send(request));
            assertTrue(thrown.getMessage().contains("Connection refused"));
        }
    }

    @Nested
    @DisplayName("send() header forwarding")
    class HeaderForwarding {

        @Test
        @DisplayName("should forward custom headers from SpeedyRequest")
        void shouldForwardCustomHeaders() throws IOException {
            ResponseEntity<String> responseEntity = new ResponseEntity<>("ok", HttpStatus.OK);

            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    entityCaptor.capture(), eq(String.class)))
                    .thenReturn(responseEntity);

            SpeedyRequest request = new SpeedyRequest("GET", "http://localhost/test",
                    Map.of(), null);
            request = request.withHeader("Authorization", "Bearer token123");
            request = request.withHeader("X-Tenant", "tenant-1");

            transport.send(request);

            HttpEntity<String> capturedEntity = entityCaptor.getValue();
            HttpHeaders capturedHeaders = capturedEntity.getHeaders();

            assertEquals(List.of("Bearer token123"), capturedHeaders.get("Authorization"));
            assertEquals(List.of("tenant-1"), capturedHeaders.get("X-Tenant"));
        }

        @Test
        @DisplayName("should set Accept header to application/json")
        void shouldSetAcceptHeader() throws IOException {
            ResponseEntity<String> responseEntity = new ResponseEntity<>("ok", HttpStatus.OK);

            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    entityCaptor.capture(), eq(String.class)))
                    .thenReturn(responseEntity);

            SpeedyRequest request = new SpeedyRequest("GET", "http://localhost/test", Map.of(), null);

            transport.send(request);

            HttpEntity<String> capturedEntity = entityCaptor.getValue();
            HttpHeaders capturedHeaders = capturedEntity.getHeaders();

            assertEquals(List.of("application/json;charset=UTF-8"),
                    capturedHeaders.get("Accept"));
        }

        @Test
        @DisplayName("should set Content-Type header when body is present")
        void shouldSetContentTypeWhenBodyPresent() throws IOException {
            ResponseEntity<String> responseEntity = new ResponseEntity<>("ok", HttpStatus.OK);

            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    entityCaptor.capture(), eq(String.class)))
                    .thenReturn(responseEntity);

            SpeedyRequest request = new SpeedyRequest("POST", "http://localhost/test",
                    Map.of(), "{\"name\":\"test\"}");

            transport.send(request);

            HttpEntity<String> capturedEntity = entityCaptor.getValue();
            HttpHeaders capturedHeaders = capturedEntity.getHeaders();

            assertEquals(List.of("application/json;charset=UTF-8"),
                    capturedHeaders.get("Content-Type"));
        }

        @Test
        @DisplayName("should not set Content-Type when body is null")
        void shouldNotSetContentTypeWhenBodyNull() throws IOException {
            ResponseEntity<String> responseEntity = new ResponseEntity<>("ok", HttpStatus.OK);

            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

            when(restTemplate.exchange(any(String.class), any(HttpMethod.class),
                    entityCaptor.capture(), eq(String.class)))
                    .thenReturn(responseEntity);

            SpeedyRequest request = new SpeedyRequest("GET", "http://localhost/test", Map.of(), null);

            transport.send(request);

            HttpEntity<String> capturedEntity = entityCaptor.getValue();
            HttpHeaders capturedHeaders = capturedEntity.getHeaders();

            assertNull(capturedHeaders.get("Content-Type"));
        }

        @Test
        @DisplayName("should forward method and URL correctly")
        void shouldForwardMethodAndUrl() throws IOException {
            ResponseEntity<String> responseEntity = new ResponseEntity<>("ok", HttpStatus.OK);

            when(restTemplate.exchange(eq("http://localhost/resource"),
                    eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(responseEntity);

            SpeedyRequest request = new SpeedyRequest("POST",
                    "http://localhost/resource", Map.of(), "{\"key\":\"value\"}");

            SpeedyRawResponse response = transport.send(request);

            assertEquals(200, response.statusCode());
            verify(restTemplate).exchange(eq("http://localhost/resource"),
                    eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
        }
    }
}
