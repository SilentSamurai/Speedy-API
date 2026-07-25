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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

// ConditionFactory#resolveAssociationOwningField lets an association path (e.g. "X.id") be
// addressed by the *associated entity's name*, not just the owning field's own output name --
// the same convention $expand already uses (ExpansionPathTracker matches by entity name). This
// is a fallback: the field's own name is always tried first, and it's still required when two
// fields on the same entity target the same associated entity (there's no single field to pick).
// Matching is case-insensitive on both sides (ConditionFactory and ExpansionPathTracker), so
// this stays consistent with $expand rather than diverging from it.
//
// Reuses ScalarFkEntity's fixtures (see ScalarFkAssociationTest) for the @SpeedyAssociation
// cases, since it already has both a uniquely-targeted association (catRef -> Category) and an
// ambiguous pair (ref & refByName both -> PkUuidTest). AliasedCategoryRefEntity covers the same
// fallback for a genuine JPA-native @ManyToOne whose field name isn't the target entity's name.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class AssociationNameFallbackTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private String createCategory() {
        return speedyClient.create("Category")
                .field("name", "cat-" + UUID.randomUUID())
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");
    }

    @Test
    void query_speedyAssociation_filterByUniqueAssociatedEntityName_navigatesThroughAssociation() {
        // Only ScalarFkEntity.catRef targets Category, so "Category.id" is unambiguous.
        String categoryId = createCategory();

        speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("catRef.id", categoryId)
                .execute()
                .expectOk();

        speedyClient.query("ScalarFkEntity")
                .where(condition("Category.id", eq(categoryId)))
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id");
    }

    @Test
    void query_speedyAssociation_filterByAmbiguousAssociatedEntityName_isRejected() {
        // Both ScalarFkEntity.ref and .refByName target PkUuidTest, so "PkUuidTest.id" can't
        // pick a single field -- it must be rejected rather than silently picking one.
        speedyClient.query("ScalarFkEntity")
                .where(condition("PkUuidTest.id", eq(UUID.randomUUID().toString())))
                .execute()
                .expectBadRequest()
                .expectJsonPath("$.message", allOf(
                        containsString("PkUuidTest"),
                        containsString("ref"),
                        containsString("refByName")));
    }

    @Test
    void query_speedyAssociation_filterByOwnFieldName_stillWorksAlongsideAmbiguousEntityName() {
        // Even though "PkUuidTest.id" alone is ambiguous on ScalarFkEntity, the owning field's
        // own name always resolves directly -- the fallback never shadows the primary path.
        String targetId = speedyClient.create("PkUuidTest")
                .field("name", "target-" + UUID.randomUUID())
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("ref.id", targetId)
                .execute()
                .expectOk();

        speedyClient.query("ScalarFkEntity")
                .where(condition("ref.id", eq(targetId)))
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id");
    }

    @Test
    void query_speedyAssociation_filterByAssociatedEntityName_isCaseInsensitive() {
        // Speedy entity names are PascalCase by JPA/Java convention ("Category"), but a caller
        // may reasonably type a lowercase field-style name ("category") -- both must resolve to
        // the same field, matching ExpansionPathTracker's case-insensitive $expand behavior.
        String categoryId = createCategory();

        speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("catRef.id", categoryId)
                .execute()
                .expectOk();

        speedyClient.query("ScalarFkEntity")
                .where(condition("category.id", eq(categoryId)))
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id");
    }

    @Test
    void query_nativeManyToOne_filterByAssociatedEntityName_navigatesThroughAssociation() {
        // AliasedCategoryRefEntity.cat is a plain @ManyToOne/@JoinColumn -- no @SpeedyAssociation
        // involved -- proving the fallback applies to every association kind, not just manual ones.
        String categoryId = createCategory();

        speedyClient.create("AliasedCategoryRefEntity")
                .field("cat.id", categoryId)
                .execute()
                .expectOk();

        speedyClient.query("AliasedCategoryRefEntity")
                .where(condition("Category.id", eq(categoryId)))
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].id");
    }
}
