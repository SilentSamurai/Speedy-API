package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.Identifier;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.interfaces.query.VariableRef;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ConditionEvaluator {

    private ConditionEvaluator() {
    }

    public static boolean evaluate(Condition condition, ConditionContext ctx) {
        if (condition instanceof BinaryCondition c) {
            return evaluateBinary(c, ctx);
        }
        if (condition instanceof BooleanCondition c) {
            return evaluateBoolean(c, ctx);
        }
        return false;
    }

    private static boolean evaluateBinary(BinaryCondition c, ConditionContext ctx) {
        if (!ctx.hasRow()) {
            return false;
        }
        QueryField field = c.getField();
        FieldMetadata fieldMetadata = field.getMetadataForParsing();
        SpeedyEntity row = ctx.row();
        if (!row.has(fieldMetadata)) {
            return false;
        }
        SpeedyValue fieldValue = row.get(fieldMetadata);
        ConditionOperator op = c.getOperator();
        Expression expr = c.getExpression();
        if (op == ConditionOperator.ISNULL) {
            return fieldValue == null || fieldValue.isNull();
        }
        if (op == ConditionOperator.ISNOTNULL) {
            return fieldValue != null && !fieldValue.isNull();
        }
        SpeedyValue rhs = resolveExpression(expr, ctx);
        return switch (op) {
            case EQ -> PolicyValueEquals.equals(fieldValue, rhs);
            case NEQ -> !PolicyValueEquals.equals(fieldValue, rhs);
            case GT -> compare(fieldValue, rhs) > 0;
            case LT -> compare(fieldValue, rhs) < 0;
            case GTE -> compare(fieldValue, rhs) >= 0;
            case LTE -> compare(fieldValue, rhs) <= 0;
            case IN -> inCollection(fieldValue, rhs);
            case NOT_IN -> !inCollection(fieldValue, rhs);
            case BETWEEN -> between(fieldValue, rhs);
            case PATTERN_MATCHING -> patternMatch(fieldValue, rhs);
            default -> false;
        };
    }

    private static boolean evaluateBoolean(BooleanCondition c, ConditionContext ctx) {
        List<Condition> conditions = c.getConditions();
        if (conditions.isEmpty()) {
            return true;
        }
        return switch (c.getOperator()) {
            case AND -> conditions.stream().allMatch(sub -> evaluate(sub, ctx));
            case OR -> conditions.stream().anyMatch(sub -> evaluate(sub, ctx));
            default -> false;
        };
    }

    private static SpeedyValue resolveExpression(Expression expr, ConditionContext ctx) {
        if (expr instanceof Literal l) {
            return l.value();
        }
        if (expr instanceof Identifier id) {
            return resolveIdentifier(id, ctx);
        }
        if (expr instanceof VariableRef vr) {
            return resolveVariable(vr, ctx);
        }
        return SpeedyNull.SPEEDY_NULL;
    }

    private static SpeedyValue resolveIdentifier(Identifier id, ConditionContext ctx) {
        if (!ctx.hasRow()) {
            return SpeedyNull.SPEEDY_NULL;
        }
        FieldMetadata fm = id.field().getMetadataForParsing();
        SpeedyEntity row = ctx.row();
        if (row.has(fm)) {
            return row.get(fm);
        }
        return SpeedyNull.SPEEDY_NULL;
    }

    private static SpeedyValue resolveVariable(VariableRef vr, ConditionContext ctx) {
        Map<String, SpeedyValue> vars = ctx.variables();
        if (vars != null && vars.containsKey(vr.name())) {
            return vars.get(vr.name());
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(SpeedyValue a, SpeedyValue b) {
        if (a == null || b == null || a.isNull() || b.isNull()) {
            return 0;
        }
        Comparable left = toComparable(a);
        Comparable right = toComparable(b);
        if (left == null || right == null) {
            return textOf(a).compareTo(textOf(b));
        }
        try {
            return left.compareTo(right);
        } catch (ClassCastException e) {
            return textOf(a).compareTo(textOf(b));
        }
    }

    private static Comparable<?> toComparable(SpeedyValue v) {
        return switch (v.getValueType()) {
            case INT, ENUM_ORD -> v.asInt();
            case FLOAT -> v.asDouble();
            case TEXT, ENUM -> v.asText();
            case DATE -> v.asDate();
            case DATE_TIME -> v.asDateTime();
            case TIME -> v.asTime();
            case ZONED_DATE_TIME -> v.asZonedDateTime();
            case BOOL -> v.asBoolean();
            default -> null;
        };
    }

    private static boolean inCollection(SpeedyValue value, SpeedyValue rhs) {
        if (rhs == null || !rhs.isCollection()) {
            return false;
        }
        Collection<SpeedyValue> items = rhs.asCollection();
        for (SpeedyValue item : items) {
            if (PolicyValueEquals.equals(value, item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean between(SpeedyValue value, SpeedyValue rhs) {
        if (rhs == null || !rhs.isCollection()) {
            return false;
        }
        List<SpeedyValue> items = List.copyOf(rhs.asCollection());
        if (items.size() != 2) {
            return false;
        }
        return compare(value, items.get(0)) >= 0 && compare(value, items.get(1)) <= 0;
    }

    private static boolean patternMatch(SpeedyValue value, SpeedyValue rhs) {
        if (value == null || rhs == null) {
            return false;
        }
        String text = textOf(value);
        String pattern = textOf(rhs);
        String regex = globToRegex(pattern);
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).matches();
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                case '.', '(', ')', '[', ']', '{', '}', '\\', '^', '$', '|', '+' ->
                        sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        sb.append('$');
        return sb.toString();
    }

    private static String textOf(SpeedyValue v) {
        if (v == null || v.isNull()) {
            return "";
        }
        return switch (v.getValueType()) {
            case TEXT, ENUM -> v.asText();
            case INT, ENUM_ORD -> String.valueOf(v.asInt());
            case FLOAT -> String.valueOf(v.asDouble());
            case BOOL -> String.valueOf(v.asBoolean());
            case DATE -> String.valueOf(v.asDate());
            case DATE_TIME -> String.valueOf(v.asDateTime());
            case TIME -> String.valueOf(v.asTime());
            case ZONED_DATE_TIME -> String.valueOf(v.asZonedDateTime());
            case NULL -> "";
            case OBJECT, COLLECTION -> String.valueOf(v);
        };
    }
}
