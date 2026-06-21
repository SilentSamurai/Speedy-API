package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Reproduces finding #3: on UPDATE, {@code DefaultFieldValidator} runs with {@code enforceRequired=false}
 * AND skips any supplied value that is empty (DefaultFieldValidator:129) before the rule set runs. So a
 * PATCH that sets a {@code @NotBlank} / non-nullable field to "" bypasses validation entirely and is
 * accepted — you can blank out a required field. (Note: a whitespace value like "  " IS caught, because
 * it is not "empty"; only the empty-string case slips through.)
 *
 * The test creates a valid AnnotatedPerson (name is @NotBlank, @Column(nullable=false)) and then updates
 * name to "". It asserts the update is rejected — which fails today (the update returns 200 OK).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class UpdateBlanksRequiredFieldTest {

    @Autowired
    MockMvc mockMvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @Test
    void updateSettingNotBlankFieldToEmpty_shouldBeRejected() {
        SpeedyTestResult created = client.create("AnnotatedPerson")
                .field("name", "John Doe")
                .field("age", 30)
                .field("email", "john.doe@example.com")
                .field("code", "ABC12")
                .field("salary", 1000)
                .field("score", 5)
                .field("debt", -10)
                .field("overdraft", 0)
                .field("rating", 1)
                .field("precisionVal", 123.45)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id");

        String id = created.jsonPath("$.payload[0].id");

        // Blank out a @NotBlank / non-nullable field via UPDATE. This MUST be rejected.
        // FAILS today: the update is accepted (200 OK) because the empty value is skipped before rules run.
        client.update("AnnotatedPerson")
                .key("id", id)
                .field("name", "")
                .execute()
                .expectBadRequest();
    }
}
