package com.github.silent.samurai.speedy.docs;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.*;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.Collection;
import java.util.Iterator;

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
            getWithPrimaryFields(entityMetadata, identifierPathItem);
//            getOperation(entityMetadata, queryPathItem);
            getWithFieldQuery(entityMetadata, queryPathItem);


            createSchemas(entityMetadata, openApi);

            String basePath = String.format("%s/%s", SpeedyConstant.URI, entityMetadata.getName());
            openApi.path(basePath, basePathItem);

            String querypath = String.format("%s/%s/$query", SpeedyConstant.URI, entityMetadata.getName());
            openApi.path(querypath, queryPathItem);

            openApi.path(getIdentifierPath(entityMetadata), identifierPathItem);
        }
    }

    private void createSchemas(EntityMetadata entityMetadata, OpenAPI openAPI) {
//        Schema<String> getSchema = OASGenerator.createEntitySchema(
//                entityMetadata,
//                fm -> fm.isSerializable() && ((fm.isCollection() && !fm.isAssociation()) || !fm.isCollection()),
//                OASGenerator.LIGHT_ENTITY_NAME,
//                false
//        );
//        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.LIGHT_ENTITY_NAME, entityMetadata), getSchema);

        Schema<String> light = OASGenerator.createEntitySchema(
                entityMetadata,
                FieldMetadata::isSerializable,
                OASGenerator.ENTITY_NAME,
                false
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.ENTITY_NAME, entityMetadata), light);

        Schema entityKeySchema = OASGenerator.createEntitySchema(
                entityMetadata,
                KeyFieldMetadata.class::isInstance,
                OASGenerator.ENTITY_KEY,
                true
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.ENTITY_KEY, entityMetadata), entityKeySchema);

        Schema createSchema = OASGenerator.createEntitySchema(
                entityMetadata,
                FieldMetadata::isInsertable,
                OASGenerator.ENTITY_KEY,
                true
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.CREATE_REQUEST_NAME, entityMetadata), createSchema);

        Schema<String> updateSchema = OASGenerator.createEntitySchema(
                entityMetadata,
                fm -> !(fm instanceof KeyFieldMetadata) && fm.isUpdatable(),
                OASGenerator.ENTITY_KEY,
                true
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.UPDATE_REQUEST_NAME, entityMetadata), updateSchema);

        Schema<?> querySchema = OASGenerator.createQueryRequest(
                entityMetadata
        );
        openAPI.getComponents().addSchemas(OASGenerator.getSchemaName(OASGenerator.QUERY_REQUEST_NAME, entityMetadata), querySchema);
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
                OASGenerator.getSchemaName("BulkCreate{0}Response", entityMetadata),
                OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_KEY, entityMetadata))
                )
        ).description("successful creation."));
        operation.responses(apiResponses);
        pathItem.post(operation);
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
                OASGenerator.getSchemaName("BulkDelete{0}Response", entityMetadata),
                OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_KEY, entityMetadata))
                )
        ).description("successful deletion."));
        operation.responses(apiResponses);
        pathItem.delete(operation);
    }

    private void putOperation(EntityMetadata entityMetadata, PathItem identifierPathItem) {
        Operation operation = new Operation();
        operation.operationId("Update" + entityMetadata.getName());
        operation.summary("Update a(n) " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));

        OASGenerator.addPrimaryKeyParameter(operation, entityMetadata);

        operation.requestBody(OASGenerator.getJsonBody(
                OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.UPDATE_REQUEST_NAME, entityMetadata))
        ).description("Fields needed for update"));
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", OASGenerator.getJsonResponse(
                OASGenerator.getSchemaName("Update{0}Response", entityMetadata),
                OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_NAME, entityMetadata))
        ).description("successful update."));
        operation.responses(apiResponses);
        identifierPathItem.put(operation);
    }


    private void getWithPrimaryFields(EntityMetadata entityMetadata, PathItem identifierPathItem) {
        Operation operation = new Operation();
        operation.operationId("Get" + entityMetadata.getName());
        operation.summary("Get a(n) " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));

        OASGenerator.addPrimaryKeyParameter(operation, entityMetadata);

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", OASGenerator.getJsonResponse(
                OASGenerator.getSchemaName("Filtered{0}Response", entityMetadata),
                OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_NAME, entityMetadata))
                )
        ).description("successful fetch."));
        operation.responses(apiResponses);
        identifierPathItem.get(operation);
    }

    private void getWithFieldQuery(EntityMetadata entityMetadata, PathItem queryPathItem) {
        Operation operation = new Operation();
        operation.operationId("Query" + entityMetadata.getName());
        operation.summary("Filter " + entityMetadata.getName());
        operation.tags(Lists.newArrayList(entityMetadata.getName()));
//        OASGenerator.addPagingAndOrderingInfo(operation);
        operation.requestBody(OASGenerator.getJsonBody(
                OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.QUERY_REQUEST_NAME, entityMetadata))
        ).description("Fields needed for creation"));
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", OASGenerator.getJsonResponse(
                OASGenerator.getSchemaName("Filtered{0}Response", entityMetadata),
                OASGenerator.wrapInArray(
                        OASGenerator.getSchemaRef(OASGenerator.getSchemaName(OASGenerator.ENTITY_NAME, entityMetadata))
                )
        ).description("successful fetch."));
        operation.responses(apiResponses);
        queryPathItem.post(operation);
    }

    private String getIdentifierPath(EntityMetadata entityMetadata) {
        StringBuilder sb = new StringBuilder();
        sb.append(SpeedyConstant.URI).append("/").append(entityMetadata.getName());
        sb.append("(");
        Iterator<KeyFieldMetadata> iterator = entityMetadata.getKeyFields().iterator();
        while (iterator.hasNext()) {
            KeyFieldMetadata keyField = iterator.next();
            sb.append(keyField.getOutputPropertyName())
                    .append("=")
                    .append("\"{")
                    .append(keyField.getOutputPropertyName())
                    .append("}\"");
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }


}
