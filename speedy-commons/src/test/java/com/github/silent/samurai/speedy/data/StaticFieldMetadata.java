package com.github.silent.samurai.speedy.data;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.models.DynamicEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Data
public class StaticFieldMetadata implements KeyFieldMetadata {

    private Field field;

    public static KeyFieldMetadata createFieldMetadata(Field field) {
        StaticFieldMetadata fieldMetadata = new StaticFieldMetadata();
        field.setAccessible(true);
        fieldMetadata.setField(field);
        return fieldMetadata;
    }

    public static ColumnType fromJavaType(Class<?> type) {
        return switch (type.getSimpleName()) {
            case "Boolean", "boolean" -> ColumnType.BOOLEAN;
            case "UUID" -> ColumnType.UUID;
            case "String" -> ColumnType.TEXT; // could also be ENUM, but TEXT is safer default
            case "Long", "Integer", "Short", "Byte", "BigInteger" -> ColumnType.INTEGER;
            case "Double", "Float", "BigDecimal" -> ColumnType.FLOAT;
            case "LocalDate", "Date" -> ColumnType.DATE;
            case "LocalTime", "Time" -> ColumnType.TIME;
            case "LocalDateTime", "Timestamp", "Instant" -> ColumnType.TIMESTAMP;
            case "ZonedDateTime" -> ColumnType.TIMESTAMP_WITH_ZONE;

            default -> throw new IllegalArgumentException("Unsupported Java type: " + type);
        };
    }

    @SneakyThrows
    @Override
    public ColumnType getColumnType() {
        if (isAssociation()) {
            return getAssociatedFieldMetadata().getColumnType();
        }
        return fromJavaType(field.getType());
    }

    @Override
    public ValueType getValueType() {
        return getColumnType().getValueType();
    }

    @Override
    public boolean isAssociation() {
        Object[] annotations = {
                field.getAnnotation(OneToMany.class),
                field.getAnnotation(ManyToOne.class),
                field.getAnnotation(ManyToMany.class),
                field.getAnnotation(OneToOne.class)
        };
        Optional<Object> isAnnotationPresent = Arrays.stream(annotations).filter(Objects::nonNull).findAny();
        if (isAnnotationPresent.isPresent()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public boolean isInsertable() {
        return true;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean isSerializable() {
        return true;
    }

//    @Override
//    public String getClassFieldName() {
//        return field.getName();
//    }

    @Override
    public boolean isDeserializable() {
        return true;
    }

    @Override
    public String getDbColumnName() {
        return field.getName();
    }

    @Override
    public String getOutputPropertyName() {
        return field.getName();
    }

    @Override
    public boolean isKeyField() {
        return field.getAnnotation(Id.class) != null;
    }

    @Override
    public boolean shouldGenerateKey() {
        return true;
    }

    @Override
    public EntityMetadata getEntityMetadata() {
        return StaticEntityMetadata.createEntityMetadata(field.getDeclaringClass());
    }

    @Override
    public EntityMetadata getAssociationMetadata() {
        if (field.getAnnotation(OneToMany.class) != null) {
            return StaticEntityMetadata.createEntityMetadata(field.getType());
        }
        if (field.getAnnotation(OneToOne.class) != null) {
            return StaticEntityMetadata.createEntityMetadata(field.getType());
        }
        return null;
    }

    @Override
    public FieldMetadata getAssociatedFieldMetadata() {
        EntityMetadata associationMetadata = getAssociationMetadata();
        return associationMetadata.getAllFields().stream().filter(fm -> fm.getOutputPropertyName().equals("id")).findAny().orElse(null);
    }

    @Override
    public boolean isEnum() {
        return field.getAnnotation(Enumerated.class) != null;
    }

    @Override
    public EnumMode getOperationalEnumMode() {
        return this.getStoredEnumMode();
    }

    @Override
    public DynamicEnum getDynamicEnum() {
        if (!isEnum()) {
            throw new IllegalArgumentException("Field is not an enum");
        }
        return DynamicEnum.of((Class<? extends Enum<?>>) field.getType());
    }

    @Override
    public EnumMode getStoredEnumMode() {
        return field.getAnnotation(Enumerated.class).value() == EnumType.STRING ? EnumMode.STRING : EnumMode.ORDINAL;
    }


}
