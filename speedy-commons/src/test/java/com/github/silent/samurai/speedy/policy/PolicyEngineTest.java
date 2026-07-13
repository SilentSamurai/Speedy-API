package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEngineTest {

    private static EntityMetadata employeeMetadata() throws NotFoundException {
        EntityBuilder entity = MetadataBuilder.builder().entity("Employee");
        entity.keyField("id", "ID", ColumnType.UUID).shouldGenerateKey(true);
        entity.field("ownerId", "OWNER_ID", ColumnType.VARCHAR);
        entity.field("name", "NAME", ColumnType.VARCHAR);
        entity.field("salary", "SALARY", ColumnType.INTEGER);
        return entity.build();
    }

    private static SpeedyEntity rowWithOwner(EntityMetadata entityMetadata, String ownerId) throws NotFoundException {
        SpeedyEntity row = new SpeedyEntity(entityMetadata);
        row.put(entityMetadata.getField("ownerId"), new SpeedyText(ownerId));
        return row;
    }

    private static PolicyEngine engineOf(PolicyEffect defaultEffect, SpeedyPolicy... rules) {
        PolicyDocument document = new PolicyDocument(defaultEffect, List.of(rules));
        return new PolicyEngine(new SpeedyAuthContext(document, Map.of("principal.id", new SpeedyText("u1"))));
    }

    @Test
    void rbacAllow_grantsOnlyTheNamedField() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata salary = employee.getField("salary");
        FieldMetadata name = employee.getField("name");

        SpeedyPolicy allowSalary = new SpeedyPolicy("allow-salary", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), "Employee.salary", List.of());
        PolicyEngine engine = engineOf(PolicyEffect.DENY, allowSalary);

        assertTrue(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, salary), null) == PolicyEffect.ALLOW);
        assertFalse(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), null) == PolicyEffect.ALLOW);
    }

    @Test
    void denyOverridesAllow_forTheSameFieldAndAction() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");

        SpeedyPolicy allow = new SpeedyPolicy("allow-name", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), "Employee.*", List.of());
        SpeedyPolicy deny = new SpeedyPolicy("deny-name", PolicyEffect.DENY,
                Set.of(PermissionType.READ), "Employee.name", List.of());

        assertFalse(engineOf(PolicyEffect.ALLOW, allow, deny).isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), null) == PolicyEffect.ALLOW);
        assertFalse(engineOf(PolicyEffect.ALLOW, deny, allow).isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), null) == PolicyEffect.ALLOW);
    }

    @Test
    void defaultEffect_appliesWhenNoRuleMatches() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");

        assertTrue(engineOf(PolicyEffect.ALLOW).isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), null) == PolicyEffect.ALLOW);
        assertFalse(engineOf(PolicyEffect.DENY).isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), null) == PolicyEffect.ALLOW);
    }

    @Test
    void replacePermission_isDistinctFromUpdatePermission() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");
        SpeedyPolicy allowReplace = new SpeedyPolicy("allow-replace", PolicyEffect.ALLOW,
                Set.of(PermissionType.REPLACE), "Employee.name", List.of());
        PolicyEngine engine = engineOf(PolicyEffect.DENY, allowReplace);

        assertEquals(PolicyEffect.ALLOW,
                engine.isAuthorized(PermissionType.REPLACE, PolicyTarget.field(employee, name), null));
        assertEquals(PolicyEffect.DENY,
                engine.isAuthorized(PermissionType.UPDATE, PolicyTarget.field(employee, name), null));
    }

    @Test
    void nullDefaultEffect_failsClosed() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");
        PolicyEngine engine = engineOf(null);

        assertEquals(PolicyEffect.DENY, new PolicyDocument(null, List.of()).defaultEffect());
        assertFalse(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), null) == PolicyEffect.ALLOW);
    }

    @Test
    void abacFieldEquals_grantsOwnRowOnly() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");

        SpeedyPolicy ownsRecord = new SpeedyPolicy("user-owns-record", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ, PermissionType.UPDATE),
                "Employee.*",
                List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, ownsRecord);

        SpeedyEntity ownRow = rowWithOwner(employee, "u1");
        SpeedyEntity otherRow = rowWithOwner(employee, "u2");

        assertTrue(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), ownRow) == PolicyEffect.ALLOW);
        assertFalse(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), otherRow) == PolicyEffect.ALLOW);
        assertTrue(engine.isAuthorized(PermissionType.UPDATE, PolicyTarget.field(employee, name), ownRow) == PolicyEffect.ALLOW);
        assertFalse(engine.isAuthorized(PermissionType.UPDATE, PolicyTarget.field(employee, name), otherRow) == PolicyEffect.ALLOW);
    }

    @Test
    void abacFieldEquals_literalOperand() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");

        SpeedyPolicy onlyTeamA = new SpeedyPolicy("only-team-a", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), "Employee.*",
                List.of(new QueryCondition(Map.of("ownerId", "team-a"))));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, onlyTeamA);

        assertTrue(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), rowWithOwner(employee, "team-a")) == PolicyEffect.ALLOW);
        assertFalse(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), rowWithOwner(employee, "team-b")) == PolicyEffect.ALLOW);
    }

    @Test
    void queryTime_conditionalAllowDoesNotGrantVisibility() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");
        FieldMetadata salary = employee.getField("salary");

        SpeedyPolicy ownsRecord = new SpeedyPolicy("user-owns-record", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), "Employee.*",
                List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));
        SpeedyPolicy allowSalaryUnconditional = new SpeedyPolicy("hr-can-read-salaries", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), "Employee.salary", List.of());
        PolicyEngine engine = engineOf(PolicyEffect.DENY, ownsRecord, allowSalaryUnconditional);

        assertFalse(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), null) == PolicyEffect.ALLOW);
        assertTrue(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, salary), null) == PolicyEffect.ALLOW);
    }

    @Test
    void unconditionalWholeEntityDeny_blocksTheEntityGate() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        SpeedyPolicy denyAll = new SpeedyPolicy("deny-employees", PolicyEffect.DENY,
                Set.of(PermissionType.READ), "Employee.*", List.of());

        assertTrue(engineOf(PolicyEffect.DENY, denyAll).isAuthorized(PermissionType.READ, PolicyTarget.entity(employee)) == PolicyEffect.DENY);
        assertTrue(engineOf(PolicyEffect.ALLOW, denyAll).isAuthorized(PermissionType.READ, PolicyTarget.entity(employee)) == PolicyEffect.DENY);
    }

    @Test
    void fieldSpecificUnconditionalDeny_doesNotBlockTheEntityGate() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        SpeedyPolicy denySalaryOnly = new SpeedyPolicy("deny-salary", PolicyEffect.DENY,
                Set.of(PermissionType.READ), "Employee.salary", List.of());

        assertFalse(engineOf(PolicyEffect.ALLOW, denySalaryOnly).isAuthorized(PermissionType.READ, PolicyTarget.entity(employee)) == PolicyEffect.DENY);
    }

    @Test
    void fieldSpecificAllow_preventsTheEntityGateFromBlocking() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        SpeedyPolicy allowSalaryOnly = new SpeedyPolicy("allow-salary", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), "Employee.salary", List.of());

        assertFalse(engineOf(PolicyEffect.DENY, allowSalaryOnly).isAuthorized(PermissionType.READ, PolicyTarget.entity(employee)) == PolicyEffect.DENY);
    }

    @Test
    void sampleIssuePolicy_endToEnd() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata salary = employee.getField("salary");
        FieldMetadata name = employee.getField("name");

        SpeedyPolicy hrCanReadSalaries = new SpeedyPolicy("hr-can-read-salaries", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ),
                "Employee.salary",
                List.of());
        SpeedyPolicy userOwnsRecord = new SpeedyPolicy("user-owns-record", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ, PermissionType.UPDATE),
                "Employee.*",
                List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, hrCanReadSalaries, userOwnsRecord);

        SpeedyEntity ownRow = rowWithOwner(employee, "u1");
        SpeedyEntity otherRow = rowWithOwner(employee, "u2");

        assertTrue(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, salary), ownRow) == PolicyEffect.ALLOW);
        assertTrue(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, salary), otherRow) == PolicyEffect.ALLOW);

        assertTrue(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), ownRow) == PolicyEffect.ALLOW);
        assertFalse(engine.isAuthorized(PermissionType.READ, PolicyTarget.field(employee, name), otherRow) == PolicyEffect.ALLOW);
        assertTrue(engine.isAuthorized(PermissionType.UPDATE, PolicyTarget.field(employee, name), ownRow) == PolicyEffect.ALLOW);
        assertFalse(engine.isAuthorized(PermissionType.UPDATE, PolicyTarget.field(employee, name), otherRow) == PolicyEffect.ALLOW);
    }

    @Test
    void rowVisibilityConditions_unconditionalAllow_injectsNoRestriction() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        SpeedyPolicy allowSalaryUnconditional = new SpeedyPolicy("hr-can-read-salaries", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), "Employee.salary", List.of());
        PolicyEngine engine = engineOf(PolicyEffect.DENY, allowSalaryUnconditional);

        assertTrue(engine.rowVisibilityConditions(employee).isEmpty());
    }

    @Test
    void rowVisibilityConditions_noReadAccessAtAll_injectsNoRestriction() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        assertTrue(engineOf(PolicyEffect.DENY).rowVisibilityConditions(employee).isEmpty());
    }

    @Test
    void rowVisibilityConditions_conditionalAllow_translatesToFieldEqualsCondition() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        SpeedyPolicy ownsRecord = new SpeedyPolicy("user-owns-record", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), "Employee.*",
                List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, ownsRecord);

        List<Condition> conditions = engine.rowVisibilityConditions(employee);
        assertEquals(1, conditions.size());
        BinaryCondition binaryCondition = assertInstanceOf(BinaryCondition.class, conditions.get(0));
        assertEquals("ownerId", binaryCondition.getField().getMetadataForParsing().getOutputPropertyName());
    }

    @Test
    void rowVisibilityConditions_unresolvableVariable_bailsOutToNoRestriction() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        SpeedyPolicy conditional = new SpeedyPolicy("custom-condition", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), "Employee.*",
                List.of(new QueryCondition(Map.of("ownerId", "${principal.missing}"))));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, conditional);

        assertTrue(engine.rowVisibilityConditions(employee).isEmpty());
    }
}
