package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.github.silent.samurai.speedy.policy.PolicyConditions.fieldEquals;
import static com.github.silent.samurai.speedy.policy.PolicyConditions.fieldIn;
import static com.github.silent.samurai.speedy.policy.PolicyConditions.variable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyBuilderTest {

    @Test
    void denyByDefault_buildsContextWithVariablesAndOrderedRules() {
        SpeedyAuthContext context = PolicyBuilder.denyByDefault()
                .principalId("user-1")
                .variable("principal.department", new SpeedyText("HR"))
                .allow("read-own-invoices", PermissionType.READ, "Invoice.*",
                        fieldEquals("ownerId", variable("principal.id")))
                .allow("update-draft-invoices", Set.of(PermissionType.READ, PermissionType.UPDATE),
                        "Invoice.status", fieldIn("status", "DRAFT", "REVIEW"))
                .deny("deny-invoice-amount", PermissionType.READ, "Invoice.amount")
                .build();

        assertEquals(PolicyEffect.DENY, context.document().defaultEffect());
        assertEquals(new SpeedyText("user-1"), context.variables().get("principal.id"));
        assertEquals(new SpeedyText("HR"), context.variables().get("principal.department"));

        List<SpeedyPolicy> rules = context.document().speedyPolicies();
        assertEquals(List.of("read-own-invoices", "update-draft-invoices", "deny-invoice-amount"),
                rules.stream().map(SpeedyPolicy::id).toList());
        assertEquals(Set.of(PermissionType.READ, PermissionType.UPDATE), rules.get(1).action());
        assertEquals(PolicyEffect.DENY, rules.get(2).effect());
        assertTrue(rules.get(0).conditions().size() == 1);
        assertTrue(rules.get(1).conditions().size() == 1);
    }

    @Test
    void allowByDefault_buildsAllowByDefaultDocument() {
        assertEquals(PolicyEffect.ALLOW, PolicyBuilder.allowByDefault().buildDocument().defaultEffect());
    }
}
