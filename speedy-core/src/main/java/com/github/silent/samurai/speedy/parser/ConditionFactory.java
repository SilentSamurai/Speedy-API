package com.github.silent.samurai.speedy.parser;

import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.models.conditions.*;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;

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

    public QueryField createQueryField(String fieldName) throws SpeedyHttpException {
        String associatedField = null;
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            if (parts.length == 2) {
                fieldName = parts[0];
                associatedField = parts[1];
            }
        }
        if (associatedField != null) {
            return createAssociatedField(fieldName, associatedField);
        }
        return createNormalField(fieldName);
    }

    public QueryField createNormalField(String field) throws SpeedyHttpException {
        FieldMetadata fieldMetadata = this.entityMetadata.field(field);
        return new NormalField(fieldMetadata);
    }

    public QueryField createAssociatedField(String field, String associatedField) throws SpeedyHttpException {
        FieldMetadata fieldMetadata = this.entityMetadata.field(field);
        if (!fieldMetadata.isAssociation()) {
            throw new BadRequestException("field is not an association: " + fieldMetadata.getOutputPropertyName());
        }
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        FieldMetadata associatedFieldMetadata = associationMetadata.field(associatedField);
        return new AssociatedField(fieldMetadata, associatedFieldMetadata);
    }

    public BinaryCondition createBiCondition(QueryField normalField, ConditionOperator operator, SpeedyValue speedyValue) throws SpeedyHttpException {
        return createCondition(normalField, operator, speedyValue);
    }

    public BinaryCondition createBinaryCondition(String field, String operatorSymbol, ValueNode value) throws SpeedyHttpException {
        ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);
        FieldMetadata fieldMetadata = this.entityMetadata.field(field);
        SpeedyValue speedyValue = SpeedyValueFactory.fromJsonValue(fieldMetadata, value);
        QueryField normalField = new NormalField(fieldMetadata);
        return createCondition(normalField, operator, speedyValue);
    }

    public BinaryCondition createAssociatedCondition(String field, String associatedField, String operatorSymbol, ValueNode value) throws SpeedyHttpException {
        FieldMetadata fieldMetadata = this.entityMetadata.field(field);
        ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);
        if (!fieldMetadata.isAssociation()) {
            throw new BadRequestException("field is not an association: " + fieldMetadata.getOutputPropertyName());
        }
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        FieldMetadata associatedFieldMetadata = associationMetadata.field(associatedField);
        QueryField queryField = new AssociatedField(fieldMetadata, associatedFieldMetadata);
        SpeedyValue speedyValue = SpeedyValueFactory.fromJsonValue(associatedFieldMetadata, value);
        return createCondition(queryField, operator, speedyValue);
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
