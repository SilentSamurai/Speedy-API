package com.github.silent.samurai.speedy.models.orderby;

import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.interfaces.query.OrderBy;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import lombok.Getter;

@Getter
public class OrderByImpl implements OrderBy {


    private final QueryField field;
    private final OrderByOperator operator;

    public OrderByImpl(QueryField field, OrderByOperator operator) {
        this.field = field;
        this.operator = operator;
    }

    public static OrderByImpl desc(QueryField field) {
        return new OrderByImpl(field, OrderByOperator.DESC);
    }

    public static OrderByImpl asc(QueryField field) {
        return new OrderByImpl(field, OrderByOperator.ASC);
    }
}
