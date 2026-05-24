package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class DateValidationIT {

    @Autowired MockMvc mockMvc;
    private SpeedyTest client;

    @BeforeEach
    void setup() { client = SpeedyTest.mockMvc(mockMvc); }

    @Nested
    @DisplayName("CREATE date validation")
    class CreateValidation {

        @Test
        @DisplayName("CREATE with past-required field in future should fail – PastRule")
        void createInvalidBirthDate_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .field("name", "JD")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("birthDate", LocalDate.now().plusDays(1).toString())
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("birthDate"));
        }

        @Test
        @DisplayName("CREATE with future-required field in past should fail – FutureRule")
        void createInvalidAppointmentDate_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .field("name", "JD")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("appointmentDate", LocalDate.now().minusDays(1).toString())
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("appointment"));
        }

        @Test
        @DisplayName("CREATE with booking date outside range should fail – DateRangeRule")
        void createInvalidBookingDate_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .field("name", "JD")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("bookingDate", "2026-01-01")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("booking"));
        }

        @Test
        @DisplayName("CREATE with ISO format violation should fail – DateFormatRule")
        void createInvalidIsoDate_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .field("name", "JD")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("isoDate", "2025-01-01T10:00:00")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("ISO_DATE"));
        }

        @Test
        @DisplayName("CREATE valid dates should succeed")
        void createValidDates_shouldSucceed() throws Exception {
            client.create("AnnotatedPerson")
                    .field("name", "JohnDoe")
                    .field("age", 30)
                    .field("email", "john@example.com")
                    .field("code", "ABC12")
                    .field("salary", 1000)
                    .field("score", 0)
                    .field("debt", -10)
                    .field("overdraft", 0)
                    .field("rating", 1)
                    .field("precisionVal", 123.45)
                    .field("birthDate", LocalDate.now().minusYears(20).toString())
                    .field("appointmentDate", LocalDate.now().plusDays(10).toString())
                    .field("bookingDate", "2025-06-15")
                    .field("isoDate", "2025-05-01")
                    .execute()
                    .expectOk()
                    .expectJsonPathExists("$.payload[0].id");
        }
    }
}
