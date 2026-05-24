package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
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
import static org.hamcrest.Matchers.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class NullAssociationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NullAssociationTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void null_fk_with_entity() throws Exception {

        speedyClient.create("FkNullEntity")
                .field("name", "TEST")
                .field("category", null)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1)))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPath("$.payload[*].id", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPath("$.payload[*].category", Matchers.everyItem(Matchers.nullValue()));

        SpeedyTestResult mvcResult = speedyClient.query("FkNullEntity")
                .where(condition("category", eq(null)))
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1)))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPath("$.payload[*].id", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPathExists("$.payload[*].name")
                .expectJsonPath("$.payload[*].name", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPath("$.payload[*].name", Matchers.everyItem(Matchers.equalTo("TEST")))
                .expectJsonPathExists("$.payload[*].category")
                .expectJsonPath("$.payload[*].category", Matchers.everyItem(Matchers.nullValue()));
    }

    @Test
    void filtering_with_null_fk() throws Exception {
        speedyClient.create("FkNullEntity")
                .field("name", "TEST")
                .field("category", null)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1)))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPath("$.payload[*].id", Matchers.everyItem(Matchers.isA(String.class)));

        SpeedyTestResult mvcResult = speedyClient.query("FkNullEntity")
                .where(
                        condition("name", eq("TEST")),
                        condition("category", eq(null))
                )
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1)))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPath("$.payload[*].id", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPathExists("$.payload[*].name")
                .expectJsonPath("$.payload[*].name", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPath("$.payload[*].name", Matchers.everyItem(Matchers.equalTo("TEST")))
                .expectJsonPathExists("$.payload[*].category")
                .expectJsonPath("$.payload[*].category", Matchers.everyItem(Matchers.nullValue()));
    }

}
