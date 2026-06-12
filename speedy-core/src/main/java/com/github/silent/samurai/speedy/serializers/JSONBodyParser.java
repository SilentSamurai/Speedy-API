package com.github.silent.samurai.speedy.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.io.JsonNode2SpeedyValue;
import com.github.silent.samurai.speedy.mappings.JsonRegistry;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.parser.JsonQueryParser;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static com.github.silent.samurai.speedy.utils.CommonUtil.json;

@Slf4j
public class JSONBodyParser implements IRequestBodyParser {

    /// The JSON registry used for decoding request-body JSON into SpeedyValue instances.
    /// Passed through to {@link JsonNode2SpeedyValue} and {@link MetadataUtil}.
    /// @see JsonRegistry
    private final JsonRegistry jsonRegistry;

    /// Creates a body parser backed by the given JSON registry.
    ///
    /// @param jsonRegistry the registry to use for JSON decoding
    public JSONBodyParser(JsonRegistry jsonRegistry) {
        this.jsonRegistry = jsonRegistry;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public SpeedyQuery parseQuery(byte[] rawBody, MetaModel metaModel, SpeedyQuery baseQuery,
                                  int maxPageSize, int defaultPageSize) throws SpeedyHttpException {
        try {
            JsonNode jsonBody = json().readTree(rawBody);
            JsonQueryParser jsonQueryParser = new JsonQueryParser(metaModel, baseQuery.getFrom(), jsonBody);
            jsonQueryParser.setMaxPageSize(maxPageSize);
            jsonQueryParser.setDefaultPageSize(defaultPageSize);
            jsonQueryParser.setJsonNode2SpeedyValue(new JsonNode2SpeedyValue(jsonRegistry));
            SpeedyQuery query = jsonQueryParser.build();
            if (query instanceof SpeedyQueryImpl impl) {
                impl.setType(SpeedyRequestType.QUERY);
            }
            return query;
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    @Override
    public SpeedyCreateBody parseCreate(byte[] rawBody, EntityMetadata entity, TransactionMode mode,
                                        QueryProcessor queryProcessor) throws SpeedyHttpException {
        try {
            JsonNode jsonBody = json().readTree(rawBody);
            List<SpeedyEntity> entities = parseCreateEntities(entity, queryProcessor, jsonBody);
            return SpeedyCreateBody.builder()
                    .entities(entities)
                    .mode(mode)
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    @Override
    public SpeedyUpdateBody parseUpdate(byte[] rawBody, EntityMetadata entity,
                                        QueryProcessor queryProcessor) throws SpeedyHttpException {
        try {
            JsonNode jsonBody = json().readTree(rawBody);
            if (jsonBody == null || !jsonBody.isObject()) {
                throw new BadRequestException("no content to process");
            }
            ObjectNode objectNode = (ObjectNode) jsonBody;

            SpeedyEntityKey pk = MetadataUtil.createIdentifierFromJSON(entity, objectNode, jsonRegistry);
            if (!queryProcessor.exists(pk)) {
                throw new BadRequestException("Entity not present.");
            }
            SpeedyEntity speedyEntity = MetadataUtil.createEntityFromJSON(entity, objectNode, jsonRegistry);
            log.info(" pk {} -> entity {}", pk, speedyEntity);

            return SpeedyUpdateBody.builder()
                    .entity(speedyEntity)
                    .pk(pk)
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    @Override
    public SpeedyDeleteBody parseDelete(byte[] rawBody, EntityMetadata entity, TransactionMode mode,
                                        QueryProcessor queryProcessor) throws SpeedyHttpException {
        try {
            JsonNode jsonBody = json().readTree(rawBody);
            List<SpeedyEntityKey> keys = parseDeleteKeys(entity, jsonBody);
            return SpeedyDeleteBody.builder()
                    .keys(keys)
                    .mode(mode)
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    private List<SpeedyEntity> parseCreateEntities(EntityMetadata resourceMetadata, QueryProcessor queryProcessor,
                                                   JsonNode jsonElement) throws SpeedyHttpException {
        if (jsonElement == null || !jsonElement.isArray()) {
            throw new BadRequestException("no content to process");
        }
        List<SpeedyEntity> parsedObjects = new LinkedList<>();
        ArrayNode batchOfEntities = (ArrayNode) jsonElement;
        for (JsonNode element : batchOfEntities) {
            if (element.isObject()) {
                ObjectNode objectNode = (ObjectNode) element;
                if (MetadataUtil.isPrimaryKeyComplete(resourceMetadata, objectNode)) {
                    SpeedyEntityKey pk = MetadataUtil.createIdentifierFromJSON(resourceMetadata, objectNode, jsonRegistry);
                    if (queryProcessor.exists(pk)) {
                        throw new BadRequestException("Entity already present.");
                    }
                }
                SpeedyEntity speedyEntity = MetadataUtil.createEntityFromJSON(resourceMetadata, objectNode, jsonRegistry);
                log.info("parsed entity {}", speedyEntity);
                parsedObjects.add(speedyEntity);
            } else {
                throw new BadRequestException("in-valid content");
            }
        }
        return parsedObjects;
    }

    private List<SpeedyEntityKey> parseDeleteKeys(EntityMetadata resourceMetadata,
                                                  JsonNode jsonElement) throws SpeedyHttpException {
        List<SpeedyEntityKey> keysToBeRemoved = new LinkedList<>();
        if (jsonElement == null || !jsonElement.isArray()) {
            throw new BadRequestException("in-valid request");
        }
        ArrayNode batchOfEntities = (ArrayNode) jsonElement;
        for (JsonNode element : batchOfEntities) {
            if (element.isObject()) {
                ObjectNode objectNode = (ObjectNode) element;
                if (!MetadataUtil.isPrimaryKeyComplete(resourceMetadata, objectNode)) {
                    throw new BadRequestException("Primary Key Incomplete ");
                }
                SpeedyEntityKey pk = MetadataUtil.createIdentifierFromJSON(resourceMetadata, objectNode, jsonRegistry);
                keysToBeRemoved.add(pk);
                log.info("parsed primary key {}", pk);
            } else {
                throw new BadRequestException("in-valid request body");
            }
        }
        return keysToBeRemoved;
    }
}
