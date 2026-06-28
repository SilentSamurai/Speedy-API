package com.github.silent.samurai.speedy.interfaces.response;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import com.github.silent.samurai.speedy.models.*;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/// Format-specific streaming sink for response serialization.
///
/// The shared {@code ResponseWalker} and {@code WalkingResponseSerializer} (both
/// format-agnostic) drive a sequence of structural tokens against this interface;
/// a format module (JSON, XML, YAML, …) implements the sink to render those tokens
/// into bytes — only the format-specific rendering lives in the format module.
///
/// ## Buffering contract
/// Implementations are expected to **buffer** the document and commit nothing to the
/// {@link HttpServletResponse} until {@link #finish}. This preserves transactional
/// error handling: a {@link SpeedyHttpException} thrown mid-walk propagates before any
/// HTTP status or body has been written, so the framework can still emit a clean error
/// response. (A format may opt into true streaming later where that trade-off is acceptable.)
///
/// ## Token protocol
/// Objects are written as {@link #startObject} / (repeated {@link #field} + value) / {@link #endObject}.
/// Arrays are {@link #startArray} / (repeated values) / {@link #endArray}. A value is either a nested
/// object/array, a {@link #writeNull}, or a {@link #writeLeaf} domain value. Each {@link #field} must be
/// followed by exactly one value.
public interface SpeedyResponseWriter {

    void startObject() throws SpeedyHttpException;

    void endObject() throws SpeedyHttpException;

    /// Names the next value within the enclosing object — for framework/envelope keys
    /// that have no entity metadata (e.g. "payload", "status").
    void field(String name) throws SpeedyHttpException;

    /// Names the next value within the enclosing object using its field metadata.
    /// Self-describing formats emit the output property name (the default); schema-driven
    /// formats (e.g. protobuf) override to emit the field's number. This is the write-side
    /// counterpart to {@link StructureReader#nextField(EntityMetadata)}.
    default void field(FieldMetadata field) throws SpeedyHttpException {
        field(field.getOutputPropertyName());
    }

    void startArray() throws SpeedyHttpException;

    void endArray() throws SpeedyHttpException;

    void writeNull() throws SpeedyHttpException;

    void writeSpeedyInt(SpeedyInt value) throws SpeedyHttpException;

    void writeSpeedyText(SpeedyText value) throws SpeedyHttpException;

    void writeSpeedyDouble(SpeedyDouble value) throws SpeedyHttpException;

    void writeSpeedyBoolean(SpeedyBoolean value) throws SpeedyHttpException;

    void writeSpeedyDate(SpeedyDate value) throws SpeedyHttpException;

    void writeSpeedyDateTime(SpeedyDateTime value) throws SpeedyHttpException;

    void writeSpeedyTime(SpeedyTime value) throws SpeedyHttpException;

    void writeSpeedyZonedDateTime(SpeedyZonedDateTime value) throws SpeedyHttpException;

    void writeSpeedyEnum(SpeedyEnum value) throws SpeedyHttpException;

    /// Writes a raw {@code long} for framework/envelope fields, bypassing the
    /// {@link SpeedyValue} wrapper. The default boxes into a {@link SpeedyInt}; formats
    /// that can render primitives directly override this to avoid the allocation.
    default void writeInt(long value) throws SpeedyHttpException {
        writeSpeedyInt(new SpeedyInt(value));
    }

    /// Writes a raw {@link String} for framework/envelope fields. Matches
    /// {@link #writeLeaf} semantics for text: a null or empty value is written as a JSON
    /// null. The default boxes into a {@link SpeedyText}; formats override to skip it.
    default void writeText(String value) throws SpeedyHttpException {
        if (value == null || value.isEmpty()) {
            writeNull();
        } else {
            writeSpeedyText(new SpeedyText(value));
        }
    }

    /// Writes a raw {@code boolean} for framework/envelope fields. The default boxes into
    /// a {@link SpeedyBoolean}; formats override to skip it.
    default void writeBool(boolean value) throws SpeedyHttpException {
        writeSpeedyBoolean(new SpeedyBoolean(value));
    }

    /// Dispatches a domain value to the typed {@code writeSpeedy*} method.
    default void writeLeaf(ValueType type, SpeedyValue value) throws SpeedyHttpException {
        if (value == null || value.isEmpty() || value.isNull()) {
            writeNull();
            return;
        }
        switch (type) {
            case BOOL -> writeSpeedyBoolean((SpeedyBoolean) value);
            case INT -> writeSpeedyInt((SpeedyInt) value);
            case FLOAT -> writeSpeedyDouble((SpeedyDouble) value);
            case TEXT -> writeSpeedyText((SpeedyText) value);
            case DATE -> writeSpeedyDate((SpeedyDate) value);
            case TIME -> writeSpeedyTime((SpeedyTime) value);
            case DATE_TIME -> writeSpeedyDateTime((SpeedyDateTime) value);
            case ZONED_DATE_TIME -> writeSpeedyZonedDateTime((SpeedyZonedDateTime) value);
            case ENUM, ENUM_ORD -> writeSpeedyEnum((SpeedyEnum) value);
            default -> throw new IllegalArgumentException("Unexpected leaf type: " + type);
        }
    }

    /// Discards any buffered, uncommitted document so a fresh document can be written.
    /// Used by the error path when a write fails mid-document and the same writer is
    /// reused: the partial output must be cleared before the error document is rendered.
    /// The default is a no-op (non-buffering formats have nothing to discard).
    default void reset() throws SpeedyHttpException {
    }

    /// Commits the buffered document to the HTTP response: status code, headers,
    /// content type, then the rendered body.
    void finish(HttpServletResponse out, int status, Map<String, String> headers, String contentType)
            throws SpeedyHttpException;
}
