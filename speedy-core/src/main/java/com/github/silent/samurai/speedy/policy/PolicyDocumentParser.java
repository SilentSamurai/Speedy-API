package com.github.silent.samurai.speedy.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.policy.condition.ConditionSpec;
import com.github.silent.samurai.speedy.policy.condition.PolicyCondition;
import com.github.silent.samurai.speedy.policy.condition.PolicyConditionFactory;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import com.github.silent.samurai.speedy.policy.model.ResourceSelector;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Convenience JSON parser turning a policy document ({@code defaultEffect} plus a {@code policies}
/// object or array) into a {@link PolicyDocument}. Each policy uses the external policy-management
/// names: {@code id}, {@code role}, {@code effect}, {@code action}, {@code subject}, and
/// {@code conditions}. Purely a helper — an {@code ISpeedyConfiguration.policyPerReq()}
/// implementation that already builds a {@link PolicyDocument} directly never needs this class.
public class PolicyDocumentParser {

    private final ObjectMapper objectMapper = CommonUtil.json();
    private final PolicyConditionRegistry conditionRegistry;

    public PolicyDocumentParser(PolicyConditionRegistry conditionRegistry) {
        this.conditionRegistry = conditionRegistry;
    }

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
                rules.add(parseRule(policyNode));
            }
        } else if (policies.isObject()) {
            rules.add(parseRule(policies));
        } else if (!policies.isMissingNode() && !policies.isNull()) {
            throw new IllegalArgumentException("'policies' must be an object or array of objects");
        }
        return new PolicyDocument(defaultEffect, rules);
    }

    private SpeedyPolicy parseRule(JsonNode node) {
        String id = text(node, "id", "name");
        String role = text(node, "role");
        PolicyEffect effect = parseEffect(node.path("effect").asText("Deny"));
        Set<PermissionType> action = parseActions(field(node, "action", "actions"));
        List<ResourceSelector> subject = parseSubjects(field(node, "subject", "resources"));
        List<PolicyCondition> conditions = parseConditions(node.path("conditions"));
        return new SpeedyPolicy(id, role, effect, action, subject, conditions);
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

    private Set<PermissionType> parseActions(JsonNode node) {
        Set<PermissionType> actions = new LinkedHashSet<>();
        if (node.isTextual()) {
            addAction(actions, node.asText());
        } else if (node.isArray()) {
            for (JsonNode actionNode : node) {
                if (!actionNode.isTextual()) {
                    throw new IllegalArgumentException("Each 'action' entry must be a string");
                }
                addAction(actions, actionNode.asText());
            }
        } else if (!node.isMissingNode() && !node.isNull()) {
            throw new IllegalArgumentException("'action' must be a string or array of strings");
        }
        return actions;
    }

    private void addAction(Set<PermissionType> actions, String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("MANAGE".equals(normalized)) {
            actions.addAll(Set.of(PermissionType.CREATE, PermissionType.READ,
                    PermissionType.UPDATE, PermissionType.DELETE));
            return;
        }
        try {
            actions.add(PermissionType.valueOf(normalized));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown policy action: '" + raw + "'", e);
        }
    }

    private List<ResourceSelector> parseSubjects(JsonNode node) {
        List<ResourceSelector> subjects = new ArrayList<>();
        if (node.isTextual()) {
            subjects.add(ResourceSelector.parse(node.asText()));
        } else if (node.isArray()) {
            for (JsonNode subjectNode : node) {
                if (!subjectNode.isTextual()) {
                    throw new IllegalArgumentException("Each 'subject' entry must be a string");
                }
                subjects.add(ResourceSelector.parse(subjectNode.asText()));
            }
        } else if (!node.isMissingNode() && !node.isNull()) {
            throw new IllegalArgumentException("'subject' must be a string or array of strings");
        }
        return subjects;
    }

    private List<PolicyCondition> parseConditions(JsonNode node) {
        List<PolicyCondition> conditions = new ArrayList<>();
        if (node.isObject()) {
            conditions.add(parseCondition(node));
        } else if (node.isArray()) {
            for (JsonNode conditionNode : node) {
                if (!conditionNode.isObject()) {
                    throw new IllegalArgumentException("Each 'conditions' entry must be an object");
                }
                conditions.add(parseCondition(conditionNode));
            }
        } else if (!node.isMissingNode() && !node.isNull()) {
            throw new IllegalArgumentException("'conditions' must be an object, array of objects, or null");
        }
        return conditions;
    }

    private static JsonNode field(JsonNode node, String preferred, String legacy) {
        return node.has(preferred) ? node.path(preferred) : node.path(legacy);
    }

    private static String text(JsonNode node, String preferred, String... legacy) {
        JsonNode value = node.path(preferred);
        if (value.isMissingNode()) {
            for (String key : legacy) {
                value = node.path(key);
                if (!value.isMissingNode()) {
                    break;
                }
            }
        }
        return value.isValueNode() && !value.isNull() ? value.asText() : null;
    }

    private PolicyCondition parseCondition(JsonNode node) {
        String type = node.path("type").asText(null);
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Policy condition is missing 'type': " + node);
        }
        PolicyConditionFactory factory = conditionRegistry.get(type)
                .orElseThrow(() -> new IllegalArgumentException("Unknown policy condition type: '" + type + "'"));
        Map<String, Object> raw = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
        });
        return factory.parse(new ConditionSpec(raw));
    }
}
