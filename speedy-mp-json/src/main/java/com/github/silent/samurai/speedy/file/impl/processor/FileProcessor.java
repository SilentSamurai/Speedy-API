package com.github.silent.samurai.speedy.file.impl.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.silent.samurai.speedy.enums.BulkOperation;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.file.impl.models.JsonAssociationColumn;
import com.github.silent.samurai.speedy.file.impl.models.JsonEntity;
import com.github.silent.samurai.speedy.file.impl.models.JsonField;
import com.github.silent.samurai.speedy.metadata.AssociationColumnRef;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.FieldBuilder;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class FileProcessor {

    public static void process(InputStream in, MetaModelBuilder mmb) throws NotFoundException, IOException {
        List<JsonEntity> entityMetadata = CommonUtil.json().readValue(in, new TypeReference<List<JsonEntity>>() {
        });

        for (JsonEntity jsonEntity : entityMetadata) {
            processEntityMetadata(jsonEntity, mmb);
        }
    }

    public static void processEntityMetadata(JsonEntity jsonEntity, MetaModelBuilder mmb) throws IOException {

        EntityBuilder eb = mmb.entity(jsonEntity.name);
        eb.hasCompositeKey(jsonEntity.hasCompositeKey);
        eb.dbTableName(jsonEntity.dbTable);
        eb.sensitive(jsonEntity.sensitive);
        if (jsonEntity.bulk != null) {
            for (String op : jsonEntity.bulk) {
                eb.addBulkOperation(BulkOperation.valueOf(op.trim().toUpperCase()));
            }
        } else if (Boolean.TRUE.equals(jsonEntity.bulkAllowed)) {
            eb.addBulkOperation(BulkOperation.ALL);
        }

        for (JsonField jsonField : jsonEntity.fields) {
            processFieldMetadata(jsonField, eb);
        }
    }

    private static void processFieldMetadata(JsonField jsonField, EntityBuilder eb) throws IOException {
        ColumnType columnType = jsonField.isAssociation ? ColumnType.VARCHAR : ColumnType.valueOf(jsonField.fieldType);
        FieldBuilder fb = jsonField.isKeyField ?
                eb.keyField(jsonField.outputProperty, jsonField.dbColumn, columnType) :
                eb.field(jsonField.outputProperty, jsonField.dbColumn, columnType);
        fb.collection(jsonField.isCollection);
        fb.nullable(jsonField.isNullable);
        fb.unique(jsonField.isUnique);
        fb.updatable(jsonField.isUpdatable);
        fb.insertable(jsonField.isInsertable);
        fb.required(jsonField.isRequired);
        fb.serializable(jsonField.isSerializable);
        fb.deserializable(jsonField.isDeserializable);
        fb.sensitive(jsonField.sensitive != null ? jsonField.sensitive : eb.isSensitive());
        fb.maxLength(jsonField.maxLength);
        fb.precision(jsonField.precision);
        fb.scale(jsonField.scale);

        if (jsonField.isAssociation) {
            fb.associateWith(jsonField.fieldType, associationColumns(jsonField));
        }
    }

    /// The foreign-key columns an association field is mapped through. The singular
    /// {@code associatedColumn} declares the common single-column case (a null local column — the
    /// field's own {@code dbColumn} carries the key, resolved at build time); the plural
    /// {@code associatedColumns} declares one (local column, target key field) pair per column of a
    /// composite target key. Declaring both, or an empty/incomplete plural list, is rejected here
    /// rather than surfacing as a half-mapped association at runtime.
    private static List<AssociationColumnRef> associationColumns(JsonField jsonField) throws IOException {
        if (jsonField.associatedColumn != null && jsonField.associatedColumns != null) {
            throw new IOException(String.format(
                    "association field '%s' declares both 'associatedColumn' and 'associatedColumns' — use one",
                    jsonField.outputProperty));
        }
        if (jsonField.associatedColumns == null) {
            if (jsonField.associatedColumn == null || jsonField.associatedColumn.isBlank()) {
                throw new IOException(String.format(
                        "association field '%s' declares neither 'associatedColumn' nor 'associatedColumns'",
                        jsonField.outputProperty));
            }
            return List.of(new AssociationColumnRef(null, jsonField.associatedColumn));
        }
        if (jsonField.associatedColumns.isEmpty()) {
            throw new IOException(String.format(
                    "association field '%s' declares an empty 'associatedColumns' — one entry per column of the target's primary key is required",
                    jsonField.outputProperty));
        }
        List<AssociationColumnRef> columns = new ArrayList<>(jsonField.associatedColumns.size());
        for (JsonAssociationColumn column : jsonField.associatedColumns) {
            if (column.dbColumn == null || column.dbColumn.isBlank()
                    || column.associatedColumn == null || column.associatedColumn.isBlank()) {
                throw new IOException(String.format(
                        "association field '%s' has an 'associatedColumns' entry missing 'dbColumn' or 'associatedColumn'",
                        jsonField.outputProperty));
            }
            columns.add(new AssociationColumnRef(column.dbColumn, column.associatedColumn));
        }
        return columns;
    }


}
