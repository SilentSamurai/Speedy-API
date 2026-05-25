package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class NullAssociationTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void null_fk_with_entity() {
        String uniqueName = "TEST-" + System.nanoTime();

        speedyClient.create("FkNullEntity")
                .field("name", uniqueName)
                .field("category", null)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1)))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPath("$.payload[*].id", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPath("$.payload[*].category", Matchers.everyItem(Matchers.nullValue()));

        speedyClient.query("FkNullEntity")
                .where(
                        condition("name", eq(uniqueName)),
                        condition("category", eq(null))
                )
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(1))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPath("$.payload[*].id", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPathExists("$.payload[*].name")
                .expectJsonPath("$.payload[*].name", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPath("$.payload[*].name", Matchers.everyItem(Matchers.equalTo(uniqueName)))
                .expectJsonPathExists("$.payload[*].category")
                .expectJsonPath("$.payload[*].category", Matchers.everyItem(Matchers.nullValue()));
    }

    @Test
    void filtering_with_null_fk() {
        String uniqueName = "TEST-" + System.nanoTime();

        speedyClient.create("FkNullEntity")
                .field("name", uniqueName)
                .field("category", null)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1)))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPath("$.payload[*].id", Matchers.everyItem(Matchers.isA(String.class)));

        speedyClient.query("FkNullEntity")
                .where(
                        condition("name", eq(uniqueName)),
                        condition("category", eq(null))
                )
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(1))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPath("$.payload[*].id", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPathExists("$.payload[*].name")
                .expectJsonPath("$.payload[*].name", Matchers.everyItem(Matchers.isA(String.class)))
                .expectJsonPath("$.payload[*].name", Matchers.everyItem(Matchers.equalTo(uniqueName)))
                .expectJsonPathExists("$.payload[*].category")
                .expectJsonPath("$.payload[*].category", Matchers.everyItem(Matchers.nullValue()));
    }

}
