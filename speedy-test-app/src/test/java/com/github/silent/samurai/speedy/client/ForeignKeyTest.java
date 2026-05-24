package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ForeignKeyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForeignKeyTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void normalTest() throws Exception {

        SpeedyTestResult createResponse = speedyClient.create("Product")
                .field("name", "client-product-1")
                .field("description", "test description")
                .field("category.id", "1")
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id");

        String productId = createResponse.jsonPath("$.payload[0].id");
        LOGGER.info("Created product with ID: {}", productId);

        SpeedyTestResult getResponse = speedyClient.get("Product")
                .key("id", productId)
                .execute()
                .expectOk();

        assertEquals(productId, getResponse.jsonPath("$.payload[0].id"));
        assertEquals("client-product-1", getResponse.jsonPath("$.payload[0].name"));
        assertEquals("created-by-event", getResponse.jsonPath("$.payload[0].description"));
        assertEquals("1", getResponse.jsonPath("$.payload[0].category.id"));
        LOGGER.info("Product is associated with category ID: 1");
        LOGGER.info("Fetched product details match with the created product");

        SpeedyTestResult updateResponse = speedyClient.update("Product")
                .key("id", productId)
                .field("name", "updated-client-product")
                .execute()
                .expectOk();

        assertEquals(productId, updateResponse.jsonPath("$.payload[0].id"));
        assertEquals("updated-client-product", updateResponse.jsonPath("$.payload[0].name"));
        LOGGER.info("Updated product name to 'updated-client-product'");

        SpeedyTestResult queryResponse = speedyClient.query("Product")
                .where(condition("name", eq("updated-client-product")))
                .execute()
                .expectOk();

        assertEquals(productId, queryResponse.jsonPath("$.payload[0].id"));
        assertEquals("updated-client-product", queryResponse.jsonPath("$.payload[0].name"));
        LOGGER.info("Queried product successfully by updated name");

        SpeedyTestResult deleteResponse = speedyClient.delete("Product")
                .key("id", productId)
                .execute()
                .expectOk();

        assertEquals(productId, deleteResponse.jsonPath("$.payload[0].id"));
        LOGGER.info("Product with ID {} successfully deleted", productId);
    }
}
