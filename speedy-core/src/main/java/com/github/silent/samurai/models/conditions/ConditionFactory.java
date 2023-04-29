package com.github.silent.samurai.models.conditions;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.speedy.models.Filter;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ConditionFactory {

    public static BinaryCondition createCondition(String identifier, Operator operator, String value) throws BadRequestException {
        switch (operator) {
            case EQ:
                return new EqCondition(identifier, value);
            case NEQ:
                return new NotEqCondition(identifier, value);
            case LT:
                return new LessThanCondition(identifier, value);
            case GT:
                return new GreaterThanCondition(identifier, value);
            case LTE:
                return new LessThanEqualCondition(identifier, value);
            case GTE:
                return new GreaterThanEqualCondition(identifier, value);
        }
        throw new BadRequestException("");
    }

    public static BinaryCondition createCondition(String identifier, Operator operator, List<String> values) throws BadRequestException {
        if (values.isEmpty()) {
            throw new BadRequestException();
        }
        switch (operator) {
            case IN:
                return new InCondition(identifier, values);
            case NOT_IN:
                return new NotInCondition(identifier, values);
        }
        throw new BadRequestException("");
    }

    public static BinaryCondition createCondition(Filter filter) throws BadRequestException {
        Objects.requireNonNull(filter);
        Operator operator = Operator.fromSymbol(filter.getOperator());
        if (filter.isMultiple()) {
            return createCondition(filter.getField(), operator, filter.getValues());
        }
        if (filter.getValues().isEmpty()) throw new BadRequestException();
        return createCondition(filter.getField(), operator, filter.getValues().get(0));
    }

    public static List<String> getConditionValue(BinaryCondition condition) {
        if (condition instanceof BinarySVCondition) {
            return Lists.newArrayList(((BinarySVCondition) condition).getValue());
        }
        if (condition instanceof BinaryMVCondition) {
            return ((BinaryMVCondition) condition).getValues();
        }
        return Collections.emptyList();
    }

}
