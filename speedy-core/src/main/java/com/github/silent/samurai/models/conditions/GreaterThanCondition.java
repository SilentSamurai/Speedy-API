package com.github.silent.samurai.models.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Data
public class GreaterThanCondition implements BinarySVCondition {

    private String field;
    private Operator operator = Operator.GT;
    private String value;

    public GreaterThanCondition(String field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public void updateFromJson(JsonParser jsonParser) {
    }

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder,
                                  Root<?> tableRoot,
                                  EntityMetadata entityMetadata) throws Exception {
        FieldMetadata fieldMetadata = entityMetadata.field(field);
        String name = fieldMetadata.getClassFieldName();
        Object instance = CommonUtil.quotedStringToPrimitive(value, fieldMetadata.getFieldType());
        Path<Comparable> path = tableRoot.get(name);
        return criteriaBuilder.greaterThan(path, (Comparable) instance);
    }
}
