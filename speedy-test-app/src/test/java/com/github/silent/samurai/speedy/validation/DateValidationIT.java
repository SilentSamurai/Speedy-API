package com.github.silent.samurai.speedy.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for newly added date validations on AnnotatedPerson.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class DateValidationIT {

    @Autowired MockMvc mockMvc;
    private SpeedyClient<ResultActions> client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() { client = SpeedyClient.mockMvc(mockMvc); }

    @Nested
    @DisplayName("CREATE date validation")
    class CreateValidation {

        @Test
        @DisplayName("CREATE with past-required field in future should fail – PastRule")
        void createInvalidBirthDate_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "JD") // keep other required fields minimal
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .addField("birthDate", LocalDate.now().plusDays(1).toString())
                    .execute()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("birthDate")));
        }

        @Test
        @DisplayName("CREATE with future-required field in past should fail – FutureRule")
        void createInvalidAppointmentDate_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "JD")
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .addField("appointmentDate", LocalDate.now().minusDays(1).toString())
                    .execute()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("appointment")));
        }

        @Test
        @DisplayName("CREATE with booking date outside range should fail – DateRangeRule")
        void createInvalidBookingDate_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "JD")
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .addField("bookingDate", "2026-01-01")
                    .execute()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("booking")));
        }

        @Test
        @DisplayName("CREATE with ISO format violation should fail – DateFormatRule")
        void createInvalidIsoDate_shouldFail() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "JD")
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    .addField("isoDate", "2025-01-01T10:00:00") // date-time instead of date
                    .execute()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("ISO_DATE")));
        }

        @Test
        @DisplayName("CREATE valid dates should succeed")
        void createValidDates_shouldSucceed() throws Exception {
            client.create("AnnotatedPerson")
                    .addField("name", "JohnDoe")
                    .addField("age", 30)
                    .addField("email", "john@example.com")
                    .addField("code", "ABC12")
                    // numeric required fields
                    .addField("salary", 1000)
                    .addField("score", 0)
                    .addField("debt", -10)
                    .addField("overdraft", 0)
                    .addField("rating", 1)
                    .addField("precisionVal", 123.45)
                    // date fields under test
                    .addField("birthDate", LocalDate.now().minusYears(20).toString())
                    .addField("appointmentDate", LocalDate.now().plusDays(10).toString())
                    .addField("bookingDate", "2025-06-15")
                    .addField("isoDate", "2025-05-01")
                    .execute()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload[0].id").exists());
        }
    }
}
