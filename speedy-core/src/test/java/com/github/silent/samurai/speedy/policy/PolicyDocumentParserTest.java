package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.policy.condition.ConditionContext;
import com.github.silent.samurai.speedy.policy.condition.ConditionSpec;
import com.github.silent.samurai.speedy.policy.condition.PolicyCondition;
import com.github.silent.samurai.speedy.policy.condition.PolicyConditionFactory;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.ResourceSelector;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyDocumentParserTest {

    private static final String SAMPLE_JSON = """
            {
              "defaultEffect": "Deny",
              "policies": [
                {
                  "id": "hr-can-read-salaries",
                  "role": "hr-role-id",
                  "effect": "Allow",
                  "action": "read",
                  "subject": ["Employee.salary", "Employee.bonus"]
                },
                {
                  "id": "user-owns-record",
                  "role": "employee-role-id",
                  "effect": "Allow",
                  "action": ["read", "update"],
                  "subject": "Employee",
                  "conditions": { "type": "QueryCondition", "ownerId": "${principal.id}" }
                }
              ]
            }
            """;

    private final PolicyConditionRegistry registry = new PolicyConditionRegistry();
    private final PolicyDocumentParser parser = new PolicyDocumentParser(registry);

    @Test
    void parsesSampleIssuePolicy() {
        PolicyDocument document = parser.parse(SAMPLE_JSON);

        assertEquals(PolicyEffect.DENY, document.defaultEffect());
        assertEquals(2, document.speedyPolicies().size());

        SpeedyPolicy hrRule = document.speedyPolicies().get(0);
        assertEquals("hr-can-read-salaries", hrRule.getName());
        assertEquals("hr-role-id", hrRule.role());
        assertEquals(PolicyEffect.ALLOW, hrRule.effect());
        assertEquals(Set.of(PermissionType.READ), hrRule.getActions());
        assertEquals(List.of(ResourceSelector.parse("Employee.salary"), ResourceSelector.parse("Employee.bonus")),
                hrRule.getResources());
        assertTrue(hrRule.conditions().isEmpty());

        SpeedyPolicy ownsRule = document.speedyPolicies().get(1);
        assertEquals("user-owns-record", ownsRule.getName());
        assertEquals("employee-role-id", ownsRule.role());
        assertEquals(Set.of(PermissionType.READ, PermissionType.UPDATE), ownsRule.getActions());
        assertEquals(List.of(ResourceSelector.parse("Employee")), ownsRule.getResources());
        assertEquals(1, ownsRule.conditions().size());

        QueryCondition condition = assertInstanceOf(QueryCondition.class, ownsRule.conditions().get(0));
        assertEquals("QueryCondition", condition.type());
    }

    @Test
    void unknownConditionType_isRejectedWithAClearMessage() {
        String json = """
                {"defaultEffect":"Deny","policies":{"id":"x","role":"r","effect":"Allow","action":"READ",
                "subject":"Employee","conditions":{"type":"NoSuchCondition"}}}
                """;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parser.parse(json));
        assertTrue(ex.getMessage().contains("NoSuchCondition"));
    }

    @Test
    void singlePolicyAndConditionArraysAreAccepted() {
        String json = """
                {"defaultEffect":"Deny","policies":{"id":"manage-employees","role":"admin-role",
                "effect":"Allow","action":"manage","subject":"Employee",
                "conditions":[{"type":"QueryCondition","ownerId":"${principal.id}"}]}}
                """;

        SpeedyPolicy rule = parser.parse(json).speedyPolicies().get(0);
        assertEquals(Set.of(PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE, PermissionType.DELETE),
                rule.getActions());
        assertEquals(List.of(ResourceSelector.parse("Employee")), rule.getResources());
        assertEquals(1, rule.conditions().size());
    }

    @Test
    void newConditionType_integratesWithoutParserOrEngineChanges() {
        registry.register(new PolicyConditionFactory() {
            @Override
            public String type() {
                return "AlwaysAllow";
            }

            @Override
            public PolicyCondition parse(ConditionSpec spec) {
                return new PolicyCondition() {
                    @Override
                    public String type() {
                        return "AlwaysAllow";
                    }

                    @Override
                    public boolean isSatisfied(ConditionContext ctx) {
                        return true;
                    }
                };
            }
        });

        String json = """
                {"defaultEffect":"Deny","policies":[{"id":"x","role":"r","effect":"Allow","action":"READ",
                "subject":"Employee","conditions":{"type":"AlwaysAllow"}}]}
                """;

        PolicyDocument document = parser.parse(json);
        assertEquals("AlwaysAllow", document.speedyPolicies().get(0).conditions().get(0).type());
    }
}
