package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class AnnotatedPersonValidationIT {

    @Autowired
    MockMvc mockMvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @Nested
    @DisplayName("CREATE validation")
    class CreateValidation {
        @Test
        @DisplayName("CREATE invalid name (blank) should fail – NotBlankRule")
        void createBlankName_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "   ")
                    .field("age", 25)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .execute()
                    .expectBadRequest();
        }

        @Test
        @DisplayName("CREATE invalid age (<18) should fail – MinRule")
        void createUnderage_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 15)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("age must be >= 18"));
        }

        @Test
        @DisplayName("CREATE invalid email format should fail – EmailRule")
        void createInvalidEmail_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "not-an-email")
                    .field("code", "ABC12")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("email must be a valid email"));
        }

        @Test
        @DisplayName("CREATE name too short (<3) should fail – LengthRule min")
        void createShortName_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "Jo")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .execute()
                    .expectBadRequest();
        }

        @Test
        @DisplayName("CREATE name too long (>20) should fail – LengthRule max")
        void createLongName_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "ThisNameIsWayTooLongForTheField")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .execute()
                    .expectBadRequest();
        }

        @Test
        @DisplayName("CREATE age above 60 should fail – MaxRule")
        void createOverage_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 65)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("age must be <= 60"));
        }

        @Test
        @DisplayName("CREATE invalid code pattern should fail – RegexRule")
        void createInvalidCode_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ab12")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("does not match pattern"));
        }

        @Test
        @DisplayName("CREATE invalid salary (<=0) should fail – PositiveRule")
        void createInvalidSalary_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 0)
                    .field("score", 5)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 1)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("salary must be > 0"));
        }

        @Test
        @DisplayName("CREATE invalid debt (>=0) should fail – NegativeRule")
        void createInvalidDebt_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 5)
                    .field("debt", 0)
                    .field("overdraft", 0)
                    .field("rating", 1)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("debt must be < 0"));
        }

        @Test
        @DisplayName("CREATE invalid rating (<0.5) should fail – DecimalMinRule")
        void createInvalidRating_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 5)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 0.3)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("rating must be >= 0.5"));
        }

        @Test
        @DisplayName("CREATE invalid precisionVal (too many digits) should fail – DigitsRule")
        void createInvalidPrecision_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 5)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 1)
                    .field("precision_val", 123456.789)
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("precision_val numeric value out of bounds"));
        }

        @Test
        @DisplayName("CREATE valid payload should succeed")
        void createValid_shouldSucceed() {
            SpeedyTestResult createResult = client.create("AnnotatedPerson")
                    .field("name", "John Doe")
                    .field("age", 30)
                    .field("email", "john.doe@example.com")
                    .field("code", "ABC12")
                    .execute()
                    .expectOk()
                    .expectJsonPathExists("$.payload[0].id");

            String id = createResult.jsonPath("$.payload[0].id");

            client.update("AnnotatedPerson")
                    .key("id", id)
                    .field("name", "Jane Doe")
                    .execute()
                    .expectOk();

            client.delete("AnnotatedPerson")
                    .key("id", id)
                    .execute()
                    .expectOk();
        }
    }
}
