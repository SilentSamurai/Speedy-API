package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.enums.IgnoreType;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.ISpeedyCustomValidation;
import lombok.Data;

import javax.persistence.metamodel.Attribute;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


@Data
public class JpaFieldMetadata implements FieldMetadata {

    private String fieldName;
    private Method getter;
    private Method setter;
    private Field field;
    private Attribute<?, ?> jpaAttribute;
    private boolean isId;
    private Class<ISpeedyCustomValidation> customValidation;
    private IgnoreType ignoreType = null;
    private Class<?> fieldClass;

    public boolean isAssociation() {
        return jpaAttribute.isAssociation();
    }

    public boolean isCollection() {
        return jpaAttribute.isCollection();
    }

    @Override
    public boolean isKeyField() {
        return isId;
    }

    @Override
    public Object extractFieldValue(Object entityObject) throws IllegalAccessException, InvocationTargetException {
        return this.getter.invoke(entityObject);
    }


}
