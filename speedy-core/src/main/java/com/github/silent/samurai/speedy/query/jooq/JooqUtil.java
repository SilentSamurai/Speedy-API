package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JooqUtil {

    static String transformSqlNames = "TO_UPPERCASE";

    public static void transformSqlNames(String setting) {
        transformSqlNames = setting;
    }

    public static DataType<?> getSQLDataType(String fieldReference, ValueType valueType) {
        switch (valueType) {
            case BOOL:
                return SQLDataType.BOOLEAN;
            case TEXT:
                return SQLDataType.NVARCHAR;
            case INT:
                return SQLDataType.INTEGER;
            case FLOAT:
                return SQLDataType.DOUBLE;
            case DATE:
                return SQLDataType.DATE;
            case TIME:
                return SQLDataType.TIME;
            case DATE_TIME:
                return SQLDataType.LOCALDATETIME;
            case ZONED_DATE_TIME:
                return SQLDataType.TIMESTAMPWITHTIMEZONE;
            case OBJECT:
            case COLLECTION:
            case NULL:
            default:
                throw new RuntimeException(String.format("DataType not supported: {} for field {}", valueType, fieldReference));
        }
    }

    public static Table getTable(EntityMetadata entityMetadata) {
        String name = entityMetadata.getDbTableName();

        Objects.requireNonNull(name);

        if (transformSqlNames.equals("TO_UPPERCASE")) {
            name = name.toUpperCase();
        } else if (transformSqlNames.equals("TO_LOWERCASE")) {
            name = name.toLowerCase();
        }

        return DSL.table(DSL.name(name));
    }

    public static <T> Field<T> getColumn(FieldMetadata fieldMetadata) {

        EntityMetadata entityMetadata = fieldMetadata.getEntityMetadata();
        String name = fieldMetadata.getDbColumnName();
        Objects.requireNonNull(name);

        if (transformSqlNames.equals("TO_UPPERCASE")) {
            name = name.toUpperCase();
        } else if (transformSqlNames.equals("TO_LOWERCASE")) {
            name = name.toLowerCase();
        }

        Table<?> table = JooqUtil.getTable(entityMetadata);
        DataType<?> sqlDataType;
        if (fieldMetadata.isAssociation()) {
            ValueType valueType = fieldMetadata.getAssociatedFieldMetadata().getValueType();
            sqlDataType = JooqUtil.getSQLDataType(name, valueType);
        } else {
            sqlDataType = JooqUtil.getSQLDataType(name, fieldMetadata.getValueType());
        }

        Name columnName = DSL.name(
                table.getUnqualifiedName(),
                DSL.name(name)
        );

        if (fieldMetadata instanceof KeyFieldMetadata && ((KeyFieldMetadata) fieldMetadata).shouldGenerateKey()) {
            return (Field<T>) DSL.field(columnName, SQLDataType.VARCHAR(36));
        }
        return (Field<T>) DSL.field(columnName, sqlDataType);
    }

    public static <T> Optional<T> getValueFromRecord(Record record, FieldMetadata fieldMetadata) {
        // should never happen
        if (fieldMetadata.getDbColumnName() == null) {
            return Optional.empty();
        }
        Field<T> column = JooqUtil.getColumn(fieldMetadata);
        try {
            T value = record.get(column);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
