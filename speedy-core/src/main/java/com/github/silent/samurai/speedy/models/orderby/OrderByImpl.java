package com.github.silent.samurai.speedy.models.orderby;

import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.OrderBy;
import lombok.Getter;

@Getter
public class OrderByImpl implements OrderBy {


    private final FieldMetadata fieldMetadata;
    private final OrderByOperator operator;


    public OrderByImpl(FieldMetadata fieldMetadata, OrderByOperator operator) {
        this.fieldMetadata = fieldMetadata;
        this.operator = operator;
    }

    public static OrderByImpl desc(FieldMetadata fieldMetadata) {
        return new OrderByImpl(fieldMetadata, OrderByOperator.DESC);
    }

    public static OrderByImpl asc(FieldMetadata fieldMetadata) {
        return new OrderByImpl(fieldMetadata, OrderByOperator.ASC);
    }
}
