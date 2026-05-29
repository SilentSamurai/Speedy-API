package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.util.SpeedyTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskEnumTest {

    SpeedyTest speedyClient;
    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private String createTask() throws Exception {
        SpeedyTestResult result = speedyClient.create("Task")
                .field("title", "enum-task-1")
                .field("priority", "LOW")
                .field("difficulty", Integer.parseInt("0"))
                .execute();

        var idPath = SpeedyTestUtil.assertThat(result.responseBody())
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[0].id", String.class).is(not(emptyString()))
                .path("$.payload[0].id", String.class);
        String id = idPath.get();
        return id;
    }

    private void assertTask(String id, String expectedPriority, String expectedDifficulty) throws Exception {
        SpeedyTestResult get = speedyClient.get("Task")
                .key("id", id)
                .execute();
        SpeedyTestUtil.assertThat(get.responseBody())
                .path("$.payload[0].priority", String.class).is(equalTo(expectedPriority))
                .path("$.payload[0].difficulty", Integer.class).is(equalTo(Integer.parseInt(expectedDifficulty)));
    }

    private void updateTask(String id, String newPriority, String newDifficulty) throws Exception {
        SpeedyTestResult upd = speedyClient.update("Task")
                .key("id", id)
                .field("priority", newPriority)
                .field("difficulty", Integer.parseInt(newDifficulty))
                .execute();
        SpeedyTestUtil.assertThat(upd.responseBody())
                .path("$.payload[0].priority", String.class).is(equalTo(newPriority))
                .path("$.payload[0].difficulty", Integer.class).is(equalTo(Integer.parseInt(newDifficulty)));
    }

    private void queryByPriority(String priority, String expectedId) throws Exception {
        SpeedyTestResult query = speedyClient.query("Task")
                .where(condition("priority", eq(priority)))
                .execute();
        SpeedyTestUtil.assertThat(query.responseBody())
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[*].id", Iterable.class).is(hasItem(expectedId))
                .path("$.payload[*].priority", Iterable.class).is(hasItem(priority));
    }

    private void queryByDifficulty(String difficulty, String expectedId) throws Exception {
        SpeedyTestResult query = speedyClient.query("Task")
                .where(condition("difficulty", eq(Integer.parseInt(difficulty))))
                .execute();
        SpeedyTestUtil.assertThat(query.responseBody())
                .path("$.payload.length()", Integer.class).is(greaterThan(0))
                .path("$.payload[*].id", Iterable.class).is(hasItem(expectedId))
                .path("$.payload[*].difficulty", Iterable.class).is(hasItem(Integer.parseInt(difficulty)));
    }

    @Test
    void testEnumCrudAndQuery() throws Exception {
        String id = createTask();
        assertTask(id, "LOW", "0");

        updateTask(id, "MEDIUM", "1");
        assertTask(id, "MEDIUM", "1");

        queryByPriority("MEDIUM", id);
        queryByDifficulty("1", id);

        updateTask(id, "HIGH", "2");
        assertTask(id, "HIGH", "2");

        queryByPriority("HIGH", id);
        queryByDifficulty("2", id);

        SpeedyTestResult del = speedyClient.delete("Task").key("id", id).execute();
        SpeedyTestUtil.assertThat(del.responseBody())
                .path("$.payload[0].id", String.class).is(equalTo(id));
    }
}
