package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyDate;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.silent.samurai.speedy.policy.PolicyConditions.fieldEquals;
import static com.github.silent.samurai.speedy.policy.PolicyConditions.variable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyBuilderTest {

    @Test
    void denyByDefault_buildsContextWithVariablesAndOrderedRules() {
        SpeedyAuthContext context = PolicyBuilder.denyByDefault()
                .principalId("user-1")
                .variable("principal.department", "HR")
                .allow("read-own-invoices", PermissionType.READ, "Invoice.*",
                        fieldEquals("ownerId", variable("principal.id")))
                .allow("update-draft-invoices", PermissionType.UPDATE, "Invoice.status",
                        new QueryCondition(Map.of("status", Map.of("$in", List.of("DRAFT", "REVIEW")))))
                .deny("deny-invoice-amount", PermissionType.READ, "Invoice.amount")
                .build();

        assertEquals(PolicyEffect.DENY, context.document().defaultEffect());
        assertEquals(new SpeedyText("user-1"), context.variables().get("principal.id"));
        assertEquals(new SpeedyText("HR"), context.variables().get("principal.department"));

        List<SpeedyPolicy> rules = context.document().speedyPolicies();
        assertEquals(List.of("read-own-invoices", "update-draft-invoices", "deny-invoice-amount"),
                rules.stream().map(SpeedyPolicy::id).toList());
        assertEquals(Set.of(PermissionType.UPDATE), rules.get(1).action());
        assertEquals(PolicyEffect.DENY, rules.get(2).effect());
        assertTrue(rules.get(0).conditions().size() == 1);
        assertTrue(rules.get(1).conditions().size() == 1);
    }

    @Test
    void allowByDefault_buildsAllowByDefaultDocument() {
        assertEquals(PolicyEffect.ALLOW, PolicyBuilder.allowByDefault().buildDocument().defaultEffect());
    }

    /// Plain Java values in, {@code SpeedyValue}s out: the conversion the caller no longer has to do.
    @Test
    void variable_convertsEverySupportedPlainJavaType() {
        SpeedyAuthContext context = PolicyBuilder.denyByDefault()
                .variable("principal.id", "user-1")
                .variable("principal.level", 3L)
                .variable("principal.active", true)
                .variable("principal.hiredOn", LocalDate.of(2020, 1, 2))
                .variable("principal.department", null)
                .build();

        assertEquals(new SpeedyText("user-1"), context.variables().get("principal.id"));
        assertEquals(new SpeedyInt(3L), context.variables().get("principal.level"));
        assertEquals(new SpeedyBoolean(true), context.variables().get("principal.active"));
        assertEquals(new SpeedyDate(LocalDate.of(2020, 1, 2)), context.variables().get("principal.hiredOn"));
        assertTrue(context.variables().get("principal.department").isNull());
    }

    @Test
    void variable_stillAcceptsASpeedyValueDirectly() {
        SpeedyAuthContext context = PolicyBuilder.denyByDefault()
                .variable("principal.id", new SpeedyText("user-1"))
                .build();

        assertEquals(new SpeedyText("user-1"), context.variables().get("principal.id"));
    }

    @Test
    void variable_rejectsAnUnsupportedTypeAndNamesTheVariable() {
        PolicyBuilder builder = PolicyBuilder.denyByDefault();

        ConversionException thrown = assertThrows(ConversionException.class,
                () -> builder.variable("principal.balance", BigDecimal.ONE));
        assertTrue(thrown.getMessage().contains("principal.balance"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("BigDecimal"), thrown.getMessage());
    }

    /// The shared-document pattern: one immutable document built once, combined with the current
    /// caller's variables per request.
    @Test
    void from_adoptsAnExistingDocumentAndPairsItWithCallerVariables() {
        PolicyDocument shared = new PolicyDocument(PolicyEffect.DENY, List.of(
                new SpeedyPolicy("read-invoices", PolicyEffect.ALLOW,
                        Set.of(PermissionType.READ), "Invoice.*", List.of())));

        SpeedyAuthContext context = PolicyBuilder.from(shared).principalId("user-1").build();

        assertEquals(PolicyEffect.DENY, context.document().defaultEffect());
        assertEquals(List.of("read-invoices"),
                context.document().speedyPolicies().stream().map(SpeedyPolicy::id).toList());
        assertEquals(new SpeedyText("user-1"), context.variables().get("principal.id"));
    }

    @Test
    void build_snapshotsVariables_soLaterBuilderUseCannotMutateAnIssuedContext() {
        PolicyBuilder builder = PolicyBuilder.denyByDefault().principalId("user-1");
        SpeedyAuthContext issued = builder.build();

        builder.variable("principal.id", "user-2");

        assertEquals(new SpeedyText("user-1"), issued.variables().get("principal.id"));
    }
}
