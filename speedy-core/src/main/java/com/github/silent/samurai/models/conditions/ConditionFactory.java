package com.github.silent.samurai.models.conditions;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.speedy.model.Filter;

import java.util.List;
import java.util.Objects;

public class ConditionFactory {

    public static Condition createCondition(String identifier, Operator operator, String value) throws BadRequestException {
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

    public static Condition createCondition(String identifier, Operator operator, List<String> values) throws BadRequestException {
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

    public static Condition createCondition(Filter filter) throws BadRequestException {
        Operator operator = Operator.fromSymbol(filter.getOperator());
        Objects.requireNonNull(filter.getValue());
        if (filter.getValue().isMultiple()) {
            return createCondition(filter.getIdentifier(), operator, filter.getValue().getValues());
        }
        return createCondition(filter.getIdentifier(), operator, filter.getValue().getValues().get(0));
    }

}
