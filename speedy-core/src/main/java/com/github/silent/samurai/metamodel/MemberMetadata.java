package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.enums.IgnoreType;
import com.github.silent.samurai.interfaces.ISpeedyCustomValidation;

import javax.persistence.metamodel.Attribute;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MemberMetadata {

    private Attribute<?, ?> jpaAttribute;
    private String name;
    private Method getter;
    private Method setter;
    private Field field;
    private boolean isId;
    private Class<ISpeedyCustomValidation> customValidation;
    private IgnoreType ignoreType = null;

    public boolean isCustomValidationRequired() {
        return customValidation != null;
    }

    public Object getFieldValue(Object entityObject) throws IllegalAccessException, InvocationTargetException {
        return this.getter.invoke(entityObject);
    }


    public Attribute<?, ?> getJpaAttribute() {
        return jpaAttribute;
    }

    public void setJpaAttribute(Attribute<?, ?> jpaAttribute) {
        this.jpaAttribute = jpaAttribute;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Method getGetter() {
        return getter;
    }

    public void setGetter(Method getter) {
        this.getter = getter;
    }

    public Method getSetter() {
        return setter;
    }

    public void setSetter(Method setter) {
        this.setter = setter;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public boolean isId() {
        return isId;
    }

    public void setId(boolean id) {
        isId = id;
    }

    public Class<ISpeedyCustomValidation> getCustomValidation() {
        return customValidation;
    }

    public void setCustomValidation(Class<ISpeedyCustomValidation> customValidation) {
        this.customValidation = customValidation;
    }

    public IgnoreType getIgnoreType() {
        return ignoreType;
    }

    public void setIgnoreType(IgnoreType ignoreType) {
        this.ignoreType = ignoreType;
    }
}
