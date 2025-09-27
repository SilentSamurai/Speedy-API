package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Verifies that when a BadRequestException is raised inside a Speedy event handler
 * the HTTP response returned to the caller has status 400 (Bad Request).
 * <p>
 * The corresponding event handler lives in {@code EntityEvents#productInsert}. When the "name"
 * field of the {@code Product} entity equals "invalid-trigger" the handler throws
 * {@code BadRequestException}. This test triggers that code path and validates
 * that the client receives a {@link HttpClientErrorException.BadRequest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class EventExceptionTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyClient<ResultActions> speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyClient.mockMvc(mvc);
    }

    @Test
    void testBadRequestWhenEventHandlerThrows() throws Exception {
        // First create a valid category to satisfy the FK constraint of Product
        String categoryName = "test-cat-" + java.util.UUID.randomUUID();
        String catJson = speedyClient.create("Category")
                .addField("name", categoryName)
                .execute()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[*].id").exists())
                .andReturn().getResponse().getContentAsString();
        String categoryId = JsonPath.read(catJson, "$.payload[0].id");



        // Attempt to create a product with an invalid name. The PRE_INSERT event handler will
        // throw BadRequestException which should be translated to an HTTP 400.
        speedyClient.create("Product")
                .addField("name", "invalid-trigger")
                .addField("category.id", categoryId)
                .execute()
                .andExpect(status().isBadRequest());
    }
}
