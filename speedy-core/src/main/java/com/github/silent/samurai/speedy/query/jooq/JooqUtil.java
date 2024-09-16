package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.enums.ValueType;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;

public class JooqUtil {

    public static DataType<?> getSQLDataType(ValueType valueType) {
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
                throw new RuntimeException("DataType not supported: " + valueType);
        }
    }
}
