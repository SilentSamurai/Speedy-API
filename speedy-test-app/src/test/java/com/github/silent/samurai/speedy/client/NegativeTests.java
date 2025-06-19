package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import com.github.silent.samurai.speedy.repositories.ValueTestRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class NegativeTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(NegativeTests.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    ValueTestRepository valueTestRepository;
    SpeedyClient<SpeedyResponse> speedyClient;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        speedyClient = SpeedyClient.restTemplate(restTemplate, "http://localhost");
    }

    @Test
    void normalTest() throws Exception {
        HttpClientErrorException.BadRequest badRequestException = assertThrows(HttpClientErrorException.BadRequest.class, () -> {
            speedyClient.create("ValueTestEntity")
                    .addField("localDateTime", "11-09-2020")
                    .execute();
        });
    }

    @Test
    void sqlInjectTest() throws Exception {
        HttpClientErrorException.BadRequest badRequestException = assertThrows(HttpClientErrorException.BadRequest.class, () -> {
            speedyClient.create("ValueTestEntity")
                    .addField("localDateTime", "delete Customer")
                    .execute();
        });
    }

    @Test
    void sqlInjectTest2() throws Exception {
        speedyClient.create("Product")
                .addField("name", "abcd'; delete Customer")
                .addField("category.id", "1")
                .execute();
    }

    @Test
    void sqlInjectTestInNameField() throws Exception {
        speedyClient.create("Product")
                .addField("name", "test'; DROP TABLE Products; --")
                .addField("category.id", "1")
                .execute();
    }

    @Test
    void sqlInjectTestInIdField() throws Exception {
        speedyClient.create("Procurement")
                .addField("amount", "1 OR 1=1; --")
                .addField("dueAmount", 0)
                .addField("product.id", "1")
                .addField("supplier.id", "1")
                .addField("purchaseDate", ZonedDateTime.now())
                .execute();
    }

    @Test
    void sqlInjectUnionAttackTest() throws Exception {
        speedyClient.create("Procurement")
                .addField("amount", "1 UNION SELECT * FROM users; --")
                .addField("dueAmount", 0)
                .addField("product.id", "1")
                .addField("supplier.id", "1")
                .addField("purchaseDate", ZonedDateTime.now())
                .execute();
    }

    @Test
    void sqlInjectTestInWhereClause() throws Exception {
        speedyClient.query(
                        SpeedyQuery.from("Customer")
                                .where(condition("name", eq("John'; DROP TABLE Customer; --")))
                )
                .execute();
    }

    @Test
    void sqlInjectTestInNumericField() throws Exception {
        speedyClient.create("Procurement")
                .addField("amount", "1 OR 1=1; --")
                .addField("dueAmount", 0)
                .addField("product.id", "1")
                .addField("supplier.id", "1")
                .addField("purchaseDate", ZonedDateTime.now())
                .execute();
    }
}
