package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyRequestReader;
import com.github.silent.samurai.speedy.interfaces.StructureReader;
import com.github.silent.samurai.speedy.interfaces.StructureReader.Kind;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
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
/// {@link StructureReader}. The read-side mirror of {@code WalkingResponseSerializer};
/// the only format-specific piece is the {@link SpeedyRequestReader}.
public class WalkingRequestParser implements IRequestBodyParser {

    private final String contentType;
    private final SpeedyRequestReader reader;
    private final StructureToSpeedy builder = new StructureToSpeedy();
    private final StructureToQuery queryBuilder = new StructureToQuery();

    public WalkingRequestParser(String contentType, SpeedyRequestReader reader) {
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
        try (StructureReader r = reader.readDocument(rawBody)) {
            if (r.begin() != Kind.ARRAY) {
                throw new BadRequestException("no content to process");
            }
            Kind elementKind;
            while ((elementKind = r.nextElement()) != null) {
                if (elementKind != Kind.OBJECT) {
                    throw new BadRequestException("in-valid content");
                }
                SpeedyEntity parsed = builder.fromEntity(entity, r);
                if (builder.isKeyComplete(entity, parsed)
                        && queryProcessor.exists(builder.toKey(entity, parsed))) {
                    throw new BadRequestException("Entity already present.");
                }
                entities.add(parsed);
            }
        }
        return SpeedyCreateBody.builder()
                .entities(entities)
                .mode(mode)
                .build();
    }

    @Override
    public SpeedyUpdateBody parseUpdate(byte[] rawBody, EntityMetadata entity,
                                        QueryProcessor queryProcessor) throws SpeedyHttpException {
        try (StructureReader r = reader.readDocument(rawBody)) {
            if (r.begin() != Kind.OBJECT) {
                throw new BadRequestException("no content to process");
            }
            SpeedyEntity parsed = builder.fromEntity(entity, r);
            SpeedyEntityKey pk = builder.toKey(entity, parsed);
            if (!builder.isKeyComplete(entity, parsed) || !queryProcessor.exists(pk)) {
                throw new BadRequestException("Entity not present.");
            }
            return SpeedyUpdateBody.builder()
                    .entity(parsed)
                    .pk(pk)
                    .build();
        }
    }

    @Override
    public SpeedyDeleteBody parseDelete(byte[] rawBody, EntityMetadata entity, TransactionMode mode,
                                        QueryProcessor queryProcessor) throws SpeedyHttpException {
        List<SpeedyEntityKey> keys = new LinkedList<>();
        try (StructureReader r = reader.readDocument(rawBody)) {
            if (r.begin() != Kind.ARRAY) {
                throw new BadRequestException("in-valid request");
            }
            Kind elementKind;
            while ((elementKind = r.nextElement()) != null) {
                if (elementKind != Kind.OBJECT) {
                    throw new BadRequestException("in-valid request body");
                }
                SpeedyEntity parsed = builder.fromEntity(entity, r);
                if (!builder.isKeyComplete(entity, parsed)) {
                    throw new BadRequestException("Primary Key Incomplete ");
                }
                keys.add(builder.toKey(entity, parsed));
            }
        }
        return SpeedyDeleteBody.builder()
                .keys(keys)
                .mode(mode)
                .build();
    }
}
