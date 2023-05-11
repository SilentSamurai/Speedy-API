package com.github.silent.samurai.parser;

import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.models.conditions.*;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.models.Filter;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ConditionFactory {

    public static BinarySVCondition createCondition(DbField field, Operator operator, Object instance) throws SpeedyHttpException {
        switch (operator) {
            case EQ:
                return new EqCondition(field, instance);
            case NEQ:
                return new NotEqCondition(field, instance);
            case LT:
                return new LessThanCondition(field, instance);
            case GT:
                return new GreaterThanCondition(field, instance);
            case LTE:
                return new LessThanEqualCondition(field, instance);
            case GTE:
                return new GreaterThanEqualCondition(field, instance);
        }
        throw new BadRequestException("");
    }

    public static BinaryMVCondition createCondition(
            DbField field,
            Operator operator,
            List<Object> instances) throws SpeedyHttpException {
        if (instances.isEmpty()) {
            throw new BadRequestException();
        }
        switch (operator) {
            case IN:
                return new InCondition(field, instances);
            case NOT_IN:
                return new NotInCondition(field, instances);
        }
        throw new BadRequestException("");
    }

    public static BinaryCondition createCondition(Filter filter, EntityMetadata entityMetadata) throws SpeedyHttpException {
        Objects.requireNonNull(filter);
        Operator operator = Operator.fromSymbol(filter.getOperator());
        DbField field = getDbField(filter, entityMetadata);
        List<String> values = filter.getValues();
        if (operator.doesAcceptMultipleValues()) {
            List<Object> instances = new LinkedList<>();
            for (String value : values) {
                Object instance = CommonUtil.quotedStringToPrimitive(value, field.getFieldMetadata().getFieldType());
                instances.add(instance);
            }
            return createCondition(field, operator, instances);
        }
        if (filter.getValues().isEmpty()) {
            throw new BadRequestException();
        }
        Object instance = CommonUtil.quotedStringToPrimitive(values.get(0), field.getFieldMetadata().getFieldType());
        return createCondition(field, operator, instance);
    }

    public static List<Object> getConditionValue(BinaryCondition condition) {
        if (condition instanceof BinarySVCondition) {
            return Lists.newArrayList(((BinarySVCondition) condition).getInstance());
        }
        if (condition instanceof BinaryMVCondition) {
            return ((BinaryMVCondition) condition).getInstances();
        }
        return Collections.emptyList();
    }

    public static DbField getDbField(Filter filter, EntityMetadata entityMetadata) throws SpeedyHttpException {
        if (filter.isAssociationPresent()) {
            FieldMetadata fieldMetadata = entityMetadata.field(filter.getField());
            if (!fieldMetadata.isAssociation()) {
                throw new BadRequestException("");
            }
            EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
            FieldMetadata associatedFieldMetadata = associationMetadata.field(filter.getAssociationId());
            return new AssociatedField(fieldMetadata, associatedFieldMetadata);
        }
        FieldMetadata metadata = entityMetadata.field(filter.getField());
        return new NormalField(metadata);
    }

}
