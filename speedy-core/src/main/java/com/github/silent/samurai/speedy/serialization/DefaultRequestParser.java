package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyRequestReader;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader.Kind;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyCreateBody;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;

import java.util.LinkedList;
import java.util.List;

/// Format-agnostic {@link IRequestBodyParser}: owns request-body envelope composition
/// (the create/delete arrays, the update object + primary-key checks) and delegates the
/// entity tree to {@link StructureToSpeedy}, pulling tokens from a per-call
/// {@link StructureReader}. The read-side mirror of {@code DefaultResponseSerializer};
/// the only format-specific piece is the {@link SpeedyRequestReader}.
public class DefaultRequestParser implements IRequestBodyParser {

    private final String contentType;
    private final SpeedyRequestReader reader;
    private final StructureToSpeedy builder = new StructureToSpeedy();
    private final StructureToQuery queryBuilder = new StructureToQuery();

    public DefaultRequestParser(String contentType, SpeedyRequestReader reader) {
        this.contentType = contentType;
        this.reader = reader;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public SpeedyQuery parseQuery(byte[] rawBody, MetaModel metaModel, SpeedyQuery baseQuery,
                                  int maxPageSize, int defaultPageSize) throws SpeedyHttpException {
        try (StructureReader r = reader.readDocument(rawBody)) {
            return queryBuilder.parse(baseQuery.getFrom(), r, maxPageSize, defaultPageSize);
        }
    }

    @Override
    public SpeedyCreateBody parseCreate(byte[] rawBody, EntityMetadata entity, TransactionMode mode,
                                        QueryProcessor queryProcessor) throws SpeedyHttpException {
        List<SpeedyEntity> entities = new LinkedList<>();
        List<SpeedyEntityKey> keysToCheck = new LinkedList<>();
        try (StructureReader r = reader.readDocument(rawBody)) {
            // Accept either a bare JSON object (single entity shorthand) or an
            // array of objects (one or more entities); anything else is rejected.
            Kind rootKind = r.begin();
            if (rootKind == Kind.ARRAY) {
                Kind elementKind;
                while ((elementKind = r.nextElement()) != null) {
                    if (elementKind != Kind.OBJECT) {
                        throw new BadRequestException("in-valid content");
                    }
                    addParsedEntity(r, entity, entities, keysToCheck);
                }
            } else if (rootKind == Kind.OBJECT) {
                addParsedEntity(r, entity, entities, keysToCheck);
            } else {
                throw new BadRequestException("no content to process");
            }
        }
        // Reject multi-element (bulk) requests for entities that opted out via @SpeedyBulk(false).
        // Placed before the existence check so a disabled-bulk request fails fast.
        if (entities.size() > 1 && !entity.isBulkAllowed()) {
            throw new BadRequestException("Bulk create is disabled for entity '" + entity.getName() + "'");
        }
        // Batch existence check: one query instead of N
        if (!keysToCheck.isEmpty()) {
            java.util.Set<SpeedyEntityKey> existing = queryProcessor.findExistingKeys(keysToCheck);
            if (!existing.isEmpty()) {
                throw new BadRequestException("Entity already present.");
            }
        }
        return SpeedyCreateBody.builder()
                .entities(entities)
                .mode(mode)
                .build();
    }

    @Override
    public SpeedyUpdateBody parseUpdate(byte[] rawBody, EntityMetadata entity, TransactionMode mode,
                                        QueryProcessor queryProcessor) throws SpeedyHttpException {
        List<SpeedyUpdateBody.Item> items = new LinkedList<>();
        try (StructureReader r = reader.readDocument(rawBody)) {
            // Accept either a bare JSON object (single-item shorthand) or an array of
            // objects (one or more items), symmetric with parseCreate/parseDelete.
            Kind rootKind = r.begin();
            if (rootKind == Kind.ARRAY) {
                Kind elementKind;
                while ((elementKind = r.nextElement()) != null) {
                    if (elementKind != Kind.OBJECT) {
                        throw new BadRequestException("in-valid content");
                    }
                    items.add(parseOneUpdateItem(r, entity));
                }
            } else if (rootKind == Kind.OBJECT) {
                items.add(parseOneUpdateItem(r, entity));
            } else {
                throw new BadRequestException("no content to process");
            }
        }
        // Reject multi-element (bulk) requests for entities that opted out via @SpeedyBulk(false).
        if (items.size() > 1 && !entity.isBulkAllowed()) {
            throw new BadRequestException("Bulk update is disabled for entity '" + entity.getName() + "'");
        }
        return SpeedyUpdateBody.builder()
                .items(items)
                .mode(mode)
                .build();
    }

    /// Parses one update item (fields + primary key) from the reader's current object token.
    /// Distinguishes a malformed request (incomplete primary key -> 400) from a well-formed
    /// request whose target row is absent, which is checked later, per-item, by the handler
    /// (-> 404). Shared by both PATCH (UPDATE) and PUT (REPLACE), which parse the same body shape.
    private SpeedyUpdateBody.Item parseOneUpdateItem(StructureReader r, EntityMetadata entity)
            throws SpeedyHttpException {
        SpeedyEntity parsed = builder.fromEntity(entity, r);
        if (!builder.isKeyComplete(entity, parsed)) {
            throw new BadRequestException("Primary key incomplete");
        }
        SpeedyEntityKey pk = builder.toKey(entity, parsed);
        return SpeedyUpdateBody.Item.builder()
                .entity(parsed)
                .pk(pk)
                .build();
    }

    @Override
    public SpeedyDeleteBody parseDelete(byte[] rawBody, EntityMetadata entity, TransactionMode mode,
                                        QueryProcessor queryProcessor) throws SpeedyHttpException {
        List<SpeedyEntityKey> keys = new LinkedList<>();
        try (StructureReader r = reader.readDocument(rawBody)) {
            // Symmetric with parseCreate: accept a bare JSON object (single key
            // shorthand) or an array of key objects.
            Kind rootKind = r.begin();
            if (rootKind == Kind.ARRAY) {
                Kind elementKind;
                while ((elementKind = r.nextElement()) != null) {
                    if (elementKind != Kind.OBJECT) {
                        throw new BadRequestException("in-valid request body");
                    }
                    keys.add(parseOneKey(r, entity));
                }
            } else if (rootKind == Kind.OBJECT) {
                keys.add(parseOneKey(r, entity));
            } else {
                throw new BadRequestException("in-valid request");
            }
        }
        // Reject multi-element (bulk) requests for entities that opted out via @SpeedyBulk(false).
        if (keys.size() > 1 && !entity.isBulkAllowed()) {
            throw new BadRequestException("Bulk delete is disabled for entity '" + entity.getName() + "'");
        }
        return SpeedyDeleteBody.builder()
                .keys(keys)
                .mode(mode)
                .build();
    }

    /// Parses one entity from the reader's current object token, appending it to {@code entities}
    /// and — when its primary key is fully present — its key to {@code keysToCheck}.
    private void addParsedEntity(StructureReader r, EntityMetadata entity,
                                 List<SpeedyEntity> entities, List<SpeedyEntityKey> keysToCheck)
            throws SpeedyHttpException {
        SpeedyEntity parsed = builder.fromEntity(entity, r);
        if (builder.isKeyComplete(entity, parsed)) {
            keysToCheck.add(builder.toKey(entity, parsed));
        }
        entities.add(parsed);
    }

    /// Parses one key object from the reader's current object token, requiring a complete primary key.
    private SpeedyEntityKey parseOneKey(StructureReader r, EntityMetadata entity) throws SpeedyHttpException {
        SpeedyEntity parsed = builder.fromEntity(entity, r);
        if (!builder.isKeyComplete(entity, parsed)) {
            throw new BadRequestException("Primary Key Incomplete ");
        }
        return builder.toKey(entity, parsed);
    }
}
