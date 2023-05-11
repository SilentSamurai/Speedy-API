package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.speedy.enums.IgnoreType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.metamodel.Attribute;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


@Getter
@Setter
public class JpaFieldMetadata implements FieldMetadata {

    private String dbColumnName;
    private String outputPropertyName;
    private String classFieldName;
    private Method getter;
    private Method setter;
    private Field field;
    private Attribute<?, ?> jpaAttribute;
    private IgnoreType ignoreType = null;
    private Class<?> fieldType;
    private boolean isInsertable;
    private boolean isUpdatable;
    private boolean isUnique;
    private boolean isNullable;
    private boolean isRequired;
    private boolean isSerializable;
    private boolean isDeserializable;
    private EntityMetadata associationMetadata;

    public boolean isAssociation() {
        return jpaAttribute.isAssociation();
    }

    public boolean isCollection() {
        return jpaAttribute.isCollection();
    }

    @Override
    public boolean setEntityFieldWithValue(Object entity, Object value) {
        try {
            this.setter.invoke(entity, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object getEntityFieldValue(Object entityObject) {
        try {
            return this.getter.invoke(entityObject);
        } catch (Exception e) {
            return null;
        }
    }


}
