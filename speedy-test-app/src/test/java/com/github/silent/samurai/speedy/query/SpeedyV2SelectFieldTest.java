package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyV2SelectFieldTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void selectSpecificFields() throws Exception {
        speedyClient.query("Product")
                .select("id", "name")
                .pageSize(2)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThan(0)))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPathExists("$.payload[*].name");
    }

    @Test
    void selectWithExpandPreservesExpandedFields() throws Exception {
        speedyClient.query("Product")
                .select("id", "name", "category")
                .expand("Category")
                .pageSize(2)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThan(0)))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPathExists("$.payload[*].name")
                .expectJsonPathExists("$.payload[*].category")
                .expectJsonPathExists("$.payload[*].category.name");
    }
}