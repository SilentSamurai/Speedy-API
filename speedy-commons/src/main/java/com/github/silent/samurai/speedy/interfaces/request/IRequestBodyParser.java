package com.github.silent.samurai.speedy.interfaces.request;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.response.IResponseSerializerV2;
import com.github.silent.samurai.speedy.models.SpeedyCreateBody;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;

/// Parses raw HTTP request body bytes into typed SpeedyBody instances.
///
/// Format-agnostic interface that mirrors IResponseSerializerV2 on the
/// request side. Implementations handle specific content types (JSON,
/// YAML, XML). All format-specific logic (e.g., Jackson ObjectMapper) is
/// confined to implementations, keeping the core architecture decoupled
/// from any particular serialization format.
///
/// @see IResponseSerializerV2
public interface IRequestBodyParser {

    /// The MIME content type this parser handles (e.g. "application/json").
    String getContentType();

    /// Parses a $query JSON body into a SpeedyQuery with filters, pagination,
    /// sorting, expands, and field selections.
    SpeedyQuery parseQuery(byte[] rawBody, MetaModel metaModel, SpeedyQuery baseQuery,
                           int maxPageSize, int defaultPageSize) throws SpeedyHttpException;

    /// Parses a $create JSON array body into a list of SpeedyEntity instances
    /// wrapped in a SpeedyCreateBody.
    SpeedyCreateBody parseCreate(byte[] rawBody, EntityMetadata entity,
                                 QueryProcessor queryProcessor) throws SpeedyHttpException;

    /// Parses a $update JSON body (a bare object, or an array for bulk update) into a list
    /// of (entity, primary key) pairs, wrapped in a SpeedyUpdateBody.
    SpeedyUpdateBody parseUpdate(byte[] rawBody, EntityMetadata entity,
                                 QueryProcessor queryProcessor) throws SpeedyHttpException;

    /// Parses a $delete JSON array body into a list of SpeedyEntityKey instances,
    /// wrapped in a SpeedyDeleteBody.
    SpeedyDeleteBody parseDelete(byte[] rawBody, EntityMetadata entity,
                                 QueryProcessor queryProcessor) throws SpeedyHttpException;
}
