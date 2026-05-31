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

import java.util.List;

import static org.hamcrest.Matchers.containsString;

/// Tests the four default FieldRule implementations: TypeCompatibilityRule,
/// NonAssociationObjectRule, CollectionRule, and AssociationRule.
///
/// ## What we test
/// - TypeCompatibilityRule: rejects string values for numeric fields (INT, FLOAT)
/// - NonAssociationObjectRule: rejects nested objects on non-association fields
/// - CollectionRule: rejects array values on scalar fields
/// - AssociationRule: rejects empty/null keys on association fields
///
/// ## How we test
/// Integration tests via SpeedyTest client that calls CREATE endpoints and
/// asserts 400 Bad Request with specific error messages.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class DefaultFieldValidatorRulesIT {

    @Autowired
    MockMvc mockMvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @Nested
    @DisplayName("TypeCompatibilityRule")
    class TypeCompatibilityRuleTests {

        @Test
        @DisplayName("CREATE with string value for integer field should fail")
        void stringValueForIntegerField() {
            client.create("Company")
                    .field("name", "TestCo")
                    .field("address", "123 Test St")
                    .field("phone", "555-0001")
                    .field("currency", "USD")
                    .field("invoiceNo", "not-a-number")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("expects type INT"));
        }

        @Test
        @DisplayName("CREATE with string value for double field should fail")
        void stringValueForDoubleField() {
            client.create("Invoice")
                    .field("customer.id", "1")
                    .field("paid", "not-a-double")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("expects type FLOAT"));
        }
    }

    @Nested
    @DisplayName("NonAssociationObjectRule")
    class NonAssociationObjectRuleTests {

        @Test
        @DisplayName("CREATE with nested object for non-association field should fail")
        void nestedObjectForNonAssociationField() {
            String uniqueCat = "cat-obj-" + java.util.UUID.randomUUID();
            SpeedyTestResult catResult = client.create("Category")
                    .field("name", uniqueCat)
                    .execute()
                    .expectOk();
            String categoryId = catResult.jsonPath("$.payload[0].id");

            client.create("Product")
                    .field("name", "prod-obj-" + java.util.UUID.randomUUID())
                    .field("category.id", categoryId)
                    .field("description.nested", "value")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("not an association field"));
        }
    }

    @Nested
    @DisplayName("CollectionRule")
    class CollectionRuleTests {

        @Test
        @DisplayName("CREATE with array value for scalar field should fail")
        void arrayValueForScalarField() {
            String uniqueCat = "cat-coll-" + java.util.UUID.randomUUID();
            SpeedyTestResult catResult = client.create("Category")
                    .field("name", uniqueCat)
                    .execute()
                    .expectOk();
            String categoryId = catResult.jsonPath("$.payload[0].id");

            client.create("Product")
                    .field("name", "prod-coll-" + java.util.UUID.randomUUID())
                    .field("category.id", categoryId)
                    .field("description", List.of("v1", "v2"))
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("should not be a collection"));
        }
    }

    @Nested
    @DisplayName("AssociationRule")
    class AssociationRuleTests {

        @Test
        @DisplayName("CREATE with empty key for association field should fail")
        void emptyKeyForAssociation() {
            client.create("Product")
                    .field("name", "prod-assoc-" + java.util.UUID.randomUUID())
                    .field("category.id", "")
                    .execute()
                    .expectBadRequest()
                    .expectJsonPath("$.message", containsString("cannot be null or empty"));
        }

        @Test
        @DisplayName("CREATE with valid association should succeed")
        void validAssociation() {
            String uniqueCat = "cat-valid-" + java.util.UUID.randomUUID();
            SpeedyTestResult catResult = client.create("Category")
                    .field("name", uniqueCat)
                    .execute()
                    .expectOk();
            String categoryId = catResult.jsonPath("$.payload[0].id");

            client.create("Product")
                    .field("name", "prod-valid-" + java.util.UUID.randomUUID())
                    .field("category.id", categoryId)
                    .execute()
                    .expectOk();
        }
    }
}
