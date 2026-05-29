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
        @DisplayName("CREATE invalid score (negative) should fail – PositiveOrZeroRule")
        void createNegativeScore_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", -1)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 1)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("score must be >= 0"));
        }

        @Test
        @DisplayName("CREATE valid score (zero boundary) should succeed – PositiveOrZeroRule")
        void createZeroScore_shouldSucceed() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 0)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 1)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectOk();
        }

        @Test
        @DisplayName("CREATE valid score (positive) should succeed – PositiveOrZeroRule")
        void createPositiveScore_shouldSucceed() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 10)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 1)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectOk();
        }

        @Test
        @DisplayName("CREATE invalid overdraft (positive) should fail – NegativeOrZeroRule")
        void createPositiveOverdraft_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 5)
                    .field("debt", -10)
                    .field("overdraft", 1)
                    .field("rating", 1)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("overdraft must be <= 0"));
        }

        @Test
        @DisplayName("CREATE valid overdraft (zero boundary) should succeed – NegativeOrZeroRule")
        void createZeroOverdraft_shouldSucceed() {
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
                    .field("precision_val", 123.45)
                    .execute()
                    .expectOk();
        }

        @Test
        @DisplayName("CREATE valid overdraft (negative) should succeed – NegativeOrZeroRule")
        void createNegativeOverdraft_shouldSucceed() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 5)
                    .field("debt", -10)
                    .field("overdraft", -100)
                    .field("rating", 1)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectOk();
        }

        @Test
        @DisplayName("CREATE invalid rating (above max) should fail – DecimalMaxRule")
        void createExcessiveRating_shouldFail() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 5)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 5.1)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("rating must be <= 5.0"));
        }

        @Test
        @DisplayName("CREATE valid rating (max boundary) should succeed – DecimalMaxRule")
        void createMaxRating_shouldSucceed() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 5)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 5.0)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectOk();
        }

        @Test
        @DisplayName("CREATE valid rating (within range) should succeed – DecimalMaxRule")
        void createValidRating_shouldSucceed() {
            client.create("AnnotatedPerson")
                    .field("name", "John")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 5)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 0.5)
                    .field("precision_val", 123.45)
                    .execute()
                    .expectOk();
        }

        @Test
        @DisplayName("CREATE valid bookingDate (min boundary 2025-01-01) should succeed – DateRangeRule")
        void createMinBookingDate_shouldSucceed() {
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
                    .field("precision_val", 123.45)
                    .field("bookingDate", "2025-01-01")
                    .execute()
                    .expectOk();
        }

        @Test
        @DisplayName("CREATE valid bookingDate (max boundary 2025-12-31) should succeed – DateRangeRule")
        void createMaxBookingDate_shouldSucceed() {
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
                    .field("precision_val", 123.45)
                    .field("bookingDate", "2025-12-31")
                    .execute()
                    .expectOk();
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
