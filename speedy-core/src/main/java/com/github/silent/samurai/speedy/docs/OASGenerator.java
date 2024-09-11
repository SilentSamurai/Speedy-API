package com.github.silent.samurai.speedy.docs;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
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
    public static final String GET_REQUEST_NAME = "Get{0}Request";
    public static final String QUERY_REQUEST_NAME = "QueryRequest";
    public static final String QUERY_REQUEST_WHERE_NAME = "QueryRequestWhere";


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

    public static Schema basicSchema(ValueType valueType) {
        switch (valueType) {
            case BOOL:
                return new Schema<>().type("boolean");
            case TEXT:
                return new Schema<>().type("string");
            case INT:
                return new Schema<>().type("integer").format("int64");
            case FLOAT:
                return new Schema<>().type("number").format("double");
            case DATE_TIME:
            case DATE:
            case TIME:
                return new Schema<>().type("string");
            case OBJECT:
            case COLLECTION:
            case NULL:
            default:
                return new Schema<>().type("string");
        }
    }

    public static Schema generateBasicSchema(FieldMetadata fieldMetadata) {
        return basicSchema(fieldMetadata.getValueType());
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
                                            String associationFormat,
                                            boolean isRequest) {
        Schema<String> schema = new Schema<>();
        schema.type("object");
        List<String> required = new LinkedList<>();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (predicate.test(fieldMetadata)) {
                Schema fieldSchema = createFieldSchema(fieldMetadata, associationFormat);
                schema.addProperty(fieldMetadata.getOutputPropertyName(), fieldSchema);
                if (isRequest && fieldMetadata.isRequired()) {
                    required.add(fieldMetadata.getOutputPropertyName());
                }
            }
        }
        if (!required.isEmpty()) {
            schema.required(required);
        }
        return schema;
    }

    public static Schema<?> createWhereQuerySchema() {
        Schema whereSchema = new Schema<>().type("object");

        Schema conditions = new Schema<>().type("object")
                .addProperty("$eq", new Schema<>())
//                                .addProperty("=", new Schema<>())
//                                .addProperty("==", new Schema<>())
                .addProperty("$neq", new Schema<>())
                .addProperty("$ne", new Schema<>())
//                                .addProperty("!=", new Schema<>())
                .addProperty("$lt", new Schema<>())
//                                .addProperty("<", new Schema<>())
                .addProperty("$lte", new Schema<>())
//                                .addProperty("<=", new Schema<>())
                .addProperty("$gt", new Schema<>())
//                                .addProperty(">", new Schema<>())
                .addProperty("$gte", new Schema<>())
//                                .addProperty(">=", new Schema<>())
                .addProperty("$in", new Schema<>())
//                                .addProperty("<>", new Schema<>())
                .addProperty("$nin", new Schema<>())
//                                .addProperty("<!>", new Schema<>())
                .addProperty("$and", new Schema().type("array").items(getSchemaRef(QUERY_REQUEST_WHERE_NAME)))
                .addProperty("$or", new Schema().type("array").items(getSchemaRef(QUERY_REQUEST_WHERE_NAME)));

        whereSchema.additionalProperties(new Schema<>().anyOf(
                        List.of(
                                basicSchema(ValueType.TEXT),
                                conditions)
                )
        );

        whereSchema
                .addProperty("$and", new Schema().type("array").items(getSchemaRef(QUERY_REQUEST_WHERE_NAME)))
                .addProperty("$or", new Schema().type("array").items(getSchemaRef(QUERY_REQUEST_WHERE_NAME)));

        return whereSchema;
    }

    public static Schema<?> createQueryRequest() {
        Schema<String> schema = new Schema<>();
        schema.type("object");

//        schema.addProperty("$from", basicSchema(ValueType.TEXT));

        Schema orderBySchema = new Schema<>().type("object");


        orderBySchema.additionalProperties(basicSchema(ValueType.TEXT).example("ASC|DESC"));


//        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
//            String outputPropertyName = fieldMetadata.getOutputPropertyName();
//
////            orderBySchema.addPatternProperty("^[A-Za-z][A-Za-z0-9_]$", basicSchema(ValueType.TEXT).example("ASC|DESC"));
//            orderBySchema.addProperty(outputPropertyName, basicSchema(ValueType.TEXT).example("ASC|DESC"));
//            whereSchema.addProperty(outputPropertyName, new Schema<>().anyOf(
//                    List.of(
//                            new Schema<>().type("object")
//                                    .addProperty("$eq", basicSchema(ValueType.TEXT)),
//                            basicSchema(ValueType.TEXT)
//                    )
//            ));
//
//        }

//
//        String expandExample = entityMetadata
//                .getAssociatedFields()
//                .stream()
//                .map(FieldMetadata::getOutputPropertyName)
//                .reduce((a, b) -> a + "|" + b).orElse("");

        schema.addProperty("$orderBy", orderBySchema);
        schema.addProperty("$where", getSchemaRef(QUERY_REQUEST_WHERE_NAME));

        Schema<?> expandSchema = new Schema<>().type("array")
                .items(basicSchema(ValueType.TEXT).example("field names"));
        schema.addProperty("$expand", expandSchema);

        Schema pageSchema = new Schema<>().type("object");
        pageSchema.addProperty("$index", basicSchema(ValueType.INT));
        pageSchema.addProperty("$size", basicSchema(ValueType.INT));
        schema.addProperty("$page", pageSchema);
        return schema;
    }

    public static Schema wrapInArray(Schema ref) {
        return new Schema<>().type("array").items(ref);
    }

    public static Schema wrapInPayload(Schema ref) {
        return new Schema<>()
                .type("object")
                .addProperty("payload", ref)
                .addProperty("pageCount", OASGenerator.basicSchema(ValueType.INT))
                .addProperty("pageIndex", OASGenerator.basicSchema(ValueType.INT));
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

    public static ApiResponse getJsonResponse(String name, Schema schema) {
        return new ApiResponse()
                .content(new Content()
                        .addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                                .schema(wrapInPayload(schema).name(name).title(name))
                        )
                );
    }


    public static void addPrimaryKeyParameter(Operation operation, EntityMetadata entityMetadata) {
        for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
            operation.addParametersItem(
                    new Parameter()
                            .name(keyField.getOutputPropertyName())
                            .description(keyField.getOutputPropertyName() + " field value.")
                            .in("path")
                            .allowEmptyValue(false)
                            .schema(OASGenerator.generateBasicSchema(keyField))
            );
        }
    }


}
