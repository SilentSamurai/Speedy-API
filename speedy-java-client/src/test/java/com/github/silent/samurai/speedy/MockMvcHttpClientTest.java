package com.github.silent.samurai.speedy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.clients.MockMvcHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MockMvcHttpClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldNotNpeOnNullBody() throws Exception {
        @org.springframework.web.bind.annotation.RestController
        class TestController {
            @org.springframework.web.bind.annotation.GetMapping("/test")
            public String test() { return "ok"; }
        }

        MockMvc mockMvc = standaloneSetup(new TestController()).build();
        MockMvcHttpClient client = new MockMvcHttpClient(mockMvc);

        ResultActions result = client.invokeAPI(
                "/test",
                HttpMethod.GET,
                new LinkedMultiValueMap<>(),
                null,
                new HttpHeaders()
        );

        assertNotNull(result);
        result.andExpect(status().isOk());
    }

    @Test
    void shouldSendPostWithBody() throws Exception {
        @org.springframework.web.bind.annotation.RestController
        class TestController {
            @org.springframework.web.bind.annotation.PostMapping("/test")
            public String test(@org.springframework.web.bind.annotation.RequestBody String body) { return body; }
        }

        MockMvc mockMvc = standaloneSetup(new TestController()).build();
        MockMvcHttpClient client = new MockMvcHttpClient(mockMvc);

        ObjectNode body = mapper.createObjectNode().put("key", "value");

        ResultActions result = client.invokeAPI(
                "/test",
                HttpMethod.POST,
                new LinkedMultiValueMap<>(),
                body,
                new HttpHeaders()
        );

        result.andExpect(status().isOk());
    }
}
