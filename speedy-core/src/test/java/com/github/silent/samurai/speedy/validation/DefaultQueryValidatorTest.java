package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.conditions.BinaryConditionImpl;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Unit tests for {@link DefaultQueryValidator}: walks a query's WHERE tree applying the
/// default {@code QueryRule} set (pattern-match type, ordering type) plus the query-level
/// complexity limits (condition count, nesting depth, expand count).
class DefaultQueryValidatorTest {

    private static final int MAX_DEPTH = 3;
    private static final int MAX_COUNT = 4;
    private static final int MAX_EXPANDS = 2;

    private final DefaultQueryValidator validator = new DefaultQueryValidator(MAX_DEPTH, MAX_COUNT, MAX_EXPANDS);

    @Test
    void patternMatchingOnNonTextFieldIsRejected() {
        SpeedyQuery query = queryWith(condition(field("cost", ValueType.INT, false), ConditionOperator.PATTERN_MATCHING));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateQuery(query));
        assertTrue(ex.getMessage().contains("$matches"));
    }

    @Test
    void patternMatchingOnTextFieldIsAllowed() {
        SpeedyQuery query = queryWith(condition(field("name", ValueType.TEXT, false), ConditionOperator.PATTERN_MATCHING));

        assertDoesNotThrow(() -> validator.validateQuery(query));
    }

    @Test
    void relationalOperatorOnBooleanFieldIsRejected() {
        SpeedyQuery query = queryWith(condition(field("active", ValueType.BOOL, false), ConditionOperator.GT));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateQuery(query));
        assertTrue(ex.getMessage().contains("GT"));
    }

    @Test
    void relationalOperatorOnNumericFieldIsAllowed() {
        SpeedyQuery query = queryWith(condition(field("cost", ValueType.INT, false), ConditionOperator.GT));

        assertDoesNotThrow(() -> validator.validateQuery(query));
    }

    @Test
    void plainEqualityOnTextFieldIsAllowed() {
        SpeedyQuery query = queryWith(condition(field("name", ValueType.TEXT, false), ConditionOperator.EQ));

        assertDoesNotThrow(() -> validator.validateQuery(query));
    }

    @Test
    void nullWhereClauseIsAllowed() {
        SpeedyQuery query = mock(SpeedyQuery.class);
        when(query.getWhere()).thenReturn(null);

        assertDoesNotThrow(() -> validator.validateQuery(query));
    }

    @Test
    void tooManyConditionsIsRejected() {
        BinaryCondition[] conditions = new BinaryCondition[MAX_COUNT + 1];
        for (int i = 0; i < conditions.length; i++) {
            conditions[i] = condition(field("name", ValueType.TEXT, false), ConditionOperator.EQ);
        }
        SpeedyQuery query = queryWith(conditions);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateQuery(query));
        assertTrue(ex.getMessage().contains("too many query conditions"));
    }

    @Test
    void conditionCountAtLimitIsAllowed() {
        BinaryCondition[] conditions = new BinaryCondition[MAX_COUNT];
        for (int i = 0; i < conditions.length; i++) {
            conditions[i] = condition(field("name", ValueType.TEXT, false), ConditionOperator.EQ);
        }
        SpeedyQuery query = queryWith(conditions);

        assertDoesNotThrow(() -> validator.validateQuery(query));
    }

    @Test
    void deeplyNestedConditionsAreRejected() {
        BooleanConditionImpl where = nest(MAX_DEPTH + 1,
                condition(field("name", ValueType.TEXT, false), ConditionOperator.EQ));
        SpeedyQuery query = mock(SpeedyQuery.class);
        when(query.getWhere()).thenReturn(where);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateQuery(query));
        assertTrue(ex.getMessage().contains("too deep"));
    }

    @Test
    void nestingDepthAtLimitIsAllowed() {
        BooleanConditionImpl where = nest(MAX_DEPTH,
                condition(field("name", ValueType.TEXT, false), ConditionOperator.EQ));
        SpeedyQuery query = mock(SpeedyQuery.class);
        when(query.getWhere()).thenReturn(where);

        assertDoesNotThrow(() -> validator.validateQuery(query));
    }

    @Test
    void tooManyExpandsIsRejected() {
        Set<String> expands = new HashSet<>();
        for (int i = 0; i <= MAX_EXPANDS; i++) {
            expands.add("rel" + i);
        }
        SpeedyQuery query = mock(SpeedyQuery.class);
        when(query.getWhere()).thenReturn(null);
        when(query.getExpand()).thenReturn(expands);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateQuery(query));
        assertTrue(ex.getMessage().contains("$expand"));
    }

    /* ---------------------------------------------------------------------- */

    /** Builds {@code levels} nested boolean groups with {@code leaf} at the bottom. */
    private BooleanConditionImpl nest(int levels, BinaryCondition leaf) {
        BooleanConditionImpl current = new BooleanConditionImpl(ConditionOperator.AND);
        current.addSubCondition(leaf);
        for (int i = 1; i < levels; i++) {
            BooleanConditionImpl parent = new BooleanConditionImpl(ConditionOperator.AND);
            parent.addSubCondition(current);
            current = parent;
        }
        return current;
    }

    private FieldMetadata field(String name, ValueType type, boolean sensitive) {
        FieldMetadata fm = mock(FieldMetadata.class);
        lenient().when(fm.getOutputPropertyName()).thenReturn(name);
        lenient().when(fm.getValueType()).thenReturn(type);
        lenient().when(fm.isSensitive()).thenReturn(sensitive);
        return fm;
    }

    private BinaryCondition condition(FieldMetadata fm, ConditionOperator operator) {
        QueryField queryField = mock(QueryField.class);
        when(queryField.getMetadataForParsing()).thenReturn(fm);
        SpeedyValue literal = mock(SpeedyValue.class);
        try {
            return new BinaryConditionImpl(queryField, operator, new Literal(literal));
        } catch (SpeedyHttpException e) {
            throw new RuntimeException(e);
        }
    }

    private SpeedyQuery queryWith(BinaryCondition... conditions) {
        BooleanConditionImpl where = new BooleanConditionImpl(ConditionOperator.AND);
        for (BinaryCondition condition : conditions) {
            where.addSubCondition(condition);
        }
        SpeedyQuery query = mock(SpeedyQuery.class);
        when(query.getWhere()).thenReturn(where);
        return query;
    }
}
