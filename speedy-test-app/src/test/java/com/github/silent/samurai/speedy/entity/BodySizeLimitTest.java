package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "speedy.api.max-request-body-size=100B")
class BodySizeLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void oversizedBody_returns413() throws Exception {
        StringBuilder json = new StringBuilder("{\"name\":\"");
        json.append("A".repeat(200));
        json.append("\"}");

        mockMvc.perform(post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(json.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.status", equalTo(413)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.message", not(emptyString())))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void bodyWithinLimit_succeeds() throws Exception {
        String json = "[{\"name\":\"test-cat-" + UUID.randomUUID() + "\"}]";

        mockMvc.perform(post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void oversizedBody_withContentLength_returns413() throws Exception {
        StringBuilder json = new StringBuilder("{\"name\":\"");
        json.append("A".repeat(200));
        json.append("\"}");
        byte[] body = json.toString().getBytes();

        mockMvc.perform(post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.status", equalTo(413)));
    }

    @Test
    void bodyExactlyAtLimit_succeeds() throws Exception {
        String fieldValue = "A".repeat(70);
        String json = "[{\"name\":\"" + fieldValue + "\"}]";

        mockMvc.perform(post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());
    }

}
