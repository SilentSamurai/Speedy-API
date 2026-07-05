package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.validation.rules.OrderingOperatorTypeRule;
import com.github.silent.samurai.speedy.validation.rules.PatternMatchingTypeRule;
import com.github.silent.samurai.speedy.validation.rules.QueryRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the **default** read-request validation logic applied to a
 * {@link SpeedyQuery} (both URI {@code GET} and {@code POST /$query}).
 * <p>
 * This is the read-side counterpart to {@link DefaultFieldValidator}. It does two
 * things:
 * <ul>
 *     <li>applies composable, backend-agnostic {@link QueryRule}s to each binary
 *     condition (operator/type constraints), so the same constraints hold no matter
 *     which {@code QueryProcessor} executes the query; and</li>
 *     <li>enforces query-level complexity limits — condition nesting depth, total
 *     condition count, and number of expands — to guard against pathological queries.</li>
 * </ul>
 * Note: {@code @SpeedySensitive} field references are handled separately by
 * {@code ConditionFactory} (only a sensitive field on the right-hand side of a
 * {@code $field} reference is blocked); filtering a sensitive field against a
 * literal is intentionally allowed and is not re-checked here.
 * <p>
 * The class is stateless and therefore thread-safe; rule instances are kept in an
 * immutable list and all per-request state is passed through the recursion.
 */
public class DefaultQueryValidator {

    /**
     * Built-in rule set, applied to every binary condition in the query.
     */
    private static final List<QueryRule> DEFAULT_RULES = List.of(
            new PatternMatchingTypeRule(),
            new OrderingOperatorTypeRule()
    );

    private final List<QueryRule> queryRules;
    private final int maxConditionDepth;
    private final int maxConditionCount;
    private final int maxExpandCount;

    /**
     * @param maxConditionDepth max nesting depth of boolean condition groups
     * @param maxConditionCount max number of binary (leaf) conditions
     * @param maxExpandCount    max number of {@code $expand} entries
     */
    public DefaultQueryValidator(int maxConditionDepth, int maxConditionCount, int maxExpandCount) {
        this.queryRules = DEFAULT_RULES;
        this.maxConditionDepth = maxConditionDepth;
        this.maxConditionCount = maxConditionCount;
        this.maxExpandCount = maxExpandCount;
    }

    public void validateQuery(SpeedyQuery query) throws BadRequestException {
        List<String> errors = new ArrayList<>();

        BooleanCondition where = query.getWhere();
        if (where != null) {
            Metrics metrics = new Metrics();
            walk(where, 1, metrics, errors);

            if (metrics.maxDepth > maxConditionDepth) {
                errors.add("query condition nesting is too deep (max " + maxConditionDepth + ")");
            }
            if (metrics.conditionCount > maxConditionCount) {
                errors.add("too many query conditions: " + metrics.conditionCount + " (max " + maxConditionCount + ")");
            }
        }

        Set<String> expand = query.getExpand();
        if (expand != null && expand.size() > maxExpandCount) {
            errors.add("too many $expand entries: " + expand.size() + " (max " + maxExpandCount + ")");
        }

        throwIfErrors(errors);
    }

    /**
     * Single traversal of the condition tree that both gathers complexity metrics
     * (depth, count) and applies the per-condition {@link QueryRule}s.
     */
    private void walk(Condition condition, int depth, Metrics metrics, List<String> errors) {
        if (condition instanceof BooleanCondition booleanCondition) {
            metrics.maxDepth = Math.max(metrics.maxDepth, depth);
            for (Condition sub : booleanCondition.getConditions()) {
                walk(sub, depth + 1, metrics, errors);
            }
        } else if (condition instanceof BinaryCondition binaryCondition) {
            metrics.conditionCount++;
            validateBinary(binaryCondition, errors);
        }
    }

    private void validateBinary(BinaryCondition condition, List<String> errors) {
        FieldMetadata field = condition.getField().getMetadataForParsing();
        ConditionOperator operator = condition.getOperator();
        SpeedyValue literal = (condition.getExpression() instanceof Literal lit) ? lit.value() : null;

        for (QueryRule rule : queryRules) {
            rule.validate(field, operator, literal, errors);
        }
    }

    private static void throwIfErrors(List<String> errors) throws BadRequestException {
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join(" | ", errors));
        }
    }

    /** Mutable per-request accumulator for query-complexity metrics. */
    private static final class Metrics {
        private int maxDepth = 0;
        private int conditionCount = 0;
    }
}
