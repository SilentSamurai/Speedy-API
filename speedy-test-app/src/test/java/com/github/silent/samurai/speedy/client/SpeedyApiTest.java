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
class SpeedyApiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyApiTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    String createTest() throws Exception {
        SpeedyTestResult result = speedyClient.create("Category")
                .field("name", "cat-client-1")
                .execute()
                .expectOk();

        String id = result.jsonPath("$.payload[0].id");
        assertNotNull(id);

        return id;
    }

    void updateTest(String id) throws Exception {
        SpeedyTestResult result = speedyClient.update("Category")
                .key("id", id)
                .field("name", "cat-CLIENT-updated-1")
                .execute()
                .expectOk();

        String returnedId = result.jsonPath("$.payload[0].id");
        assertNotNull(returnedId);

        String name = result.jsonPath("$.payload[0].name");
        assertEquals("cat-CLIENT-updated-1", name);
    }

    void deleteTest(String id) throws Exception {
        SpeedyTestResult result = speedyClient.delete("Category")
                .key("id", id)
                .execute()
                .expectOk();

        String deletedId = result.jsonPath("$.payload[0].id");
        assertNotNull(deletedId);
    }

    void getTest(String id, String name) throws Exception {
        SpeedyTestResult result = speedyClient.get("Category")
                .key("id", id)
                .execute()
                .expectOk();

        String returnedId = result.jsonPath("$.payload[0].id");
        assertNotNull(returnedId);
        assertEquals(id, returnedId);

        String returnedName = result.jsonPath("$.payload[0].name");
        assertEquals(name, returnedName);
    }

    void query(String id, String name) throws Exception {
        SpeedyTestResult result = speedyClient.query("Category")
                .where(condition("name", eq(name)))
                .execute()
                .expectOk();

        String returnedId = result.jsonPath("$.payload[0].id");
        assertNotNull(returnedId);
        assertEquals(id, returnedId);

        String returnedName = result.jsonPath("$.payload[0].name");
        assertEquals(name, returnedName);
    }

    @Test
    void normalTest() throws Exception {
        String id = createTest();

        getTest(id, "cat-client-1");

        updateTest(id);

        getTest(id, "cat-CLIENT-updated-1");

        query(id, "cat-CLIENT-updated-1");

        deleteTest(id);
    }
}

