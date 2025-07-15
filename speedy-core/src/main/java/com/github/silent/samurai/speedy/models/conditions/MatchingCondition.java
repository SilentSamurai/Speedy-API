package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingCondition implements BinaryCondition {

    private final QueryField field;
    private final Literal expression;
    private final ConditionOperator operator = ConditionOperator.PATTERN_MATCHING;


    public MatchingCondition(QueryField queryField, Expression expression) throws BadRequestException {
        this.field = queryField;
        if (!(expression instanceof Literal)) {
            throw new BadRequestException("Match only accepts a literal");
        }
        this.expression = (Literal) expression;
    }
}
