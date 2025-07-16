package com.github.silent.samurai.speedy.file.impl.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.file.impl.metadata.FileFieldMetadata;
import com.github.silent.samurai.speedy.file.impl.models.JsonEntity;
import com.github.silent.samurai.speedy.file.impl.models.JsonField;
import com.github.silent.samurai.speedy.file.impl.validator.JsonValidator;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder.FieldBuilder;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder.MetaModelBuilder;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileProcessor {

    public static void process(InputStream in, MetaModelBuilder mmb) throws NotFoundException, IOException {
        List<JsonEntity> entityMetadata = CommonUtil.json().readValue(in, new TypeReference<List<JsonEntity>>() {
        });

        for (JsonEntity jsonEntity : entityMetadata) {
            JsonValidator.validate(jsonEntity);
            processEntityMetadata(jsonEntity, mmb);
        }
    }

    public static void processEntityMetadata(JsonEntity jsonEntity, MetaModelBuilder mmb) throws IOException {

        EntityBuilder eb = mmb.entity(jsonEntity.name);
        eb.hasCompositeKey(jsonEntity.hasCompositeKey);
        eb.dbTableName(jsonEntity.dbTable);
//        eb.setKeyType(jsonEntity.keyType);

        Map<String, FileFieldMetadata> fieldMap = new HashMap<>();

        for (JsonField jsonField : jsonEntity.fields) {
            JsonValidator.validate(jsonField);
            processFieldMetadata(jsonField, eb);
        }
    }

    public static FieldBuilder processFieldMetadata(JsonField jsonField, EntityBuilder eb) throws IOException {
        FieldBuilder fb;
        if (jsonField.isKeyField) {
            fb = eb.keyField(jsonField.outputProperty, jsonField.dbColumn, ColumnType.valueOrDefault(jsonField.fieldType, ColumnType.VARCHAR));
        } else {
            fb = eb.field(jsonField.outputProperty, jsonField.dbColumn, ColumnType.valueOrDefault(jsonField.fieldType, ColumnType.VARCHAR));
        }
        fb.collection(jsonField.isCollection);
        fb.nullable(jsonField.isNullable);
        fb.unique(jsonField.isUnique);
        fb.updatable(jsonField.isUpdatable);
        fb.insertable(jsonField.isInsertable);
        fb.required(jsonField.isRequired);
        fb.serializable(jsonField.isSerializable);
        fb.deserializable(jsonField.isDeserializable);

        if (jsonField.isAssociation) {
            fb.associateWith(jsonField.fieldType, jsonField.associatedColumn);
        }

        return fb;
    }


}
