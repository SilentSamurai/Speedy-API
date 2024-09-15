package com.github.silent.samurai.speedy.file.impl.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.file.impl.metadata.FileEntityMetadata;
import com.github.silent.samurai.speedy.file.impl.metadata.FileFieldMetadata;
import com.github.silent.samurai.speedy.file.impl.metadata.FileKeyFieldMetadata;
import com.github.silent.samurai.speedy.file.impl.models.JsonEntity;
import com.github.silent.samurai.speedy.file.impl.models.JsonField;
import com.github.silent.samurai.speedy.file.impl.validator.JsonValidator;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileProcessor {

    public static void process(InputStream in, Map<String, FileEntityMetadata> entityMap) throws NotFoundException, IOException {
        List<JsonEntity> entityMetadata = CommonUtil.json().readValue(in, new TypeReference<List<JsonEntity>>() {
        });

        for (JsonEntity jsonEntity : entityMetadata) {
            JsonValidator.validate(jsonEntity);
            String name = jsonEntity.name;
            FileEntityMetadata fileEntityMetadata = processEntityMetadata(jsonEntity);
            entityMap.put(name, fileEntityMetadata);
        }
        processAssociation(entityMap);

    }

    public static FileEntityMetadata processEntityMetadata(JsonEntity jsonEntity) throws IOException {
        FileEntityMetadata entityMetadata = new FileEntityMetadata();
        entityMetadata.setName(jsonEntity.name);
        entityMetadata.setHasCompositeKey(jsonEntity.hasCompositeKey);
        entityMetadata.setDbTableName(jsonEntity.dbTable);
        entityMetadata.setKeyType(jsonEntity.keyType);

        Map<String, FileFieldMetadata> fieldMap = new HashMap<>();

        for (JsonField jsonField : jsonEntity.fields) {
            JsonValidator.validate(jsonField);

            FileFieldMetadata fileFieldMetadata = processFieldMetadata(jsonField);
            fieldMap.put(jsonField.name, fileFieldMetadata);
        }
        entityMetadata.setFieldMap(fieldMap);
        return entityMetadata;
    }

    public static void processAssociation(Map<String, FileEntityMetadata> entityMap) throws NotFoundException {
        for (Map.Entry<String, FileEntityMetadata> entry : entityMap.entrySet()) {
            FileEntityMetadata entityMetadata = entry.getValue();
            for (FileFieldMetadata fieldMetadata : entityMetadata.getFields()) {
                fieldMetadata.setEntityMetadata(entityMetadata);
                if (fieldMetadata.isAssociation()) {
                    FileEntityMetadata associatedMetadata = entityMap.getOrDefault(fieldMetadata.getType(), null);
                    if (associatedMetadata == null) {
                        throw new NotFoundException("Associated Entity not found " + fieldMetadata.getType());
                    }
                    fieldMetadata.setAssociationMetadata(associatedMetadata);
                    fieldMetadata.setValueType(ValueType.OBJECT);
                    FileFieldMetadata fileFieldMetadata = (FileFieldMetadata) associatedMetadata
                            .field(fieldMetadata.getAssociatedColumn());

                    fieldMetadata.setAssociatedFieldMetadata(fileFieldMetadata);
                }
            }
        }
    }

    public static FileFieldMetadata processFieldMetadata(JsonField jsonField) throws IOException {
        FileFieldMetadata fieldMetadata;
        if (jsonField.isKeyField) {
            fieldMetadata = new FileKeyFieldMetadata();
        } else {
            fieldMetadata = new FileFieldMetadata();
        }
        fieldMetadata.setName(jsonField.name);
        fieldMetadata.setCollection(jsonField.isCollection);
        fieldMetadata.setNullable(jsonField.isNullable);
        fieldMetadata.setUnique(jsonField.isUnique);
        fieldMetadata.setUpdatable(jsonField.isUpdatable);
        fieldMetadata.setInsertable(jsonField.isInsertable);
        fieldMetadata.setRequired(jsonField.isRequired);
        fieldMetadata.setAssociation(jsonField.isAssociation);
        fieldMetadata.setSerializable(jsonField.isSerializable);
        fieldMetadata.setDeserializable(jsonField.isDeserializable);
        fieldMetadata.setOutputPropertyName(jsonField.outputProperty);
        fieldMetadata.setDbColumnName(jsonField.dbColumn);
        fieldMetadata.setType(jsonField.fieldType);
        fieldMetadata.setAssociatedColumn(jsonField.associatedColumn);

        if (!jsonField.isAssociation) {
            fieldMetadata.setValueType(ValueType.valueOf(jsonField.fieldType));
        }

        return fieldMetadata;
    }


}
