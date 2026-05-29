package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductValidationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @Test
    void createWithInvalidTriggerName_shouldFail() {
        client.create("Product")
                .field("name", "invalid-trigger")
                .field("category.id", "cat-1")
                .execute()
                .expectBadRequest()
                .expectJsonPath("$.message", containsString("invalid-trigger"));
    }

    @Test
    void createValid_shouldSucceed() {
        String productName = "Valid Product " + System.nanoTime();

        SpeedyTestResult result = client.create("Product")
                .field("name", productName)
                .field("category.id", "cat-1")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", greaterThan(0))
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = result.jsonPath("$.payload[0].id");

        EntityManager em = entityManagerFactory.createEntityManager();
        Product persisted = em.createQuery(
                        "SELECT p FROM Product p WHERE p.id = :id", Product.class)
                .setParameter("id", id)
                .getSingleResult();

        assertEquals(productName, persisted.getName());
        em.close();
    }
}
