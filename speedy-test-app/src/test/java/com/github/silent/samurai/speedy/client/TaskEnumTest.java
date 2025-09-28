package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import com.github.silent.samurai.speedy.util.SpeedyTestUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.eq;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests covering CRUD & query operations for Task entity which
 * demonstrates both STRING stored enums (TaskPriority) and ORDINAL stored enums (TaskDifficulty).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskEnumTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    SpeedyClient<SpeedyResponse> speedyClient;

    private String lastTitle;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        speedyClient = SpeedyClient.restTemplate(restTemplate, "http://localhost");
    }

    private String createTask(String title, String priority, String difficulty) throws Exception {
        SpeedyResponse response = speedyClient.create("Task")
                .addField("title", title)
                .addField("priority", priority)      // STRING stored enum
                .addField("difficulty", Integer.parseInt(difficulty))  // ORDINAL stored enum represented as ordinal int
                .execute();

        var idPath = SpeedyTestUtil.assertThat(response)
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[0].id", String.class).is(not(emptyString()))
                .path("$.payload[0].id", String.class);
        String id = idPath.get();
        lastTitle = title;
        return id;
    }

    private void assertTask(String id, String expectedPriority, String expectedDifficulty) throws Exception {
        SpeedyResponse get = speedyClient.get("Task")
                .key("id", id)
                .execute();
        SpeedyTestUtil.assertThat(get)
                .path("$.payload[0].priority", String.class).is(equalTo(expectedPriority))
                .path("$.payload[0].difficulty", Integer.class).is(equalTo(Integer.parseInt(expectedDifficulty)));
    }

    private void updateTask(String id, String newPriority, String newDifficulty) throws Exception {
        SpeedyResponse upd = speedyClient.update("Task")
                .key("id", id)
                .field("priority", newPriority)
                .field("difficulty", Integer.parseInt(newDifficulty))
                .execute();
        SpeedyTestUtil.assertThat(upd)
                .path("$.payload[0].priority", String.class).is(equalTo(newPriority))
                .path("$.payload[0].difficulty", Integer.class).is(equalTo(Integer.parseInt(newDifficulty)));
    }

    private void queryByPriority(String priority, String expectedId) throws Exception {
        SpeedyResponse query = speedyClient.query(
                SpeedyQuery.from("Task").where(condition("priority", eq(priority)))
        ).execute();
        SpeedyTestUtil.assertThat(query)
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[*].id", Iterable.class).is(hasItem(expectedId))
                .path("$.payload[*].priority", Iterable.class).is(hasItem(priority));
    }

    private void queryByDifficulty(String difficulty, String expectedId) throws Exception {
        SpeedyResponse query = speedyClient.query(
                SpeedyQuery.from("Task").where(condition("difficulty", eq(Integer.parseInt(difficulty))))
        ).execute();
        SpeedyTestUtil.assertThat(query)
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[*].id", Iterable.class).is(hasItem(expectedId))
                .path("$.payload[*].difficulty", Iterable.class).is(hasItem(Integer.parseInt(difficulty)));
    }

    @Test
    void testEnumCrudAndQuery() throws Exception {
        // Create with LOW/EASY
        String id = createTask("enum-task-1", "LOW", "0");
        assertTask(id, "LOW", "0");

        // Update to MEDIUM/MEDIUM
        updateTask(id, "MEDIUM", "1");
        assertTask(id, "MEDIUM", "1");

        // Query by MEDIUM priority/difficulty
        queryByPriority("MEDIUM", id);
        queryByDifficulty("1", id);

        // Update to HIGH/HARD
        updateTask(id, "HIGH", "2");
        assertTask(id, "HIGH", "2");

        queryByPriority("HIGH", id);
        queryByDifficulty("2", id);

        // Cleanup
        SpeedyResponse del = speedyClient.delete("Task").key("id", id).execute();
        SpeedyTestUtil.assertThat(del)
                .path("$.payload[0].id", String.class).is(equalTo(id));
    }
}
