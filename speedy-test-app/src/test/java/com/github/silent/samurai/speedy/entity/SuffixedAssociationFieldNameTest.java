package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.hamcrest.Matchers.containsString;

// Repro for a NotFoundException reported against a downstream consumer's @SpeedyAssociation field
// named with an "Id" suffix (its Kotlin entity: `productId: UUID` on `Inventory`, targeting
// `Product`). The field is exposed under its literal member name ("productId"), not a
// suffix-stripped alias ("product") — there is no such stripping anywhere in the metamodel
// processor (see JpaMetaModelProcessorV2#findOutputName, which just returns member.getName()
// absent a @JsonProperty override). So filtering via "product.id" 404s, while "productId.id"
// works exactly like any other association field path. See SuffixedFkEntity for the reproducing
// entity.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SuffixedAssociationFieldNameTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void query_bySuffixStrippedFieldName_isNotFound() {
        // This is the exact shape of the reported failure: querying "product.id" against a field
        // that is actually named "productId" in the entity.
        speedyClient.query("SuffixedFkEntity")
                .where(condition("product.id", eq(UUID.randomUUID().toString())))
                .execute()
                .expectNotFound()
                .expectJsonPath("$.message", containsString("Field 'product' not found in entity SuffixedFkEntity"));
    }

    @Test
    void query_byActualFieldName_navigatesThroughAssociation() {
        String targetId = speedyClient.create("PkUuidTest")
                .field("name", "target-" + UUID.randomUUID())
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.create("SuffixedFkEntity")
                .field("productId.id", targetId)
                .execute()
                .expectOk();

        speedyClient.query("SuffixedFkEntity")
                .where(condition("productId.id", eq(targetId)))
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id");
    }
}
