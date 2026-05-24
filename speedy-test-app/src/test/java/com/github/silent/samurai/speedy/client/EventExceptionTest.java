package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class EventExceptionTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void testBadRequestWhenEventHandlerThrows() throws Exception {
        String categoryName = "test-cat-" + java.util.UUID.randomUUID();
        SpeedyTestResult catResult = speedyClient.create("Category")
                .field("name", categoryName)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[*].id");
        String categoryId = catResult.jsonPath("$.payload[0].id");

        speedyClient.create("Product")
                .field("name", "invalid-trigger")
                .field("category.id", categoryId)
                .execute()
                .expectBadRequest();
    }
}
