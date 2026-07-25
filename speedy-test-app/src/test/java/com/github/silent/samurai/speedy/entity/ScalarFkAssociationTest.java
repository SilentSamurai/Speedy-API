package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

// Covers ScalarFkEntity's plain scalar columns (no @ManyToOne/@JoinColumn) wired as Speedy
// associations purely via @SpeedyAssociation: by target class (ref), by target entity name
// (refByName), and against target primary keys of different types/generation strategies
// (catRef -> Category.id: String/GenerationType.UUID; numRef -> AutoGenIdentityEntity.id:
// Long/GenerationType.IDENTITY) to confirm read/write conversion isn't UUID-specific.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ScalarFkAssociationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SpeedyFactory speedyFactory;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private String createTarget() {
        return speedyClient.create("PkUuidTest")
                .field("name", "target-" + UUID.randomUUID())
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");
    }

    // Category.id: String, GenerationType.UUID.
    private String createCategoryTarget() {
        return speedyClient.create("Category")
                .field("name", "cat-" + UUID.randomUUID())
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");
    }

    // AutoGenIdentityEntity.id: Long, GenerationType.IDENTITY (DB-assigned).
    private long createIdentityTarget() {
        Object rawId = speedyClient.create("AutoGenIdentityEntity")
                .field("name", "num-target-" + UUID.randomUUID())
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id", Object.class);
        return Long.parseLong(String.valueOf(rawId));
    }

    @Test
    void metamodel_scalarUuidField_isWiredAsAssociation() throws NotFoundException {
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata field = metaModel.findFieldMetadata("ScalarFkEntity", "ref");

        assertThat(field.isAssociation(), is(true));
        assertThat(field.getAssociationMetadata().getName(), is("PkUuidTest"));
        assertThat(field.getAssociatedFieldMetadata().getOutputPropertyName(), is("id"));
    }

    @Test
    void metamodel_entityNameForm_isWiredAsAssociation() throws NotFoundException {
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata field = metaModel.findFieldMetadata("ScalarFkEntity", "refByName");

        assertThat(field.isAssociation(), is(true));
        assertThat(field.getAssociationMetadata().getName(), is("PkUuidTest"));
        assertThat(field.getAssociatedFieldMetadata().getOutputPropertyName(), is("id"));
    }

    @Test
    void create_withEntityNameFormField_roundTripsAsKeysOnlyReference() {
        String targetId = createTarget();

        String sourceId = speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("refByName.id", targetId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.get("ScalarFkEntity")
                .key("id", sourceId)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].refByName.id", targetId);
    }

    @Test
    void metamodel_stringPkTargetField_isWiredAsAssociation() throws NotFoundException {
        // Category.id is a String primary key (GenerationType.UUID), not java.util.UUID —
        // confirms association resolution isn't tied to the target's key being a UUID type.
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata field = metaModel.findFieldMetadata("ScalarFkEntity", "catRef");

        assertThat(field.isAssociation(), is(true));
        assertThat(field.getAssociationMetadata().getName(), is("Category"));
        assertThat(field.getAssociatedFieldMetadata().getOutputPropertyName(), is("id"));
    }

    @Test
    void metamodel_numericPkTargetField_isWiredAsAssociation() throws NotFoundException {
        // AutoGenIdentityEntity.id is a DB-assigned Long primary key.
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata field = metaModel.findFieldMetadata("ScalarFkEntity", "numRef");

        assertThat(field.isAssociation(), is(true));
        assertThat(field.getAssociationMetadata().getName(), is("AutoGenIdentityEntity"));
        assertThat(field.getAssociatedFieldMetadata().getOutputPropertyName(), is("id"));
    }

    @Test
    void create_withStringPkTarget_roundTrips() {
        String targetId = createCategoryTarget();

        String sourceId = speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("catRef.id", targetId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.get("ScalarFkEntity")
                .key("id", sourceId)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].catRef.id", targetId);
    }

    @Test
    void create_withNumericPkTarget_roundTrips() {
        long targetId = createIdentityTarget();

        String sourceId = speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("numRef.id", targetId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        Object actual = speedyClient.get("ScalarFkEntity")
                .key("id", sourceId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].numRef.id", Object.class);

        assertThat(Long.parseLong(String.valueOf(actual)), is(targetId));
    }

    @Test
    void create_withNestedObjectPayload_roundTripsAsKeysOnlyReference() {
        String targetId = createTarget();

        SpeedyTestResult createResponse = speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("ref.id", targetId)
                .execute()
                .expectOk();

        String sourceId = createResponse.jsonPath("$.payload[0].id");

        speedyClient.get("ScalarFkEntity")
                .key("id", sourceId)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].ref.id", targetId)
                .expectJsonPathDoesNotExist("$.payload[0].ref.name");
    }

    @Test
    void create_withBareScalarPayload_isRejected() {
        // Association fields are rejected as soon as the request body is deserialized
        // (StructureToSpeedy), before AssociationRule even runs.
        speedyClient.create("ScalarFkEntity")
                .field("name", "bad-" + UUID.randomUUID())
                .field("ref", UUID.randomUUID().toString())
                .execute()
                .expectBadRequest()
                .expectJsonPath("$.message", containsString("must be an object"));
    }

    @Test
    void query_withExpand_returnsFullyNestedObject() {
        // $expand is only honored on the $query path (DefaultQueryProcessor.executeManyWithCount);
        // GET-by-key (fetchByKey) hardcodes an empty expand set regardless of association kind.
        // Per ExpansionPathTracker, $expand entries are matched by the associated entity's name
        // ("PkUuidTest"), not the field's output property name ("ref") — same as every other
        // association in this codebase (see SpeedyV2ExpandTest).
        String targetId = createTarget();

        String sourceId = speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("ref.id", targetId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.query("ScalarFkEntity")
                .where(condition("id", eq(sourceId)))
                .expand("PkUuidTest")
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload[0].ref.name");
    }

    @Test
    void query_filterByAssociatedField_navigatesThroughAssociation() {
        String targetId = createTarget();

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
    void update_reassigningAssociationField_changesReference() {
        String firstTargetId = createTarget();
        String secondTargetId = createTarget();

        String sourceId = speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("ref.id", firstTargetId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.update("ScalarFkEntity")
                .key("id", sourceId)
                .field("ref.id", secondTargetId)
                .execute()
                .expectOk();

        speedyClient.get("ScalarFkEntity")
                .key("id", sourceId)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].ref.id", secondTargetId);
    }

    @Test
    void update_withNumericPkTarget_reassignsReference() {
        // Confirms the update path also drives the write conversion through the *target* key
        // field's type (Long/IDENTITY here), same as create.
        long firstTargetId = createIdentityTarget();
        long secondTargetId = createIdentityTarget();

        String sourceId = speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("numRef.id", firstTargetId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.update("ScalarFkEntity")
                .key("id", sourceId)
                .field("numRef.id", secondTargetId)
                .execute()
                .expectOk();

        Object actual = speedyClient.get("ScalarFkEntity")
                .key("id", sourceId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].numRef.id", Object.class);

        assertThat(Long.parseLong(String.valueOf(actual)), is(secondTargetId));
    }

    @Test
    void update_withBareScalarPayload_isRejected() {
        // Same StructureToSpeedy-level rejection as create_withBareScalarPayload_isRejected —
        // association fields must be sent as a nested object on update too.
        String targetId = createTarget();

        String sourceId = speedyClient.create("ScalarFkEntity")
                .field("name", "source-" + UUID.randomUUID())
                .field("ref.id", targetId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.update("ScalarFkEntity")
                .key("id", sourceId)
                .field("ref", UUID.randomUUID().toString())
                .execute()
                .expectBadRequest()
                .expectJsonPath("$.message", containsString("must be an object"));
    }
}
