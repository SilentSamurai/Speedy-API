package com.github.silent.samurai.speedy.interfaces.query;


public interface BinaryCondition extends Condition {

    QueryField getField();

    Expression getExpression();

    /// Returns the expression as a {@link Literal}, asserting the invariant that operators which
    /// require a literal operand (IN, NOT_IN, PATTERN_MATCHING, BETWEEN, ISNULL, ISNOTNULL) carry one.
    /// The invariant is enforced at construction (see {@code BinaryConditionImpl}); this accessor lets
    /// backends consume it without a raw cast.
    default Literal getLiteral() {
        if (!(getExpression() instanceof Literal literal)) {
            throw new IllegalStateException(getOperator() + " requires a literal expression");
        }
        return literal;
    }
}
