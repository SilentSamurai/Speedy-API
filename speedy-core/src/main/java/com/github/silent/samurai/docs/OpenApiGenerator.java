package com.github.silent.samurai.docs;

import com.github.silent.samurai.interfaces.*;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

public class OpenApiGenerator {

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
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Date.class, new Schema<>().type("string").format("date"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(java.util.Date.class, new Schema<>().type("string").format("date"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(Timestamp.class, new Schema<>().type("string").format("timestamp"));
        PRIMITIVE_TYPE_TO_SCHEMA_MAP.put(UUID.class, new Schema<>().type("string").format("uuid"));
    }

    private final MetaModelProcessor metaModelProcessor;

    public OpenApiGenerator(MetaModelProcessor metaModelProcessor) {
        this.metaModelProcessor = metaModelProcessor;
    }

    private void postOperation(EntityMetadata entityMetadata, OpenAPI openAPI, PathItem pathItem) {
        Operation operation = new Operation();
        operation.operationId("CreateMultiple" + entityMetadata.getName());
        operation.summary("Bulk create " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        operation.requestBody(new RequestBody()
                .description("Fields needed for creation")
                .content(new Content()
                        .addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                                .schema(new Schema<>().$ref(
                                                getSchemaName("post", entityMetadata, true, true)
                                        )
                                )
                        )
                )
        );
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful save.")
        );
        operation.responses(apiResponses);
        pathItem.post(operation);

        Schema<String> createSchema = new Schema<>();
        createSchema.type("object");
        List<String> required = new LinkedList<>();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            createSchema.addProperty(fieldMetadata.getClassFieldName(), resolveFieldSchema(fieldMetadata));
            if (fieldMetadata instanceof KeyFieldMetadata) {
                required.add(fieldMetadata.getClassFieldName());
            }
        }
        createSchema.required(required);
        Components components = openAPI.getComponents();
        components.addSchemas(getSchemaName("post", entityMetadata, false, false), createSchema);
        components.addSchemas(getSchemaName("post", entityMetadata, false, true),
                wrapInArray(
                        getSchemaName("post", entityMetadata, true, false)
                )
        );
    }

    private void putOperation(EntityMetadata entityMetadata, OpenAPI openAPI, PathItem identifierPathItem) {
        Operation operation = new Operation();
        operation.operationId("Update" + entityMetadata.getName());
        operation.summary("Update a(n) " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        operation.addParametersItem(
                new Parameter()
                        .name("identifiers")
                        .in("path")
                        .allowEmptyValue(true)
                        .schema(PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(String.class))
                        .example(getQueryExample(entityMetadata, true))
        );
        operation.requestBody(new RequestBody()
                .description("Fields needed for update")
                .content(new Content()
                        .addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                                .schema(new Schema().$ref(
                                                getSchemaName("put", entityMetadata, true, false)
                                        )
                                )
                        )
                )
        );
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful update.")
        );
        operation.responses(apiResponses);
        identifierPathItem.put(operation);

        Schema<String> updateSchema = new Schema<>();
        updateSchema.type("object");
        Set<FieldMetadata> allFields = entityMetadata.getAllFields();
        for (FieldMetadata fieldMetadata : allFields) {
            if (!(fieldMetadata instanceof KeyFieldMetadata)) {
                updateSchema.addProperty(fieldMetadata.getClassFieldName(), resolveFieldSchema(fieldMetadata));
            }
        }
        openAPI.getComponents().addSchemas(getSchemaName("put", entityMetadata, false, false), updateSchema);
    }

    private void deleteOperation(EntityMetadata entityMetadata, OpenAPI openAPI, PathItem pathItem) {
        Operation operation = new Operation();
        operation.operationId("Delete" + entityMetadata.getName());
        operation.summary("Bulk delete " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));

        operation.requestBody(new RequestBody()
                .description("Fields needed for deletion")
                .content(new Content()
                        .addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                                .schema(new Schema().$ref(
                                                getSchemaName("delete", entityMetadata, true, false)
                                        )
                                )
                        )
                )
        );

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful delete.")
        );
        operation.responses(apiResponses);
        pathItem.delete(operation);

        List<String> required = new LinkedList<>();
        Schema deleteSchema = new Schema();
        deleteSchema.type("object");
        for (KeyFieldMetadata fieldMetadata : entityMetadata.getKeyFields()) {
            deleteSchema.addProperty(fieldMetadata.getClassFieldName(), resolveFieldSchema(fieldMetadata));
            required.add(fieldMetadata.getClassFieldName());
        }
        deleteSchema.required(required);
        openAPI.getComponents()
                .addSchemas(getSchemaName("delete", entityMetadata, false, false), deleteSchema);
    }

    private void getWithFieldQuery(EntityMetadata entityMetadata, OpenAPI openAPI, PathItem queryPathItem) {
        Operation operation = new Operation();
        operation.operationId("Get" + entityMetadata.getName());
        operation.summary("Filter " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        operation.addParametersItem(
                new Parameter()
                        .name("query")
                        .in("path")
                        .allowEmptyValue(true)
                        .schema(PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(String.class))
                        .example(getQueryExample(entityMetadata, false))
        );
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful fetch.")
                .content(new Content()
                        .addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                                .schema(
                                        wrapInPayload(
                                                getSchemaName("get", entityMetadata, true, true)
                                        )
                                )
                        )
                )
        );
        operation.responses(apiResponses);
        queryPathItem.get(operation);
    }

    private void getWithPrimaryFields(EntityMetadata entityMetadata, OpenAPI openAPI, PathItem identifierPathItem) {
        Operation operation = new Operation();
        operation.operationId("GetOne" + entityMetadata.getName());
        operation.summary("Get a(n) " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        operation.addParametersItem(
                new Parameter()
                        .name("identifiers")
                        .in("path")
                        .allowEmptyValue(true)
                        .schema(PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(String.class))
                        .example(getQueryExample(entityMetadata, true))
        );
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful fetch.")
                .content(new Content()
                        .addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                                .schema(
                                        wrapInPayload(
                                                getSchemaName("get", entityMetadata, true, false)
                                        )
                                )
                        )
                )
        );
        operation.responses(apiResponses);
        identifierPathItem.get(operation);
    }

    private void getOperation(EntityMetadata entityMetadata, OpenAPI openAPI, PathItem pathItem) {
        Operation operation = new Operation();
        operation.operationId("GetAll" + entityMetadata.getName());
        operation.summary("Get all " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful fetch.")
                .content(new Content()
                        .addMediaType(APPLICATION_JSON_VALUE, new MediaType()
                                .schema(
                                        wrapInPayload(
                                                getSchemaName("get", entityMetadata, true, true)
                                        )
                                )
                        )
                )
        );
        operation.responses(apiResponses);
        pathItem.get(operation);

        Schema getSchema = new Schema();
        getSchema.type("object");
        Set<FieldMetadata> allFields = entityMetadata.getAllFields();
        for (FieldMetadata fieldMetadata : allFields) {
            getSchema.addProperty(fieldMetadata.getClassFieldName(), resolveFieldSchema(fieldMetadata));
        }
        Components components = openAPI.getComponents();
        components.addSchemas(getSchemaName("get", entityMetadata, false, false), getSchema);
        components.addSchemas(getSchemaName("get", entityMetadata, false, true),
                wrapInArray(
                        getSchemaName("get", entityMetadata, true, false)
                )
        );
    }

    public void generate(OpenAPI openApi) {
        Collection<EntityMetadata> allEntityMetadata = metaModelProcessor.getAllEntityMetadata();
        for (EntityMetadata entityMetadata : allEntityMetadata) {

            PathItem basePathItem = new PathItem();
            PathItem identifierPathItem = new PathItem();
            PathItem queryPathItem = new PathItem();

            postOperation(entityMetadata, openApi, basePathItem);
            putOperation(entityMetadata, openApi, identifierPathItem);
            deleteOperation(entityMetadata, openApi, basePathItem);
            getOperation(entityMetadata, openApi, basePathItem);
            getWithFieldQuery(entityMetadata, openApi, queryPathItem);
            getWithPrimaryFields(entityMetadata, openApi, identifierPathItem);


            openApi.path(getBasePath(entityMetadata), basePathItem);
            openApi.path(getParameterPath(entityMetadata, "identifiers"), identifierPathItem);
            openApi.path(getParameterPath(entityMetadata, "query"), queryPathItem);
        }
    }

    private String getSchemaName(String operation, EntityMetadata entityMetadata, boolean returnRef, boolean isMultiple) {
        StringBuilder sb = new StringBuilder();
        if (returnRef) {
            sb.append("#/components/schemas/");
        }
        if (isMultiple) {
            sb.append("Bulk");
        }
        sb.append(operation).append(entityMetadata.getName());
        return sb.toString();
    }

    private Schema resolveFieldSchema(FieldMetadata fieldMetadata) {
        if (PRIMITIVE_TYPE_TO_SCHEMA_MAP.containsKey(fieldMetadata.getFieldType())) {
            return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(fieldMetadata.getFieldType());
        } else {
            return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(UUID.class);
        }
    }

    private Schema wrapInPayload(String ref) {
        return new Schema<>()
                .type("object")
                .addProperty("payload", new Schema().$ref(ref))
                .addProperty("pageCount", PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(long.class))
                .addProperty("pageIndex", PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(long.class));
    }

    private Schema wrapInArray(String ref) {
        return new Schema<>()
                .type("array")
                .maxItems(100)
                .items(
                        new Schema().$ref(ref)
                );
    }

    private String getBasePath(EntityMetadata entityMetadata) {
        StringBuilder sb = new StringBuilder();
        sb.append(SpeedyConstant.URI).append("/").append(entityMetadata.getName());
        return sb.toString();
    }

    private String getParameterPath(EntityMetadata entityMetadata, String parameterName) {
        StringBuilder sb = new StringBuilder();
        sb.append(SpeedyConstant.URI).append("/").append(entityMetadata.getName());
        sb.append("({").append(parameterName).append("})");
        return sb.toString();
    }

    private String getQueryExample(EntityMetadata entityMetadata, boolean onlyPrimary) {
        StringBuilder sb = new StringBuilder();
        if (onlyPrimary) {
            Iterator<KeyFieldMetadata> iterator = entityMetadata.getKeyFields().iterator();
            while (iterator.hasNext()) {
                KeyFieldMetadata fieldMetadata = iterator.next();
                sb.append(fieldMetadata.getClassFieldName())
                        .append("=")
                        .append("'")
                        .append(resolveFieldSchema(fieldMetadata).getFormat())
                        .append("'");
                if (iterator.hasNext()) {
                    sb.append(", ");
                }
            }
        } else {
            Iterator<FieldMetadata> iterator = entityMetadata.getAllFields().iterator();
            while (iterator.hasNext()) {
                FieldMetadata fieldMetadata = iterator.next();
                sb.append(fieldMetadata.getClassFieldName())
                        .append("=")
                        .append("'")
                        .append(resolveFieldSchema(fieldMetadata).getFormat())
                        .append("'");
                if (iterator.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }
}
