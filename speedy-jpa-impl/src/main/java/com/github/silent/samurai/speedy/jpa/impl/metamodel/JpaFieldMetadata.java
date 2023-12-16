package com.github.silent.samurai.speedy.jpa.impl.metamodel;

import com.github.silent.samurai.speedy.enums.IgnoreType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaEntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaFieldMetadata;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.metamodel.Attribute;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


@Getter
@Setter
public class JpaFieldMetadata implements IJpaFieldMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaFieldMetadata.class);

    private ValueType valueType;
    private String dbColumnName;
    private String outputPropertyName;
    private String classFieldName;
    private Method getter;
    private Method setter;
    private Field field;
    private Attribute<?, ?> jpaAttribute;
    private IgnoreType ignoreProperty = null;
    private Class<?> fieldType;
    private boolean isInsertable;
    private boolean isUpdatable;
    private boolean isUnique;
    private boolean isNullable;
    private boolean isRequired;
    private boolean isSerializable;
    private boolean isDeserializable;
    private IJpaEntityMetadata entityMetadata;
    private IJpaEntityMetadata associationMetadata;

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
            LOGGER.error("Unable to set entity field", e);
            return false;
        }
    }

    @Override
    public Object getEntityFieldValue(Object entityObject) {
        try {
            return this.getter.invoke(entityObject);
        } catch (Exception e) {
            LOGGER.error("Unable to get entity field", e);
            return null;
        }
    }


}
