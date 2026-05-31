package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(OutputCaptureExtension.class)
public class SpeedyV2SelectFieldTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void selectSpecificFields() throws Exception {
        speedyClient.query("Product")
                .select("id", "name")
                .pageSize(2)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThan(0)))
                .expectJsonPathExists("$.payload[0].id")
                .expectJsonPathExists("$.payload[0].name")
                .expectJsonPathDoesNotExist("$.payload[0].description")
                .expectJsonPathDoesNotExist("$.payload[0].category");
    }

    @Test
    void selectWithAssociationEntityKeyPresent() throws Exception {
        speedyClient.query("Product")
                .select("name", "category")
                .pageSize(2)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThan(0)))
                .expectJsonPathExists("$.payload[0].name")
                .expectJsonPathExists("$.payload[0].category")
                .expectJsonPathExists("$.payload[0].category.id");
    }

    @Test
    void selectWithoutAssociationOmitsIt() throws Exception {
        speedyClient.query("Product")
                .select("name")
                .pageSize(2)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThan(0)))
                .expectJsonPathExists("$.payload[0].name")
                .expectJsonPathDoesNotExist("$.payload[0].category");
    }

    @Test
    void selectWithNoValidFieldsFallsBackToSqlAll() throws Exception {
        speedyClient.query("Product")
                .select("nonExistentField")
                .pageSize(2)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThan(0)));
    }

    @Test
    void selectRestrictsDbColumns(CapturedOutput output) throws Exception {
        speedyClient.query("Product")
                .select("name", "description")
                .pageSize(2)
                .execute()
                .expectOk();

        String captured = output.toString();
        assertTrue(captured.contains("SQL Query:"), "SQL query should be logged");
        String sqlLines = captured.substring(captured.indexOf("SQL Query:"));
        assertTrue(sqlLines.contains("ID"), "SQL should select ID column");
        assertTrue(sqlLines.contains("NAME"), "SQL should select NAME column");
        assertTrue(sqlLines.contains("DESCRIPTION"), "SQL should select DESCRIPTION column");
    }

    @Test
    void selectWithExpandPreservesExpandedFields() throws Exception {
        speedyClient.query("Product")
                .select("id", "name", "category")
                .expand("Category")
                .pageSize(2)
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThan(0)))
                .expectJsonPathExists("$.payload[*].id")
                .expectJsonPathExists("$.payload[*].name")
                .expectJsonPathExists("$.payload[*].category")
                .expectJsonPathExists("$.payload[*].category.name");
    }
}