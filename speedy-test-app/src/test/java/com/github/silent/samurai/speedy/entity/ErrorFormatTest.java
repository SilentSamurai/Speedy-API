package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.util.SpeedyTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ErrorFormatTest {

    @Autowired
    private MockMvc mockMvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @Test
    void badRequestError_hasStandardFormat() {
        SpeedyTestResult result = client.create("Supplier")
                .field("name", "")
                .execute();
        SpeedyTestUtil.expectErrorFormat(result, 400);
    }

    @Test
    void notFoundError_hasStandardFormat() throws Exception {
        String body = "{\"$from\":\"NonExistentEntity\"}";

        mockMvc.perform(post(SpeedyConstant.URI + "/NonExistentEntity/" + SpeedyEndpoint.QUERY.suffix())
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.timestamp").exists());
    }

    @Test
    void methodNotAllowed_onMetadataPost() throws Exception {
        mockMvc.perform(post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.METADATA.suffix())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}
