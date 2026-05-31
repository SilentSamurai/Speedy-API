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
class CompanyEnumRejectionTest {

    SpeedyTest speedyClient;
    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private String createCompany(String status) {
        String nanos = Long.toString(System.nanoTime());
        String email = "company-rej-" + nanos + "@example.com";
        speedyClient.create("Company")
                .field("name", "rejection-test")
                .field("address", "221B Baker Street")
                .field("email", email)
                .field("phone", "+44" + nanos.substring(Math.max(0, nanos.length() - 10)))
                .field("currency", "GBP")
                .field("status", status)
                .execute()
                .expectOk();

        SpeedyTestResult get = speedyClient.get("Company")
                .key("email", email)
                .execute()
                .expectOk();
        return get.jsonPath("$.payload[0].id");
    }

    @Test
    void createWithInvalidEnumNameShouldReturn400() {
        speedyClient.create("Company")
                .field("name", "bad-status-co")
                .field("address", "Test")
                .field("email", "bad-status@test.com")
                .field("phone", "1234567890")
                .field("currency", "GBP")
                .field("status", "NOT_A_VALID_STATUS")
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithIntegerForStringEnumShouldReturn400() {
        speedyClient.create("Company")
                .field("name", "int-status-co")
                .field("address", "Test")
                .field("email", "int-status@test.com")
                .field("phone", "1234567890")
                .field("currency", "GBP")
                .field("status", 1)
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithBooleanForStringEnumShouldReturn400() {
        speedyClient.create("Company")
                .field("name", "bool-status-co")
                .field("address", "Test")
                .field("email", "bool-status@test.com")
                .field("phone", "1234567890")
                .field("currency", "GBP")
                .field("status", true)
                .execute()
                .expectBadRequest();
    }

    @Test
    void createWithDoubleForStringEnumShouldReturn400() {
        speedyClient.create("Company")
                .field("name", "double-status-co")
                .field("address", "Test")
                .field("email", "double-status@test.com")
                .field("phone", "1234567890")
                .field("currency", "GBP")
                .field("status", 1.5)
                .execute()
                .expectBadRequest();
    }

    @Test
    void updateWithInvalidEnumNameShouldReturn400() {
        String id = createCompany("DRAFT");

        speedyClient.update("Company")
                .key("id", id)
                .field("status", "NOT_A_VALID_STATUS")
                .execute()
                .expectBadRequest();
    }

    @Test
    void updateWithIntegerForStringEnumShouldReturn400() {
        String id = createCompany("DRAFT");

        speedyClient.update("Company")
                .key("id", id)
                .field("status", 2)
                .execute()
                .expectBadRequest();
    }

}

