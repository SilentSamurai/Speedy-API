package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;

/// Passive, pull-based token source for request-body parsing — the read-counterpart
/// of {@link SpeedyResponseWriter}.
///
/// The shared {@code StructureToSpeedy} and {@code WalkingRequestParser} (both
/// format-agnostic) **drive the loop** and **pull** tokens from this interface; a
/// format module (JSON, XML, YAML, …) implements it over its own streaming parser
/// (e.g. a Jackson {@code JsonParser}) so there is no document tree and no per-node
/// wrapper allocation. This mirrors the write side, where the core walker pushes
/// tokens at the passive {@link SpeedyResponseWriter} sink — the core drives in both
/// directions and the format module is always the dumb token port.
///
/// ## Token protocol
/// {@link #begin()} positions on the document root and returns its {@link Kind}.
/// Within an object, repeated {@link #nextField(EntityMetadata)} yields each known field and advances
/// onto its value token; within an array, repeated {@link #nextElement()} advances onto
/// each element. Once positioned on a value token the caller makes **exactly one** value
/// call — {@link #readField}, a nested object/array traversal, or {@link #skipValue} — the
/// read mirror of the writer's "each field is followed by exactly one value". Leaf
/// decoding lives here (mirroring {@link SpeedyResponseWriter#writeLeaf}); there is
/// deliberately no separate value-decoder.
public interface StructureReader extends AutoCloseable {

    /// Structural kind of value token.
    enum Kind {OBJECT, ARRAY, VALUE, NULL}

    /// Advances to the document root token and returns its kind, or {@code null}
    /// if the body is empty.
    Kind begin() throws SpeedyHttpException;

    /// Kind of the current value token (root, field value, or array element).
    Kind currentKind() throws SpeedyHttpException;

    /// Within the current object, advances to the next field that maps to a known field of
    /// {@code entityMetadata}, resolving it (by name, number, …) and positioning on its
    /// value token; fields the metadata doesn't know are skipped internally. Returns
    /// {@code null} at the end of the object.
    ///
    /// Passing the entity metadata is what makes the port format-agnostic about identity:
    /// a self-describing format resolves the wire name to a {@link FieldMetadata}, while a
    /// schema-driven format (e.g. protobuf) resolves a wire field-number instead.
    FieldMetadata nextField(EntityMetadata entityMetadata) throws SpeedyHttpException;

    /// Within the current array, advances to the next element and returns its kind,
    /// or {@code null} at the end of the array.
    Kind nextElement() throws SpeedyHttpException;

    /// Decodes the current scalar value token into a {@link SpeedyValue} of the field's
    /// type ({@code field.getValueType()}) — the read mirror of
    /// {@link SpeedyResponseWriter#writeLeaf}. A null token yields a null {@link SpeedyValue}.
    SpeedyValue readField(FieldMetadata field) throws SpeedyHttpException;

    /// Skips the current value and any subtree.
    void skipValue() throws SpeedyHttpException;

    @Override
    void close();
}
