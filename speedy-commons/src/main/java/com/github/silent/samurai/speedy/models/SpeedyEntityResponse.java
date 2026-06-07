package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.SpeedyResponseType;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Predicate;

/// Response containing a list of entities with pagination metadata.
///
/// Produced by GET and $query operations when a non-count query is
/// executed. Supports field-level filtering via fieldPredicate and
/// eager-fetching of associations via expands.
@Getter
@Builder
public class SpeedyEntityResponse implements SpeedyResponse {

    /// The list of entity instances returned by the query.
    private final List<? extends SpeedyValue> payload;

    /// Predicate controlling which fields are serialized in the output.
    /// Defaults to including all fields.
    @Builder.Default
    private final Predicate<FieldMetadata> fieldPredicate = fieldMetadata -> true;

    /// Zero-based page index of the current result page.
    private final Integer pageIndex;

    /// Set of association names to expand (eager-fetch) in the response.
    @Builder.Default
    private final Set<String> expands = Collections.emptySet();

    /// Total number of entities matching the query before pagination.
    /// Only populated when the query includes a total-count calculation.
    private final BigInteger totalCount;

    /// Number of entities requested per page.
    private final int requestedPageSize;

    /// HTTP status code for the response.
    @Builder.Default
    private final int status = 200;

    /// HTTP response headers as name-value pairs.
    @Builder.Default
    private final Map<String, String> headers = new LinkedHashMap<>();

    @Override
    public SpeedyResponseType getType() {
        return SpeedyResponseType.ENTITY_LIST;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }
}
