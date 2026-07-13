package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.serialization.MapStructureReader;
import com.github.silent.samurai.speedy.serialization.StructureToQuery;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/// A row-level ABAC condition: a raw {@code Map} spec parsed lazily, on first use against the
/// entity it's evaluated for, by {@link StructureToQuery} — the same {@code $or}/{@code $and}/
/// operator/{@code ${variable}} grammar real request filters use, driven over the spec via
/// {@link MapStructureReader} instead of a bespoke second parser. Checked per-row for
/// isAuthorized decisions, and translated to a query {@link Condition} for WHERE-injection so
/// unreadable rows never come back from the database ({@link #toQueryCondition}). This is the
/// only kind of policy condition Speedy supports — there is no discriminator or plugin point.
public class QueryCondition {

    private final Map<String, Object> rawSpec;
    private Condition parsedCondition;
    private EntityMetadata parsedFor;

    public QueryCondition(Map<String, Object> rawSpec) {
        this.rawSpec = Map.copyOf(rawSpec);
    }

    public boolean isSatisfied(ConditionContext ctx) {
        try {
            Condition condition = ensureParsed(ctx.entityMetadata());
            return ConditionEvaluator.evaluate(condition, ctx);
        } catch (SpeedyHttpException e) {
            return false;
        }
    }

    public Optional<Condition> toQueryCondition(ConditionContext ctx) {
        try {
            Condition condition = ensureParsed(ctx.entityMetadata());
            Map<String, SpeedyValue> variables = ctx.variables();
            if (variables != null && !variables.isEmpty()) {
                condition = ConditionResolver.resolve(condition, variables);
            }
            if (condition instanceof BooleanCondition bc
                    && bc.getOperator() == ConditionOperator.AND
                    && bc.getConditions().size() == 1) {
                condition = bc.getConditions().get(0);
            }
            return Optional.of(condition);
        } catch (SpeedyHttpException e) {
            return Optional.empty();
        }
    }

    /// The entity fields this condition compares against — the left-hand column of every leaf
    /// predicate, ignoring the value operands (literals or {@code ${variable}} references). Used to
    /// enforce that a field cannot silently gate a write the caller can't otherwise read (see
    /// {@code PolicyEngine#writeConditionFields}).
    public Set<FieldMetadata> referencedFields(EntityMetadata entityMetadata) throws SpeedyHttpException {
        Set<FieldMetadata> fields = new LinkedHashSet<>();
        collectFields(ensureParsed(entityMetadata), fields);
        return fields;
    }

    private static void collectFields(Condition condition, Set<FieldMetadata> out) {
        if (condition instanceof BooleanCondition bc) {
            for (Condition sub : bc.getConditions()) {
                collectFields(sub, out);
            }
        } else if (condition instanceof BinaryCondition bc) {
            out.add(bc.getField().getMetadataForParsing());
        }
    }

    private Condition ensureParsed(EntityMetadata entityMetadata) throws SpeedyHttpException {
        if (parsedCondition == null || parsedFor != entityMetadata) {
            parsedCondition = new StructureToQuery().parseCondition(entityMetadata, new MapStructureReader(rawSpec));
            parsedFor = entityMetadata;
        }
        return parsedCondition;
    }
}
