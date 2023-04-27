package com.github.silent.samurai.docs;

import com.github.silent.samurai.interfaces.*;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.Collection;

public class OpenApiGenerator {

    private final MetaModelProcessor metaModelProcessor;

    public OpenApiGenerator(MetaModelProcessor metaModelProcessor) {
        this.metaModelProcessor = metaModelProcessor;
    }

    public void generate(OpenAPI openApi) {
        Collection<EntityMetadata> allEntityMetadata = metaModelProcessor.getAllEntityMetadata();
        for (EntityMetadata entityMetadata : allEntityMetadata) {

            PathItem basePathItem = new PathItem();
            PathItem identifierPathItem = new PathItem();
            PathItem queryPathItem = new PathItem();

            postOperation(entityMetadata, basePathItem);
            putOperation(entityMetadata, identifierPathItem);
            deleteOperation(entityMetadata, basePathItem);
            getOperation(entityMetadata, basePathItem);
            getWithFieldQuery(entityMetadata, queryPathItem);
            getWithPrimaryFields(entityMetadata, identifierPathItem);

            createSchemas(entityMetadata, openApi);


            openApi.path(getBasePath(entityMetadata), basePathItem);
            openApi.path(getParameterPath(entityMetadata, "identifiers"), identifierPathItem);
            openApi.path(getParameterPath(entityMetadata, "query"), queryPathItem);
        }
    }

    private void createSchemas(EntityMetadata entityMetadata, OpenAPI openAPI) {
        Schema<String> getSchema = OASGenerator.createEntitySchema(
                entityMetadata,
                fm -> fm.isSerializable() && !fm.isAssociation(),
                ""
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.LIGHT_ENTITY_NAME, entityMetadata), getSchema);

        Schema<String> light = OASGenerator.createEntitySchema(
                entityMetadata,
                FieldMetadata::isSerializable,
                OASGenerator.LIGHT_ENTITY_NAME
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.ENTITY_NAME, entityMetadata), light);

        Schema entityKeySchema = OASGenerator.createEntitySchema(
                entityMetadata,
                KeyFieldMetadata.class::isInstance,
                OASGenerator.ENTITY_KEY
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.ENTITY_KEY, entityMetadata), entityKeySchema);

        Schema createSchema = OASGenerator.createEntitySchema(
                entityMetadata,
                FieldMetadata::isInsertable,
                OASGenerator.ENTITY_KEY
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.CREATE_REQUEST_NAME, entityMetadata), createSchema);

        Schema<String> updateSchema = OASGenerator.createEntitySchema(
                entityMetadata,
                fm -> !(fm instanceof KeyFieldMetadata) && fm.isUpdatable(),
                OASGenerator.ENTITY_KEY
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.UPDATE_REQUEST_NAME, entityMetadata), updateSchema);
    }

    private void postOperation(EntityMetadata entityMetadata, PathItem pathItem) {
        Operation operation = new Operation();
        operation.operationId("BulkCreate" + entityMetadata.getName());
        operation.summary("Bulk create " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        operation.requestBody(OASGenerator.getJsonBody(
                OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.CREATE_REQUEST_NAME, entityMetadata))
                )
        ).description("Fields needed for creation"));
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", OASGenerator.getJsonResponse(
                OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_KEY, entityMetadata))
                )
        ).description("successful creation."));
        operation.responses(apiResponses);
        pathItem.post(operation);
    }

    private void putOperation(EntityMetadata entityMetadata, PathItem identifierPathItem) {
        Operation operation = new Operation();
        operation.operationId("Update" + entityMetadata.getName());
        operation.summary("Update a(n) " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        operation.addParametersItem(
                new Parameter()
                        .name("identifiers")
                        .in("path")
                        .allowEmptyValue(true)
                        .schema(OASGenerator.basicSchema(String.class))
                        .example(OASGenerator.getQueryExample(entityMetadata, KeyFieldMetadata.class::isInstance))
        );
        operation.requestBody(OASGenerator.getJsonBody(
                OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.UPDATE_REQUEST_NAME, entityMetadata))
        ).description("Fields needed for update"));
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", OASGenerator.getJsonResponse(
                OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_NAME, entityMetadata))
        ).description("successful update."));
        operation.responses(apiResponses);
        identifierPathItem.put(operation);
    }

    private void deleteOperation(EntityMetadata entityMetadata, PathItem pathItem) {
        Operation operation = new Operation();
        operation.operationId("BulkDelete" + entityMetadata.getName());
        operation.summary("Bulk delete " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        operation.requestBody(OASGenerator.getJsonBody(OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_KEY, entityMetadata))
                )
        ).description("Fields needed for deletion"));
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", OASGenerator.getJsonResponse(
                OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_KEY, entityMetadata))
                )
        ).description("successful deletion."));
        operation.responses(apiResponses);
        pathItem.delete(operation);
    }

    private void getWithFieldQuery(EntityMetadata entityMetadata, PathItem queryPathItem) {
        Operation operation = new Operation();
        operation.operationId("GetSome" + entityMetadata.getName());
        operation.summary("Filter " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        operation.addParametersItem(
                new Parameter()
                        .description("these are queries on the entity")
                        .name("query")
                        .in("path")
                        .allowEmptyValue(true)
                        .schema(OASGenerator.basicSchema(String.class))
                        .example(OASGenerator.getQueryExample(entityMetadata, FieldMetadata::isSerializable))
        );
        OASGenerator.addPagingInfo(operation);
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", OASGenerator.getJsonResponse(
                OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.LIGHT_ENTITY_NAME, entityMetadata))
                )
        ).description("successful fetch."));
        operation.responses(apiResponses);
        queryPathItem.get(operation);
    }

    private void getWithPrimaryFields(EntityMetadata entityMetadata, PathItem identifierPathItem) {
        Operation operation = new Operation();
        operation.operationId("Get" + entityMetadata.getName());
        operation.summary("Get a(n) " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        operation.addParametersItem(
                new Parameter()
                        .name("identifiers")
                        .description("query with all primary keys")
                        .in("path")
                        .allowEmptyValue(true)
                        .schema(OASGenerator.basicSchema(String.class))
                        .example(OASGenerator.getQueryExample(entityMetadata, KeyFieldMetadata.class::isInstance))
        );
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", OASGenerator.getJsonResponse(
                OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_NAME, entityMetadata))
        ).description("successful fetch."));
        operation.responses(apiResponses);
        identifierPathItem.get(operation);
    }

    private void getOperation(EntityMetadata entityMetadata, PathItem pathItem) {
        Operation operation = new Operation();
        operation.operationId("GetAll" + entityMetadata.getName());
        operation.summary("Get all " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
        OASGenerator.addPagingInfo(operation);
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", OASGenerator.getJsonResponse(
                OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.LIGHT_ENTITY_NAME, entityMetadata))
                )
        ).description("successful fetch."));
        operation.responses(apiResponses);
        pathItem.get(operation);
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


}
