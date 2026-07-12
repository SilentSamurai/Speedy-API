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
import com.github.silent.samurai.speedy.policy.condition.ConditionContext;
import com.github.silent.samurai.speedy.policy.condition.PolicyCondition;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.ResourceSelector;
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
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.salary")), List.of());
        PolicyEngine engine = engineOf(PolicyEffect.DENY, allowSalary);

        assertTrue(engine.isFieldAllowed(PermissionType.READ, employee, salary, null));
        assertFalse(engine.isFieldAllowed(PermissionType.READ, employee, name, null));
    }

    @Test
    void denyOverridesAllow_forTheSameFieldAndAction() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");

        SpeedyPolicy allow = new SpeedyPolicy("allow-name", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.*")), List.of());
        SpeedyPolicy deny = new SpeedyPolicy("deny-name", PolicyEffect.DENY,
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.name")), List.of());

        assertFalse(engineOf(PolicyEffect.ALLOW, allow, deny).isFieldAllowed(PermissionType.READ, employee, name, null));
        assertFalse(engineOf(PolicyEffect.ALLOW, deny, allow).isFieldAllowed(PermissionType.READ, employee, name, null));
    }

    @Test
    void defaultEffect_appliesWhenNoRuleMatches() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");

        assertTrue(engineOf(PolicyEffect.ALLOW).isFieldAllowed(PermissionType.READ, employee, name, null));
        assertFalse(engineOf(PolicyEffect.DENY).isFieldAllowed(PermissionType.READ, employee, name, null));
    }

    @Test
    void nullDefaultEffect_failsClosed() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");
        PolicyEngine engine = engineOf(null);

        assertEquals(PolicyEffect.DENY, new PolicyDocument(null, List.of()).defaultEffect());
        assertFalse(engine.isFieldAllowed(PermissionType.READ, employee, name, null));
    }

    @Test
    void abacFieldEquals_grantsOwnRowOnly() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");

        SpeedyPolicy ownsRecord = new SpeedyPolicy("user-owns-record", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ, PermissionType.UPDATE),
                List.of(ResourceSelector.parse("Employee.*")),
                List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, ownsRecord);

        SpeedyEntity ownRow = rowWithOwner(employee, "u1");
        SpeedyEntity otherRow = rowWithOwner(employee, "u2");

        assertTrue(engine.isFieldAllowed(PermissionType.READ, employee, name, ownRow));
        assertFalse(engine.isFieldAllowed(PermissionType.READ, employee, name, otherRow));
        assertTrue(engine.isFieldAllowed(PermissionType.UPDATE, employee, name, ownRow));
        assertFalse(engine.isFieldAllowed(PermissionType.UPDATE, employee, name, otherRow));
    }

    @Test
    void abacFieldEquals_literalOperand() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");

        SpeedyPolicy onlyTeamA = new SpeedyPolicy("only-team-a", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.*")),
                List.of(new QueryCondition(Map.of("ownerId", "team-a"))));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, onlyTeamA);

        assertTrue(engine.isFieldAllowed(PermissionType.READ, employee, name, rowWithOwner(employee, "team-a")));
        assertFalse(engine.isFieldAllowed(PermissionType.READ, employee, name, rowWithOwner(employee, "team-b")));
    }

    @Test
    void queryTime_conditionalAllowDoesNotGrantVisibility() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata name = employee.getField("name");
        FieldMetadata salary = employee.getField("salary");

        SpeedyPolicy ownsRecord = new SpeedyPolicy("user-owns-record", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.*")),
                List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));
        SpeedyPolicy allowSalaryUnconditional = new SpeedyPolicy("hr-can-read-salaries", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.salary")), List.of());
        PolicyEngine engine = engineOf(PolicyEffect.DENY, ownsRecord, allowSalaryUnconditional);

        assertFalse(engine.isFieldReadableUnconditional(employee, name));
        assertTrue(engine.isFieldReadableUnconditional(employee, salary));
    }

    @Test
    void entityHasAnyReadRule_countsFieldSpecificRules() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        SpeedyPolicy allowSalary = new SpeedyPolicy("allow-salary", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.salary")), List.of());

        assertTrue(engineOf(PolicyEffect.DENY, allowSalary).entityHasAnyReadRule(employee));
        assertFalse(engineOf(PolicyEffect.DENY).entityHasAnyReadRule(employee));
    }

    @Test
    void unconditionalWholeEntityDeny_blocksTheEntityGate() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        SpeedyPolicy denyAll = new SpeedyPolicy("deny-employees", PolicyEffect.DENY,
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.*")), List.of());

        assertTrue(engineOf(PolicyEffect.DENY, denyAll).isEntirelyDenied(PermissionType.READ, employee));
        assertTrue(engineOf(PolicyEffect.ALLOW, denyAll).isEntirelyDenied(PermissionType.READ, employee));
    }

    @Test
    void sampleIssuePolicy_endToEnd() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        FieldMetadata salary = employee.getField("salary");
        FieldMetadata name = employee.getField("name");

        SpeedyPolicy hrCanReadSalaries = new SpeedyPolicy("hr-can-read-salaries", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ),
                List.of(ResourceSelector.parse("Employee.salary")),
                List.of());
        SpeedyPolicy userOwnsRecord = new SpeedyPolicy("user-owns-record", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ, PermissionType.UPDATE),
                List.of(ResourceSelector.parse("Employee.*")),
                List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, hrCanReadSalaries, userOwnsRecord);

        SpeedyEntity ownRow = rowWithOwner(employee, "u1");
        SpeedyEntity otherRow = rowWithOwner(employee, "u2");

        assertTrue(engine.isFieldAllowed(PermissionType.READ, employee, salary, ownRow));
        assertTrue(engine.isFieldAllowed(PermissionType.READ, employee, salary, otherRow));

        assertTrue(engine.isFieldAllowed(PermissionType.READ, employee, name, ownRow));
        assertFalse(engine.isFieldAllowed(PermissionType.READ, employee, name, otherRow));
        assertTrue(engine.isFieldAllowed(PermissionType.UPDATE, employee, name, ownRow));
        assertFalse(engine.isFieldAllowed(PermissionType.UPDATE, employee, name, otherRow));
    }

    @Test
    void rowVisibilityConditions_unconditionalAllow_injectsNoRestriction() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        SpeedyPolicy allowSalaryUnconditional = new SpeedyPolicy("hr-can-read-salaries", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.salary")), List.of());
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
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.*")),
                List.of(new QueryCondition(Map.of("ownerId", "${principal.id}"))));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, ownsRecord);

        List<Condition> conditions = engine.rowVisibilityConditions(employee);
        assertEquals(1, conditions.size());
        BinaryCondition binaryCondition = assertInstanceOf(BinaryCondition.class, conditions.get(0));
        assertEquals("ownerId", binaryCondition.getField().getMetadataForParsing().getOutputPropertyName());
    }

    @Test
    void rowVisibilityConditions_untranslatableCondition_bailsOutToNoRestriction() throws NotFoundException {
        EntityMetadata employee = employeeMetadata();
        PolicyCondition neverTranslatable = new PolicyCondition() {
            @Override
            public String type() {
                return "AlwaysTrueButUntranslatable";
            }

            @Override
            public boolean isSatisfied(ConditionContext ctx) {
                return true;
            }
        };
        SpeedyPolicy conditional = new SpeedyPolicy("custom-condition", PolicyEffect.ALLOW,
                Set.of(PermissionType.READ), List.of(ResourceSelector.parse("Employee.*")),
                List.of(neverTranslatable));
        PolicyEngine engine = engineOf(PolicyEffect.DENY, conditional);

        assertTrue(engine.isFieldAllowed(PermissionType.READ, employee, employee.getField("name"), rowWithOwner(employee, "anyone")));
        assertTrue(engine.rowVisibilityConditions(employee).isEmpty());
    }
}
