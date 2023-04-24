package com.github.silent.samurai.docs;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import io.swagger.v3.oas.models.media.Schema;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class OASGenerator {

    private static final Map<Class<?>, Schema<?>> PRIMITIVE_TYPE_TO_SCHEMA_MAP = new HashMap<>();

    static {
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(boolean.class, new Schema<>().type("boolean"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(byte.class, new Schema<>().type("integer").format("int8"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(short.class, new Schema<>().type("integer").format("int16"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(int.class, new Schema<>().type("integer").format("int32"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(long.class, new Schema<>().type("integer").format("int64"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(float.class, new Schema<>().type("number").format("float"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(double.class, new Schema<>().type("number").format("double"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(char.class, new Schema<>().type("string").format("char"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(String.class, new Schema<>().type("string"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Boolean.class, new Schema<>().type("boolean"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Byte.class, new Schema<>().type("integer").format("int8"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Short.class, new Schema<>().type("integer").format("int16"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Integer.class, new Schema<>().type("integer").format("int32"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Long.class, new Schema<>().type("integer").format("int64"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Float.class, new Schema<>().type("number").format("float"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Double.class, new Schema<>().type("number").format("double"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Character.class, new Schema<>().type("string").format("char"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Instant.class, new Schema<>().type("string").format("timestamp"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(java.sql.Date.class, new Schema<>().type("string").format("date"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(java.util.Date.class, new Schema<>().type("string").format("date"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Timestamp.class, new Schema<>().type("string").format("timestamp"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(UUID.class, new Schema<>().type("string").format("uuid"));
    }

    public static Schema basicShema(Class<?> clazz) {
        return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(clazz);
    }

    public static Schema singleItemResponse(FieldMetadata fieldMetadata) {
        if (fieldMetadata.isAssociation()) {
            EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
            Schema<String> schema = new Schema<>();
            schema.type("object");
            for (FieldMetadata childKeyField : associationMetadata.getAllFields()) {
                if (!childKeyField.isSerializable() || childKeyField.isAssociation()) continue;
                schema.addProperty(childKeyField.getOutputPropertyName(), generateBasicSchema(childKeyField));
            }
            if (fieldMetadata.isCollection()) {
                return new Schema<>().type("array").items(schema);
            } else {
                return schema;
            }
        } else {
            return generateBasicSchema(fieldMetadata);
        }
    }

    public static Schema generateRequestSchema(FieldMetadata fieldMetadata) {
        if (fieldMetadata.isAssociation()) {
            Schema<String> schema = new Schema<>();
            schema.type("object");
            List<String> required = new LinkedList<>();
            for (FieldMetadata childKeyField : fieldMetadata.getAssociationMetadata().getKeyFields()) {
                if (!fieldMetadata.isSerializable()) continue;
                schema.addProperty(childKeyField.getOutputPropertyName(), generateBasicSchema(childKeyField));
                required.add(childKeyField.getOutputPropertyName());
            }
            schema.required(required);
            if (fieldMetadata.isCollection()) {
                return new Schema<>().type("array").items(schema);
            } else {
                return schema;
            }
        } else {
            return generateBasicSchema(fieldMetadata);
        }
    }

    public static Schema generateBasicSchema(FieldMetadata fieldMetadata) {
        if (PRIMITIVE_TYPE_TO_SCHEMA_MAP.containsKey(fieldMetadata.getFieldType())) {
            return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(fieldMetadata.getFieldType());
        } else {
            return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(UUID.class);
        }
    }

}
