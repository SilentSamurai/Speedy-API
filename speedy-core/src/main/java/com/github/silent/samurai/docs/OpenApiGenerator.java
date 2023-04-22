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


    private Operation getPostOperation(EntityMetadata entityMetadata) {
        Operation operation = new Operation();
        operation.operationId("CreateMultiple" + entityMetadata.getName());
        operation.summary("Bulk create " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));

        operation.requestBody(new RequestBody()
                .description("Fields needed for creation")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema()
                                        .$ref("#/components/schemas/bulkCreate" + entityMetadata.getName())
                                )
                        )
                )
        );

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful save.")
        );

        operation.responses(apiResponses);
        return operation;
    }

    private Operation getPutOperation(EntityMetadata entityMetadata) {
        Operation operation = new Operation();
        operation.operationId("Update" + entityMetadata.getName());
        operation.summary("Update a " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));

        operation.requestBody(new RequestBody()
                .description("Fields needed for update")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema()
                                        .$ref("#/components/schemas/update" + entityMetadata.getName())
                                )
                        )
                )
        );

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful update.")
        );

        operation.responses(apiResponses);
        return operation;
    }

    private Operation getDeleteOperation(EntityMetadata entityMetadata) {
        Operation operation = new Operation();
        operation.operationId("Delete" + entityMetadata.getName());
        operation.summary("Bulk delete " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));

        operation.requestBody(new RequestBody()
                .description("Fields needed for deletion")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema()
                                        .$ref("#/components/schemas/delete" + entityMetadata.getName())
                                )
                        )
                )
        );

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful delete.")
        );

        operation.responses(apiResponses);
        return operation;
    }

    private Operation GETOperation(EntityMetadata entityMetadata) {
        Operation operation = new Operation();
        operation.operationId("Get" + entityMetadata.getName());
        operation.summary("Get (one|many) " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));

        operation.parameters(Lists.newArrayList(
                new Parameter()
                        .schema(new Schema()
                                .$ref("#/components/schemas/get" + entityMetadata.getName())
                        )
        ));

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", new ApiResponse()
                .description("successful fetch.")
                .$ref("#/components/schemas/bulkget" + entityMetadata.getName())
        );

        operation.responses(apiResponses);
        return operation;
    }

    public void generate(OpenAPI openApi) {

        Collection<EntityMetadata> allEntityMetadata = metaModelProcessor.getAllEntityMetadata();
        Components components = openApi.getComponents();

        for (EntityMetadata entityMetadata : allEntityMetadata) {

            PathItem entityPathItem = new PathItem().description("this is a dummy description");
            entityPathItem.description("");


            entityPathItem.post(getPostOperation(entityMetadata));
            entityPathItem.put(getPutOperation(entityMetadata));
            entityPathItem.delete(getDeleteOperation(entityMetadata));
            entityPathItem.get(GETOperation(entityMetadata));

            openApi.path(SpeedyConstant.URI + "/" + entityMetadata.getName(), entityPathItem);


            Set<FieldMetadata> allFields = entityMetadata.getAllFields();
            Schema createSchema = new Schema();
            Schema deleteSchema = new Schema();
            Schema updateSchema = new Schema();
            Schema getSchema = new Schema();
            createSchema.type("object");
            deleteSchema.type("object");
            updateSchema.type("object");
            getSchema.type("object");
            List<String> required = new LinkedList<>();
            for (FieldMetadata fieldMetadata : allFields) {
                getSchema.addProperty(fieldMetadata.getClassFieldName(), resolveFieldSchema(fieldMetadata));
                createSchema.addProperty(fieldMetadata.getClassFieldName(), resolveFieldSchema(fieldMetadata));
                if (fieldMetadata instanceof KeyFieldMetadata) {
                    deleteSchema.addProperty(fieldMetadata.getClassFieldName(), resolveFieldSchema(fieldMetadata));
                    required.add(fieldMetadata.getClassFieldName());
                } else {
                    updateSchema.addProperty(fieldMetadata.getClassFieldName(), resolveFieldSchema(fieldMetadata));
                }
            }
            createSchema.required(required);
            deleteSchema.required(required);

            components.addSchemas("get" + entityMetadata.getName(), getSchema);
            components.addSchemas("bulkget" + entityMetadata.getName(), new Schema()
                    .type("array")
                    .maxItems(100)
                    .items(new Schema()
                            .$ref("#/components/schemas/get" + entityMetadata.getName())
                    )
            );
            components.addSchemas("delete" + entityMetadata.getName(), deleteSchema);
            components.addSchemas("update" + entityMetadata.getName(), updateSchema);
            components.addSchemas("create" + entityMetadata.getName(), createSchema);
            components.addSchemas("bulkCreate" + entityMetadata.getName(), new Schema()
                    .type("array")
                    .maxItems(100)
                    .items(new Schema()
                            .$ref("#/components/schemas/create" + entityMetadata.getName())
                    )
            );


        }

    }

    private Schema resolveFieldSchema(FieldMetadata fieldMetadata) {
        if (PRIMITIVE_TYPE_TO_SCHEMA_MAP.containsKey(fieldMetadata.getFieldType())) {
            return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(fieldMetadata.getFieldType());
        } else {
            return PRIMITIVE_TYPE_TO_SCHEMA_MAP.get(UUID.class);
        }
    }
}
