package com.github.silent.samurai.docs;

import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

public class OASGenerator {

    public static final String LIGHT_ENTITY_NAME = "Light{0}";
    public static final String ENTITY_NAME = "{0}";
    public static final String ENTITY_KEY = "{0}Key";
    public static final String CREATE_REQUEST_NAME = "Create{0}Request";
    public static final String UPDATE_REQUEST_NAME = "Update{0}Request";


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
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(java.sql.Date.class, new Schema<>().type("string").format("date"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(java.util.Date.class, new Schema<>().type("string").format("date"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Instant.class, new Schema<>().type("string").format("date-time"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Timestamp.class, new Schema<>().type("string").format("date-time"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(UUID.class, new Schema<>().type("string").format("uuid"));
    }

    public static Schema basicSchema(Class<?> clazz) {
        return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(clazz);
    }

    public static Schema generateBasicSchema(FieldMetadata fieldMetadata) {
        if (PRIMITIVE_TYPE_TO_SCHEMA_MAP.containsKey(fieldMetadata.getFieldType())) {
            return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(fieldMetadata.getFieldType());
        } else {
            return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(String.class);
        }
    }

    public static String getQueryExample(EntityMetadata entityMetadata, Predicate<FieldMetadata> predicate) {
        StringBuilder sb = new StringBuilder();
        Iterator<FieldMetadata> iterator = entityMetadata.getAllFields().iterator();
        while (iterator.hasNext()) {
            FieldMetadata fieldMetadata = iterator.next();
            if (predicate.test(fieldMetadata)) {
                sb.append(fieldMetadata.getOutputPropertyName())
                        .append("=")
                        .append("'")
                        .append(OASGenerator.generateBasicSchema(fieldMetadata).getFormat())
                        .append("'");
                if (iterator.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    public static Schema createFieldSchema(FieldMetadata fieldMetadata, String associationRef) {
        if (fieldMetadata.isAssociation()) {
            Schema schema = getSchemaRef(getSchemaName(associationRef, fieldMetadata.getAssociationMetadata()));
            if (fieldMetadata.isCollection()) {
                return new Schema<>().type("array").items(schema);
            } else {
                return schema;
            }
        } else {
            return generateBasicSchema(fieldMetadata);
        }
    }

    public static Schema createEntitySchema(EntityMetadata entityMetadata,
                                            Predicate<FieldMetadata> predicate,
                                            String associationFormat) {
        Schema<String> schema = new Schema<>();
        schema.type("object");
        List<String> required = new LinkedList<>();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (predicate.test(fieldMetadata)) {
                Schema fieldSchema = createFieldSchema(fieldMetadata, associationFormat);
                schema.addProperty(fieldMetadata.getOutputPropertyName(), fieldSchema);
                if (fieldMetadata instanceof KeyFieldMetadata || !fieldMetadata.isNullable()) {
                    required.add(fieldMetadata.getOutputPropertyName());
                }
            }
        }
        schema.required(required);
        return schema;
    }

    public static Schema wrapInArray(Schema ref) {
        return new Schema<>().type("array").maxItems(100).items(ref);
    }

    public static Schema wrapInPayload(Schema ref) {
        return new Schema<>()
                .type("object")
                .addProperty("payload", ref)
                .addProperty("pageCount", OASGenerator.basicSchema(long.class))
                .addProperty("pageIndex", OASGenerator.basicSchema(long.class));
    }

    public static Schema getSchemaRef(String schemaName) {
        return new Schema<>().$ref("#/components/schemas/" + schemaName);
    }

    public static String getSchemaName(String format, EntityMetadata entityMetadata) {
        return MessageFormat.format(format, entityMetadata.getName());
    }

    public static RequestBody getJsonBody(Schema schema) {
        return new RequestBody()
                .content(new Content()
                        .addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                                .schema(schema)
                        )
                );
    }

    public static ApiResponse getJsonResponse(Schema schema) {
        return new ApiResponse()
                .content(new Content()
                        .addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                                .schema(wrapInPayload(schema))
                        )
                );
    }


}
