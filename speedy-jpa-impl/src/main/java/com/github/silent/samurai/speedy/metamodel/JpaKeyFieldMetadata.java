package com.github.silent.samurai.speedy.metamodel;

import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


@Getter
@Setter
public class JpaKeyFieldMetadata extends JpaFieldMetadata implements KeyFieldMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaKeyFieldMetadata.class);

    private Method idClassGetter;
    private Method idClassSetter;
    private Field idClassField;

    private boolean isId;

    @Override
    public boolean isKeyField() {
        return isId;
    }

    @Override
    public boolean setIdFieldWithValue(Object idInstance, Object value) {
        try {
            this.idClassSetter.invoke(idInstance, value);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to set entity field", e);
            return false;
        }
    }

    @Override
    public Object getIdFieldValue(Object idInstance) {
        try {
            return this.idClassGetter.invoke(idInstance);
        } catch (Exception e) {
            LOGGER.error("Unable to get entity field", e);
            return null;
        }
    }

}
