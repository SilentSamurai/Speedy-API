package com.github.silent.samurai.speedy.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Test-only helper (used by {@link PolicyDocumentParser}): parses a single policy-rule JSON node
/// — {@code id}, {@code role}, {@code effect}, {@code action}, {@code subject}, {@code conditions}
/// — into a {@link SpeedyPolicy}. Subject selectors ({@code "Entity"}, {@code "Entity.field"},
/// {@code "Entity.*"}) are validated here (blank/trailing-dot tokens rejected) and kept as plain
/// strings; {@link com.github.silent.samurai.speedy.policy.PolicyEngine} owns the matching logic
/// against those strings at request time. Each {@code conditions[]} entry is a
/// {@link QueryCondition} spec — there is only one condition kind, so no type discriminator or
/// factory lookup is needed.
public class PolicyRuleParser {

    private final ObjectMapper objectMapper = CommonUtil.json();

    public SpeedyPolicy parse(JsonNode node) {
        String id = text(node, "id", "name");
        String role = text(node, "role");
        PolicyEffect effect = parseEffect(node.path("effect").asText("Deny"));
        Set<PermissionType> action = parseActions(field(node, "action", "actions"));
        List<String> subject = parseSubjects(field(node, "subject", "resources"));
        List<QueryCondition> conditions = parseConditions(node.path("conditions"));
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

    private List<String> parseSubjects(JsonNode node) {
        List<String> subjects = new ArrayList<>();
        if (node.isTextual()) {
            subjects.add(parseSubjectToken(node.asText()));
        } else if (node.isArray()) {
            for (JsonNode subjectNode : node) {
                if (!subjectNode.isTextual()) {
                    throw new IllegalArgumentException("Each 'subject' entry must be a string");
                }
                subjects.add(parseSubjectToken(subjectNode.asText()));
            }
        } else if (!node.isMissingNode() && !node.isNull()) {
            throw new IllegalArgumentException("'subject' must be a string or array of strings");
        }
        return subjects;
    }

    private String parseSubjectToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Resource selector cannot be blank");
        }
        int dot = token.indexOf('.');
        if (dot == token.length() - 1) {
            throw new IllegalArgumentException(
                    "Resource selector cannot end with a trailing dot: '" + token + "'");
        }
        return token;
    }

    private List<QueryCondition> parseConditions(JsonNode node) {
        List<QueryCondition> conditions = new ArrayList<>();
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

    private QueryCondition parseCondition(JsonNode node) {
        Map<String, Object> raw = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
        });
        raw = new LinkedHashMap<>(raw);
        raw.remove("type");
        return new QueryCondition(raw);
    }
}
