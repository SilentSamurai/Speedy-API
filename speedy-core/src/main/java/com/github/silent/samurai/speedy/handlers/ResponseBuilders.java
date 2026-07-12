package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.serialization.FieldPredicates;

import java.math.BigInteger;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/// Shared response builders for read/write handlers.
///
/// Eliminates the duplicated {@link SpeedyEntityResponse}/{@link SpeedyCountResponse}
/// construction that was copy-pasted across {@link GetHandler}, {@link QueryHandler},
/// {@link CreateHandler}, and {@link DeleteHandler}.
public final class ResponseBuilders {

    private ResponseBuilders() {
    }

    /// Key-only field predicate used by create/delete success responses.
    public static final BiPredicate<SpeedyEntity, FieldMetadata> KEY_ONLY =
            (entity, fieldMetadata) -> fieldMetadata instanceof KeyFieldMetadata;

    /// Builds a count-only response (for {@code $select=$count} requests).
    public static SpeedyCountResponse countResponse(BigInteger count) {
        return SpeedyCountResponse.builder()
                .count(count)
                .status(200)
                .build();
    }

    /// Builds a paginated entity-list response with total count (for GET / $query).
    public static SpeedyEntityResponse entityListResponse(EntityMetadata entityMetadata,
                                                          List<SpeedyEntity> entities,
                                                          SpeedyQuery speedyQuery,
                                                          QueryResult result) {
        Predicate<FieldMetadata> selectPredicate = FieldPredicates.buildFieldPredicate(speedyQuery.getSelect());
        BiPredicate<SpeedyEntity, FieldMetadata> fieldPredicate = (entity, fieldMetadata) -> selectPredicate.test(fieldMetadata);
        return SpeedyEntityResponse.builder()
                .entityMetadata(entityMetadata)
                .payload(entities)
                .pageIndex(speedyQuery.getPageInfo().getPageNo())
                .expands(speedyQuery.getExpand())
                .totalCount(result.totalCount())
                .requestedPageSize(speedyQuery.getPageInfo().getPageSize())
                .fieldPredicate(fieldPredicate)
                .status(200)
                .build();
    }

    /// Builds a key-only success response (for create/delete batch with all keys).
    public static SpeedyEntityResponse keyOnlyResponse(EntityMetadata entityMetadata,
                                                       List<SpeedyEntity> payload) {
        return SpeedyEntityResponse.builder()
                .entityMetadata(entityMetadata)
                .payload(payload)
                .pageIndex(0)
                .fieldPredicate(KEY_ONLY)
                .status(200)
                .build();
    }

    /// Builds an empty success response (for create/delete with empty input).
    public static SpeedyEntityResponse emptyKeyOnlyResponse(EntityMetadata entityMetadata) {
        return keyOnlyResponse(entityMetadata, List.of());
    }

    /// Builds a full-entity success response (for single or bulk update/replace) — unlike
    /// {@link #keyOnlyResponse}, no field predicate is applied, so every serializable field of
    /// the saved rows is emitted, preserving the single-update "returns the resource" contract.
    public static SpeedyEntityResponse updatedEntitiesResponse(EntityMetadata entityMetadata,
                                                                List<SpeedyEntity> saved) {
        return SpeedyEntityResponse.builder()
                .entityMetadata(entityMetadata)
                .payload(saved)
                .pageIndex(0)
                .status(200)
                .build();
    }
}
