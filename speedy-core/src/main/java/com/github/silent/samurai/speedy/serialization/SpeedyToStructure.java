package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponseWriter;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.ExpansionPathTracker;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

/// Format-agnostic traversal of the Speedy entity tree.
///
/// Walks an entity (or collection of entities) and emits structural tokens to a
/// {@link SpeedyResponseWriter}; the format module decides how those tokens become bytes.
/// This is the shared counterpart to the former JSON-specific {@code SpeedyToJson} walker:
/// it owns the genuinely intricate logic (serializable + predicate filtering,
/// expand-vs-keys-only association rendering, collection/object fallbacks, and
/// {@link ExpansionPathTracker} depth/cycle control) so every format reuses it.
///
/// Leaf encoding is delegated to {@link SpeedyResponseWriter#writeLeaf}, which uses the
/// format's own type registry — the walker never sees format-specific value encoding.
public class SpeedyToStructure {

    private final Predicate<FieldMetadata> fieldPredicate;

    public SpeedyToStructure(Predicate<FieldMetadata> fieldPredicate) {
        this.fieldPredicate = fieldPredicate;
    }

    /// Writes a collection of entities as an array, using a fresh expansion path.
    public void writeCollection(Collection<? extends SpeedyValue> collection,
                                EntityMetadata entityMetadata,
                                Set<String> expands,
                                SpeedyResponseWriter w) throws SpeedyHttpException {
        writeCollection(collection, entityMetadata, new ExpansionPathTracker(expands), w);
    }

    private void writeCollection(Collection<? extends SpeedyValue> collection,
                                 EntityMetadata entityMetadata,
                                 ExpansionPathTracker pathTracker,
                                 SpeedyResponseWriter w) throws SpeedyHttpException {
        w.startArray();
        for (SpeedyValue object : collection) {
            if (object.isObject()) {
                writeEntity(object.asObject(), entityMetadata, pathTracker, w);
            } else {
                w.writeNull();
            }
        }
        w.endArray();
    }

    private void writeEntity(SpeedyEntity speedyEntity,
                             EntityMetadata entityMetadata,
                             ExpansionPathTracker pathTracker,
                             SpeedyResponseWriter w) throws SpeedyHttpException {
        w.startObject();
        pathTracker.pushEntity(entityMetadata);

        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isSerializable() || !fieldPredicate.test(fieldMetadata)) continue;
            if (!speedyEntity.has(fieldMetadata)) {
                w.field(fieldMetadata);
                w.writeNull();
                continue;
            }
            if (fieldMetadata.isAssociation()) {
                writeAssociation(speedyEntity, fieldMetadata, pathTracker, w);
            } else if (fieldMetadata.isCollection()) {
                SpeedyCollection speedyValue = (SpeedyCollection) speedyEntity.get(fieldMetadata);
                if (!speedyValue.isEmpty()) {
                    w.field(fieldMetadata);
                    writeBasicCollection(fieldMetadata, speedyValue.asCollection(), w);
                }
            } else {
                SpeedyValue value = speedyEntity.get(fieldMetadata);
                w.field(fieldMetadata);
                if (!value.isEmpty()) {
                    w.writeLeaf(fieldMetadata.getValueType(), value);
                } else {
                    w.writeNull();
                }
            }
        }

        pathTracker.popEntity();
        w.endObject();
    }

    private void writeAssociation(SpeedyEntity speedyEntity,
                                  FieldMetadata fieldMetadata,
                                  ExpansionPathTracker pathTracker,
                                  SpeedyResponseWriter w) throws SpeedyHttpException {
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();

        if (pathTracker.shouldExpand(associationMetadata)) {
            if (fieldMetadata.isCollection()) {
                Collection<SpeedyValue> value = speedyEntity.get(fieldMetadata).asCollection();
                if (value != null) {
                    w.field(fieldMetadata);
                    writeCollection(value, associationMetadata, pathTracker, w);
                }
            } else if (speedyEntity.has(fieldMetadata) && speedyEntity.get(fieldMetadata) != null) {
                SpeedyValue fieldValue = speedyEntity.get(fieldMetadata);
                w.field(fieldMetadata);
                if (fieldValue.isObject()) {
                    writeEntity(fieldValue.asObject(), associationMetadata, pathTracker, w);
                } else {
                    w.writeNull();
                }
            }
        } else {
            if (fieldMetadata.isCollection()) {
                Collection<SpeedyValue> value = speedyEntity.get(fieldMetadata).asCollection();
                if (!value.isEmpty()) {
                    w.field(fieldMetadata);
                    writeOnlyKeyCollection(value, associationMetadata, w);
                }
            } else {
                SpeedyValue fieldValue = speedyEntity.get(fieldMetadata);
                w.field(fieldMetadata);
                if (fieldValue.isObject()) {
                    writeOnlyKeys(fieldValue.asObject(), associationMetadata, w);
                } else {
                    w.writeNull();
                }
            }
        }
    }

    private void writeOnlyKeyCollection(Collection<SpeedyValue> collection,
                                        EntityMetadata entityMetadata,
                                        SpeedyResponseWriter w) throws SpeedyHttpException {
        w.startArray();
        for (SpeedyValue speedyValue : collection) {
            if (speedyValue.isObject()) {
                writeOnlyKeys(speedyValue.asObject(), entityMetadata, w);
            } else {
                w.writeNull();
            }
        }
        w.endArray();
    }

    private void writeOnlyKeys(SpeedyEntity speedyEntity,
                               EntityMetadata entityMetadata,
                               SpeedyResponseWriter w) throws SpeedyHttpException {
        w.startObject();
        for (KeyFieldMetadata fieldMetadata : entityMetadata.getKeyFields()) {
            if (!fieldMetadata.isSerializable()) continue;
            w.field(fieldMetadata);
            if (!speedyEntity.has(fieldMetadata) || speedyEntity.get(fieldMetadata).isEmpty()) {
                w.writeNull();
                continue;
            }
            w.writeLeaf(fieldMetadata.getValueType(), speedyEntity.get(fieldMetadata));
        }
        w.endObject();
    }

    private void writeBasicCollection(FieldMetadata fieldMetadata,
                                      Collection<SpeedyValue> collection,
                                      SpeedyResponseWriter w) throws SpeedyHttpException {
        w.startArray();
        for (SpeedyValue value : collection) {
            w.startObject();
            w.field(fieldMetadata);
            w.writeLeaf(fieldMetadata.getValueType(), value);
            w.endObject();
        }
        w.endArray();
    }
}
