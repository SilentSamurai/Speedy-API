package com.github.silent.samurai.models.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.utils.CommonUtil;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.LinkedList;
import java.util.List;

@Data
public class InCondition implements BinaryMVCondition {

    private String field;
    private Operator operator = Operator.IN;
    private List<String> values = new LinkedList<>();

    public InCondition(String field, List<String> values) {
        this.field = field;
        this.values.addAll(values);
    }

    @Override
    public void updateFromJson(JsonParser jsonParser) {

    }

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder,
                                  Root<?> tableRoot,
                                  EntityMetadata entityMetadata) throws NotFoundException {
        FieldMetadata fieldMetadata = entityMetadata.field(field);
        String name = fieldMetadata.getClassFieldName();
        List<Object> instances = new LinkedList<>();
        for (String value : values) {
            Object instance = CommonUtil.quotedStringToPrimitive(value, fieldMetadata.getFieldType());
            instances.add(instance);
        }
        return criteriaBuilder.in(tableRoot.get(name)).in(values);
    }

}
