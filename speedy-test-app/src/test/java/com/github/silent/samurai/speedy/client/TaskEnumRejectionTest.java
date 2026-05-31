package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskEnumRejectionTest {

    SpeedyTest speedyClient;
    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private String createTask(String priority, int difficulty) {
        SpeedyTestResult result = speedyClient.create("Task")
                .field("title", "valid-task-" + System.nanoTime())
                .field("priority", priority)
                .field("difficulty", difficulty)
                .execute()
                .expectOk();

        return result.jsonPath("$.payload[0].id");
    }

    // ---- STRING enum (priority) rejection ----

    @Test
    void createWithInvalidPriorityShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "bad-priority")
                .field("priority", "URGENT")
                .field("difficulty", 0)
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithIntegerForStringEnumPriorityShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "int-priority")
                .field("priority", 1)
                .field("difficulty", 0)
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithBooleanForStringEnumPriorityShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "bool-priority")
                .field("priority", true)
                .field("difficulty", 0)
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithDoubleForStringEnumPriorityShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "double-priority")
                .field("priority", 1.5)
                .field("difficulty", 0)
                .execute()
                .expectBadRequest();
    }

    @Test
    void updateWithInvalidPriorityShouldReturn400() {
        String id = createTask("LOW", 0);

        speedyClient.update("Task")
                .key("id", id)
                .field("priority", "URGENT")
                .execute()
                .expectBadRequest();
    }

    @Test
    void updateWithIntegerForStringEnumPriorityShouldReturn400() {
        String id = createTask("LOW", 0);

        speedyClient.update("Task")
                .key("id", id)
                .field("priority", 2)
                .execute()
                .expectBadRequest();
    }

    // ---- ORDINAL enum (difficulty) rejection ----

    @Test
    void createWithInvalidDifficultyOrdinalShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "bad-difficulty")
                .field("priority", "LOW")
                .field("difficulty", 99)
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithNegativeDifficultyOrdinalShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "neg-difficulty")
                .field("priority", "LOW")
                .field("difficulty", -1)
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithStringForOrdinalEnumDifficultyShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "str-difficulty")
                .field("priority", "LOW")
                .field("difficulty", "99")
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithEnumNameStringForOrdinalEnumShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "name-ordinal")
                .field("priority", "LOW")
                .field("difficulty", "EASY")
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithBooleanForOrdinalEnumDifficultyShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "bool-difficulty")
                .field("priority", "LOW")
                .field("difficulty", true)
                .execute()
                .expectBadRequest();
    }

    @Test
    void updateWithInvalidDifficultyOrdinalShouldReturn400() {
        String id = createTask("LOW", 0);

        speedyClient.update("Task")
                .key("id", id)
                .field("difficulty", 99)
                .execute()
                .expectBadRequest();
    }

    @Test
    void updateWithStringForOrdinalEnumDifficultyShouldReturn400() {
        String id = createTask("LOW", 0);

        speedyClient.update("Task")
                .key("id", id)
                .field("difficulty", "99")
                .execute()
                .expectBadRequest();
    }

    // ---- Combined rejections ----

    @Test
    void createWithBothInvalidEnumsShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "both-bad")
                .field("priority", "URGENT")
                .field("difficulty", 99)
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithSwappedTypesShouldReturn400() {
        speedyClient.create("Task")
                .field("title", "swapped")
                .field("priority", 0)
                .field("difficulty", "LOW")
                .execute()
                .expectBadRequest();
    }
}
