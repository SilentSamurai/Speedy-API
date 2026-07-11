package com.github.silent.samurai.speedy.client.transport;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class JdkHttpTransportTest {

    @Test
    void transportConstructorShouldNotThrow() {
        assertDoesNotThrow((org.junit.jupiter.api.function.Executable) JdkHttpTransport::new);
    }

    @Test
    void customHttpClientShouldBeAccepted() {
        java.net.http.HttpClient custom = java.net.http.HttpClient.newHttpClient();
        JdkHttpTransport transport = new JdkHttpTransport(custom);
        assertNotNull(transport);
    }

    @Test
    void sendPublishesBodyAddsDefaultHeadersAndMapsResponse() throws Exception {
        RecordingHttpClient client = new RecordingHttpClient(response(201,
                Map.of("X-Request-Id", List.of("request-123")), "created"));
        JdkHttpTransport transport = new JdkHttpTransport(client);
        String body = "{\"name\":\"Miyazaki\"}";
        SpeedyRequest request = new SpeedyRequest("POST", "https://example.test/suppliers",
                Map.of("X-Tenant", List.of("tenant-a", "tenant-b")), body);

        SpeedyRawResponse result = transport.send(request);

        assertEquals(201, result.statusCode());
        assertEquals("created", result.body());
        assertEquals(List.of("request-123"), result.headers().get("X-Request-Id"));
        assertEquals("POST", client.request.method());
        assertEquals(URI.create("https://example.test/suppliers"), client.request.uri());
        assertEquals(body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                client.request.bodyPublisher().orElseThrow().contentLength());
        assertEquals("application/json;charset=UTF-8",
                client.request.headers().firstValue("Accept").orElseThrow());
        assertEquals("application/json;charset=UTF-8",
                client.request.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(List.of("tenant-a", "tenant-b"), client.request.headers().allValues("X-Tenant"));
    }

    @Test
    void sendPreservesExplicitHeadersAndUsesNoBodyForEmptyContent() throws Exception {
        RecordingHttpClient client = new RecordingHttpClient(response(204, Map.of(), ""));
        JdkHttpTransport transport = new JdkHttpTransport(client);
        SpeedyRequest request = new SpeedyRequest("DELETE", "https://example.test/suppliers/1",
                Map.of("accept", List.of("application/xml"), "Content-Type", List.of("text/plain")), "");

        SpeedyRawResponse result = transport.send(request);

        assertEquals(204, result.statusCode());
        assertEquals("application/xml", client.request.headers().firstValue("Accept").orElseThrow());
        assertEquals("text/plain", client.request.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(0, client.request.bodyPublisher().orElseThrow().contentLength());
    }

    @Test
    void sendRestoresInterruptStatusWhenHttpClientIsInterrupted() {
        RecordingHttpClient client = new RecordingHttpClient(response(200, Map.of(), ""));
        client.interrupt = true;
        JdkHttpTransport transport = new JdkHttpTransport(client);

        try {
            IOException exception = assertThrows(IOException.class,
                    () -> transport.send(new SpeedyRequest("GET", "https://example.test/metadata", Map.of(), null)));

            assertEquals("Request interrupted", exception.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void requestShouldPreserveHeaders() {
        SpeedyRequest request = new SpeedyRequest("GET", "http://localhost", null, null);
        assertEquals(Collections.emptyMap(), request.headers());
    }

    @Test
    void withHeaderShouldAddHeader() {
        SpeedyRequest request = new SpeedyRequest("GET", "http://localhost", new HashMap<>(), null);
        SpeedyRequest modified = request.withHeader("Authorization", "Bearer token");

        assertTrue(modified.headers().containsKey("Authorization"));
        assertEquals(List.of("Bearer token"), modified.headers().get("Authorization"));
        assertTrue(request.headers().isEmpty());
    }

    @Test
    void withHeadersShouldAddMultipleHeaders() {
        SpeedyRequest request = new SpeedyRequest("GET", "http://localhost", new HashMap<>(), null);
        Map<String, String> headers = Map.of("X-Trace", "trace-id", "X-Tenant", "tenant-1");
        SpeedyRequest modified = request.withHeaders(headers);

        assertTrue(modified.headers().containsKey("X-Trace"));
        assertTrue(modified.headers().containsKey("X-Tenant"));
    }

    @Test
    void rawResponseShouldPreserveStatusCode() {
        SpeedyRawResponse response = new SpeedyRawResponse(201, Map.of(), "body");
        assertEquals(201, response.statusCode());
        assertEquals("body", response.body());
        assertTrue(response.is2xx());
    }

    @Test
    void rawResponseShouldDetectStatusCategories() {
        assertTrue(new SpeedyRawResponse(200, Map.of(), null).is2xx());
        assertTrue(new SpeedyRawResponse(400, Map.of(), null).is4xx());
        assertTrue(new SpeedyRawResponse(500, Map.of(), null).is5xx());
        assertFalse(new SpeedyRawResponse(201, Map.of(), null).is4xx());
    }

    @Test
    void rawResponseShouldHandleNullHeaders() {
        SpeedyRawResponse response = new SpeedyRawResponse(200, null, null);
        assertTrue(response.headers().isEmpty());
    }

    private static HttpResponse<String> response(int status, Map<String, List<String>> headers, String body) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return status;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(headers, (name, value) -> true);
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return URI.create("https://example.test");
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }

    private static class RecordingHttpClient extends HttpClient {
        private final HttpResponse<String> response;
        private HttpRequest request;
        private boolean interrupt;

        private RecordingHttpClient(HttpResponse<String> response) {
            this.response = response;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            this.request = request;
            if (interrupt) {
                throw new InterruptedException("interrupted by test");
            }
            @SuppressWarnings("unchecked")
            HttpResponse<T> typedResponse = (HttpResponse<T>) response;
            return typedResponse;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                 HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("not used by JdkHttpTransport");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                 HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                 HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException("not used by JdkHttpTransport");
        }
    }
}
