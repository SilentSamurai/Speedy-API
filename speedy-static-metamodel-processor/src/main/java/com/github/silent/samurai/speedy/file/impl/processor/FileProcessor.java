package com.github.silent.samurai.speedy.file.impl.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.file.impl.models.JsonEntity;
import com.github.silent.samurai.speedy.file.impl.models.JsonField;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.FieldBuilder;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.io.IOException;
import java.io.InputStream;
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
        if (jsonEntity.transactionMode != null && !jsonEntity.transactionMode.isEmpty()) {
            TransactionMode mode = TransactionMode.valueOf(jsonEntity.transactionMode.toUpperCase());
            eb.transactionMode(mode);
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

        if (jsonField.isAssociation) {
            fb.associateWith(jsonField.fieldType, jsonField.associatedColumn);
        }
    }


}
