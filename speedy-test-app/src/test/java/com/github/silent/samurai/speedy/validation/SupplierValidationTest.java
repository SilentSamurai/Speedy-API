package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.entity.Supplier;
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
class SupplierValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    private String uniquePhone() {
        String nanos = Long.toString(System.nanoTime());
        return "+91" + nanos.substring(Math.max(0, nanos.length() - 10));
    }

    @Test
    void createWithoutName_shouldFail() {
        String uniquePhone = uniquePhone();

        client.create("Supplier")
                .field("phoneNo", uniquePhone)
                .field("altPhoneNo", uniquePhone)
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithoutPhoneNo_shouldFail() {
        client.create("Supplier")
                .field("name", "Test Supplier " + System.nanoTime())
                .field("altPhoneNo", "+91-9999999999")
                .execute()
                .expectBadRequest();
    }

    @Test
    void createValid_shouldSucceed() {
        String uniquePhone = uniquePhone();
        String supplierName = "Valid Supplier " + System.nanoTime();

        SpeedyTestResult result = client.create("Supplier")
                .field("name", supplierName)
                .field("phoneNo", uniquePhone)
                .field("altPhoneNo", uniquePhone)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", greaterThan(0))
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = result.jsonPath("$.payload[0].id");

        EntityManager em = entityManagerFactory.createEntityManager();
        Supplier persisted = em.createQuery(
                        "SELECT s FROM Supplier s WHERE s.id = :id", Supplier.class)
                .setParameter("id", id)
                .getSingleResult();

        assertEquals(supplierName, persisted.getName());
        assertEquals(uniquePhone, persisted.getPhoneNo());
        em.close();
    }
}
