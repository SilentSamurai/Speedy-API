package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.ApiClient;
import com.github.silent.samurai.speedy.api.client.SpeedyApi;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.api.client.SpeedyRequest;
import com.github.silent.samurai.speedy.api.client.models.*;
import com.github.silent.samurai.speedy.repositories.ValueTestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityManagerFactory;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.$condition;
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.$eq;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ForeignKeyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForeignKeyTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    ValueTestRepository valueTestRepository;
    ApiClient defaultClient;
    SpeedyApi speedyApi;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
        speedyApi = new SpeedyApi(defaultClient);
    }

    @Test
    void normalTest() throws Exception {

        // Create Product with Foreign Key (Category)
        SpeedyCreateRequest createRequest = SpeedyCreateRequest.builder("Product")
                .addField("name", "client-product-1")
                .addField("description", "test description")
                .addField("category.id", "1")  // Foreign Key to Category entity
                .build();

        SpeedyResponse createResponse = speedyApi.create(createRequest);

        // Assert that the product creation is successful
        assertFalse(createResponse.getPayload().isEmpty());
        JsonNode product = createResponse.getPayload().get(0);

        assertTrue(product.has("id"));
        assertTrue(product.get("id").isTextual());
        String productId = product.get("id").asText();
        LOGGER.info("Created product with ID: {}", productId);



        // Fetch and validate the created product
        SpeedyGetRequest getRequest = SpeedyGetRequest.builder("Product")
                .key("id", productId)
                .build();

        SpeedyResponse getResponse = speedyApi.get(getRequest);

        assertFalse(getResponse.getPayload().isEmpty());
        JsonNode fetchedProduct = getResponse.getPayload().get(0);

        assertEquals(productId, fetchedProduct.get("id").asText());
        assertEquals("client-product-1", fetchedProduct.get("name").asText());
        assertEquals("test description", fetchedProduct.get("description").asText());
        assertEquals("1", fetchedProduct.get("category").get("id").asText());

        // Validate Foreign Key association with Category
        assertTrue(fetchedProduct.has("category"));
        assertTrue(fetchedProduct.get("category").has("id"));
        assertEquals("1", fetchedProduct.get("category").get("id").asText());
        LOGGER.info("Product is associated with category ID: 1");

        LOGGER.info("Fetched product details match with the created product");

        // Update Product name
        SpeedyUpdateRequest updateRequest = SpeedyRequest.update("Product")
                .key("id", productId)
                .field("name", "updated-client-product")
                .build();

        SpeedyResponse updateResponse = speedyApi.update(updateRequest);

        // Validate update
        JsonNode updatedProduct = updateResponse.getPayload();
        assertEquals(productId, updatedProduct.get("id").asText());
        assertEquals("updated-client-product", updatedProduct.get("name").asText());
        LOGGER.info("Updated product name to 'updated-client-product'");

        // Query the updated product using the new name
        SpeedyQuery queryRequest = SpeedyRequest.query("Product")
                .$where(
                        $condition("name", $eq("updated-client-product"))
                );

        SpeedyResponse queryResponse = speedyApi.query(queryRequest);

        // Validate query result
        assertFalse(queryResponse.getPayload().isEmpty());
        JsonNode queriedProduct = queryResponse.getPayload().get(0);
        assertEquals(productId, queriedProduct.get("id").asText());
        assertEquals("updated-client-product", queriedProduct.get("name").asText());
        LOGGER.info("Queried product successfully by updated name");

        // Optionally, delete the product for cleanup
        SpeedyDeleteRequest deleteRequest = SpeedyDeleteRequest.builder("Product")
                .key("id", productId)
                .build();

        SpeedyResponse deleteResponse = speedyApi.delete(deleteRequest);

        assertFalse(deleteResponse.getPayload().isEmpty());
        JsonNode deletedProduct = deleteResponse.getPayload().get(0);
        assertEquals(productId, deletedProduct.get("id").asText());
        LOGGER.info("Product with ID {} successfully deleted", productId);
    }
}
