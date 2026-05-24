package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
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

import java.time.ZonedDateTime;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class NegativeTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(NegativeTests.class);

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
        speedyClient.create("ValueTestEntity")
                .field("localDateTime", "11-09-2020")
                .execute()
                .expectBadRequest();
    }

    @Test
    void sqlInjectTest() throws Exception {
        speedyClient.create("ValueTestEntity")
                .field("localDateTime", "delete Customer")
                .execute()
                .expectBadRequest();
    }

    @Test
    void sqlInjectTest2() throws Exception {
        speedyClient.create("Product")
                .field("name", "abcd'; delete Customer")
                .field("category.id", "1")
                .execute();
    }

    @Test
    void sqlInjectTestInNameField() throws Exception {
        speedyClient.create("Product")
                .field("name", "test'; DROP TABLE Products; --")
                .field("category.id", "1")
                .execute();
    }

    @Test
    void sqlInjectTestInIdField() throws Exception {
        speedyClient.create("Procurement")
                .field("amount", "1 OR 1=1; --")
                .field("dueAmount", 0)
                .field("product.id", "1")
                .field("supplier.id", "1")
                .field("purchaseDate", ZonedDateTime.now())
                .execute();
    }

    @Test
    void sqlInjectUnionAttackTest() throws Exception {
        speedyClient.create("Procurement")
                .field("amount", "1 UNION SELECT * FROM users; --")
                .field("dueAmount", 0)
                .field("product.id", "1")
                .field("supplier.id", "1")
                .field("purchaseDate", ZonedDateTime.now())
                .execute();
    }

    @Test
    void sqlInjectTestInWhereClause() throws Exception {
        speedyClient.query("Customer")
                .where(condition("name", eq("John'; DROP TABLE Customer; --")))
                .execute();
    }

    @Test
    void sqlInjectTestInNumericField() throws Exception {
        speedyClient.create("Procurement")
                .field("amount", "1 OR 1=1; --")
                .field("dueAmount", 0)
                .field("product.id", "1")
                .field("supplier.id", "1")
                .field("purchaseDate", ZonedDateTime.now())
                .execute();
    }
}
