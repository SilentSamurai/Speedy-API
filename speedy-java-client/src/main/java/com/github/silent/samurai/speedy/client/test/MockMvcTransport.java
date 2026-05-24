package com.github.silent.samurai.speedy.client.test;

import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;
import com.github.silent.samurai.speedy.client.transport.SpeedyTransport;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SpeedyTransport} implementation that wraps Spring's {@link MockMvc}
 * for integration testing without starting a real HTTP server.
 *
 * <p>Requires {@code spring-test} and {@code jakarta.servlet-api} on the classpath (optional).
 */
public class MockMvcTransport implements SpeedyTransport {

    private final MockMvc mockMvc;
    private ResultActions lastResultActions;

    public MockMvcTransport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    ResultActions getLastResultActions() {
        return lastResultActions;
    }

    @Override
    public SpeedyRawResponse send(SpeedyRequest request) throws IOException {
        try {
            MockHttpServletRequestBuilder builder = buildRequest(request);
            ResultActions actions = mockMvc.perform(builder);
            this.lastResultActions = actions;
            MockHttpServletResponse response = actions.andReturn().getResponse();

            Map<String, List<String>> headers = new HashMap<>();
            for (String name : response.getHeaderNames()) {
                headers.put(name, new ArrayList<>(response.getHeaders(name)));
            }

            return new SpeedyRawResponse(
                    response.getStatus(),
                    headers,
                    response.getContentAsString());

        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("MockMvc request failed: " + e.getMessage(), e);
        }
    }

    private MockHttpServletRequestBuilder buildRequest(SpeedyRequest request) {
        MockHttpServletRequestBuilder builder;
        String method = request.method().toUpperCase();
        String url = request.url();

        switch (method) {
            case "GET" -> builder = MockMvcRequestBuilders.get(url);
            case "POST" -> builder = MockMvcRequestBuilders.post(url);
            case "PATCH" -> builder = MockMvcRequestBuilders.patch(url);
            case "PUT" -> builder = MockMvcRequestBuilders.put(url);
            case "DELETE" -> builder = MockMvcRequestBuilders.delete(url);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        builder.header("Accept", "application/json");
        if (request.body() != null && !request.body().isEmpty()) {
            builder.contentType("application/json");
            builder.content(request.body());
        }

        for (Map.Entry<String, List<String>> entry : request.headers().entrySet()) {
            for (String value : entry.getValue()) {
                builder.header(entry.getKey(), value);
            }
        }

        return builder;
    }
}
