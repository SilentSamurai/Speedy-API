package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import org.junit.jupiter.api.Test;

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
                  "subject": "Employee.salary"
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

    private final PolicyDocumentParser parser = new PolicyDocumentParser();

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
        assertEquals("Employee.salary", hrRule.getResource());
        assertTrue(hrRule.conditions().isEmpty());

        SpeedyPolicy ownsRule = document.speedyPolicies().get(1);
        assertEquals("user-owns-record", ownsRule.getName());
        assertEquals("employee-role-id", ownsRule.role());
        assertEquals(Set.of(PermissionType.READ, PermissionType.UPDATE), ownsRule.getActions());
        assertEquals("Employee", ownsRule.getResource());
        assertEquals(1, ownsRule.conditions().size());

        assertInstanceOf(QueryCondition.class, ownsRule.conditions().get(0));
    }

    @Test
    void singlePolicyAndConditionArraysAreAccepted() {
        String json = """
                {"defaultEffect":"Deny","policies":{"id":"manage-employees","role":"admin-role",
                "effect":"Allow","action":"manage","subject":"Employee",
                "conditions":[{"type":"QueryCondition","ownerId":"${principal.id}"}]}}
                """;

        SpeedyPolicy rule = parser.parse(json).speedyPolicies().get(0);
        assertEquals(Set.of(PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE,
                        PermissionType.REPLACE, PermissionType.DELETE),
                rule.getActions());
        assertEquals("Employee", rule.getResource());
        assertEquals(1, rule.conditions().size());
    }

    @Test
    void subjectArrayIsRejected() {
        String json = """
                {"defaultEffect":"Deny","policies":{"id":"read-employees","effect":"Allow",
                "action":"read","subject":["Employee.name","Employee.salary"]}}
                """;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parser.parse(json));

        assertEquals("'subject' must be a string", exception.getMessage());
    }
}
