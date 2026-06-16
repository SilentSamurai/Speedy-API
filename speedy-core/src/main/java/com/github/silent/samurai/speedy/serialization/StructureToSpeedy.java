package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.StructureReader;
import com.github.silent.samurai.speedy.interfaces.StructureReader.Kind;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import java.util.Collection;
import java.util.LinkedList;

/// Format-agnostic builder: pulls tokens from a {@link StructureReader} and assembles a
/// {@link SpeedyEntity} tree in document order, driven by {@link EntityMetadata}. The
/// read-side mirror of {@link ResponseWalker} — structure only; leaf decoding lives on
/// the reader ({@link StructureReader#readField}, the inverse of {@code writeLeaf}).
///
/// Stateless and thread-safe; the same instance can serve every request.
public class StructureToSpeedy {

    /// Reads the current object as a {@link SpeedyEntity}. Fields are matched to metadata
    /// by name; unknown, non-deserializable, or explicitly-null fields are skipped
    /// (mirroring the previous {@code isDeserializable() && has && !isNull} gate).
    public SpeedyEntity fromEntity(EntityMetadata entityMetadata, StructureReader r) throws SpeedyHttpException {
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        FieldMetadata fieldMetadata;
        while ((fieldMetadata = r.nextField(entityMetadata)) != null) {
            if (!fieldMetadata.isDeserializable() || r.currentKind() == Kind.NULL) {
                r.skipValue();
                continue;
            }
            entity.put(fieldMetadata, fromField(fieldMetadata, r));
        }
        return entity;
    }

    private SpeedyValue fromField(FieldMetadata fieldMetadata, StructureReader r) throws SpeedyHttpException {
        String name = fieldMetadata.getOutputPropertyName();
        if (fieldMetadata.isAssociation()) {
            EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
            if (fieldMetadata.isCollection()) {
                requireArray(r, name);
                Collection<SpeedyValue> collection = new LinkedList<>();
                Kind k;
                while ((k = r.nextElement()) != null) {
                    if (k != Kind.OBJECT) {
                        throw new BadRequestException("Field " + name + " must be an object");
                    }
                    collection.add(fromEntity(associationMetadata, r));
                }
                return new SpeedyCollection(collection);
            }
            if (r.currentKind() != Kind.OBJECT) {
                throw new BadRequestException("Field " + name + " must be an object");
            }
            return fromEntity(associationMetadata, r);
        }
        if (fieldMetadata.isCollection()) {
            requireArray(r, name);
            Collection<SpeedyValue> collection = new LinkedList<>();
            Kind k;
            while ((k = r.nextElement()) != null) {
                if (k == Kind.OBJECT || k == Kind.ARRAY) {
                    throw new BadRequestException("Field " + name + " must be a value");
                }
                collection.add(r.readField(fieldMetadata));
            }
            return new SpeedyCollection(collection);
        }
        Kind kind = r.currentKind();
        if (kind == Kind.OBJECT || kind == Kind.ARRAY) {
            throw new BadRequestException("Field " + name + " must be a value");
        }
        return r.readField(fieldMetadata);
    }

    private void requireArray(StructureReader r, String fieldName) throws SpeedyHttpException {
        if (r.currentKind() != Kind.ARRAY) {
            throw new BadRequestException("Field " + fieldName + " must be an array");
        }
    }

    /// Derives the primary key from an already-parsed entity (streaming parses each
    /// object once, so the key is read off the built entity rather than re-read).
    public SpeedyEntityKey toKey(EntityMetadata entityMetadata, SpeedyEntity entity) {
        SpeedyEntityKey key = new SpeedyEntityKey(entityMetadata);
        for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
            if (entity.has(keyField)) {
                key.put(keyField, entity.get(keyField));
            }
        }
        return key;
    }

    /// Whether every key field of the entity is present and non-null.
    public boolean isKeyComplete(EntityMetadata entityMetadata, SpeedyEntity entity) {
        for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
            if (!entity.has(keyField) || entity.get(keyField).isNull()) {
                return false;
            }
        }
        return true;
    }
}
