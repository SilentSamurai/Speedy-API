package com.github.silent.samurai.speedy.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/// Test-only JSON parser turning a policy document ({@code defaultEffect} plus a {@code policies}
/// object or array) into a {@link PolicyDocument}, so policy tests can express fixtures as JSON
/// instead of hand-building {@link SpeedyPolicy} objects. Each policy uses the external
/// policy-management names: {@code id}, {@code role}, {@code effect}, {@code action},
/// {@code subject}, and {@code conditions} — parsed per-rule by {@link PolicyRuleParser}. Not part
/// of the library's public API: {@code ISpeedyConfiguration.policyPerReq()} implementations build
/// a {@link PolicyDocument} directly in Java.
public class PolicyDocumentParser {

    private final ObjectMapper objectMapper = CommonUtil.json();
    private final PolicyRuleParser ruleParser = new PolicyRuleParser();

    public PolicyDocument parse(String json) {
        try {
            return parse(objectMapper.readTree(json));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid policy document JSON: " + e.getMessage(), e);
        }
    }

    public PolicyDocument parse(JsonNode root) {
        PolicyEffect defaultEffect = parseEffect(root.path("defaultEffect").asText("Deny"));
        List<SpeedyPolicy> rules = new ArrayList<>();
        JsonNode policies = root.path("policies");
        if (policies.isArray()) {
            for (JsonNode policyNode : policies) {
                rules.add(ruleParser.parse(policyNode));
            }
        } else if (policies.isObject()) {
            rules.add(ruleParser.parse(policies));
        } else if (!policies.isMissingNode() && !policies.isNull()) {
            throw new IllegalArgumentException("'policies' must be an object or array of objects");
        }
        return new PolicyDocument(defaultEffect, rules);
    }

    private PolicyEffect parseEffect(String raw) {
        if (raw == null) {
            return PolicyEffect.DENY;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ALLOW" -> PolicyEffect.ALLOW;
            case "DENY" -> PolicyEffect.DENY;
            default -> throw new IllegalArgumentException("Unknown policy effect: '" + raw + "' (expected Allow/Deny)");
        };
    }
}
