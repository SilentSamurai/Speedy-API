package com.github.silent.samurai.speedy.parser;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import com.github.silent.samurai.speedy.models.conditions.*;

import java.util.LinkedList;
import java.util.List;

public class ConditionFactory {

    private final EntityMetadata entityMetadata;

    public ConditionFactory(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    private BinaryCondition createCondition(QueryField field, ConditionOperator operator, SpeedyValue instance) throws SpeedyHttpException {
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
            case IN:
                return new InCondition(field, instance);
            case NOT_IN:
                return new NotInCondition(field, instance);
        }
        throw new BadRequestException("");
    }

    public BinaryCondition createBinaryConditionQuotedString(String field, String operatorSymbol, String value) throws SpeedyHttpException {
        ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);
        FieldMetadata fieldMetadata = this.entityMetadata.field(field);
        SpeedyValue speedyValue = SpeedyValueFactory.fromQuotedString(fieldMetadata, value);
        QueryField normalField = new NormalField(fieldMetadata);
        return createCondition(normalField, operator, speedyValue);
    }

    public BinaryCondition createBinaryConditionQuotedString(String field, String operatorSymbol, List<String> values) throws SpeedyHttpException {
        ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);
        FieldMetadata fieldMetadata = this.entityMetadata.field(field);
        List<SpeedyValue> instances = new LinkedList<>();
        for (String value : values) {
            SpeedyValue speedyValue = SpeedyValueFactory.fromQuotedString(fieldMetadata, value);
            instances.add(speedyValue);
        }
        SpeedyValue speedyValue = SpeedyValueFactory.fromCollection(instances);
        QueryField normalField = new NormalField(fieldMetadata);
        return createCondition(normalField, operator, speedyValue);
    }

    public BinaryCondition createAssociatedConditionQuotedString(String field, String associatedField, String operatorSymbol, String value) throws SpeedyHttpException {
        FieldMetadata fieldMetadata = this.entityMetadata.field(field);
        ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);
        if (!fieldMetadata.isAssociation()) {
            throw new BadRequestException("");
        }
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        FieldMetadata associatedFieldMetadata = associationMetadata.field(associatedField);
        QueryField queryField = new AssociatedField(fieldMetadata, associatedFieldMetadata);
        SpeedyValue speedyValue = SpeedyValueFactory.fromQuotedString(associatedFieldMetadata, value);
        return createCondition(queryField, operator, speedyValue);
    }

}
