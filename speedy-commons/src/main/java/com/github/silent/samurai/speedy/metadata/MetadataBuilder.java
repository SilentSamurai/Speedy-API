package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MetadataBuilder {

    public static MetaModelBuilder builder() {
        return new MetaModelBuilder();
    }

    public static class MetaModelBuilder {

        private Map<String, EntityBuilder> entityMap = new HashMap<>();

        public EntityBuilder entity(String name) {
            EntityBuilder entityBuilder = new EntityBuilder().name(name);
            entityMap.put(name, entityBuilder);
            return entityBuilder;
        }

        public MetaModel build() {
            MetaModelImpl metaModelProcessor = new MetaModelImpl();

            entityMap.values().stream().map(EntityBuilder::build)
                    .forEach(metaModelProcessor::add);

            try {
                for (EntityBuilder eb : entityMap.values()) {
                    EntityMetadataImpl entityMetadata;
                    entityMetadata = (EntityMetadataImpl) metaModelProcessor.findEntityMetadata(eb.name);
                    for (FieldBuilder fb : eb.fieldMap.values()) {
                        if (fb.isAssociation) {
                            FieldMetadataImpl field = (FieldMetadataImpl) entityMetadata.field(fb.outputPropertyName);

                            EntityMetadata fkEntityMetadata = metaModelProcessor.findEntityMetadata(
                                    fb.associatedEntity
                            );
                            FieldMetadata fieldMetadata = metaModelProcessor.findFieldMetadata(
                                    fb.associatedEntity, fb.associatedField
                            );

                            field.setAssociatedFieldMetadata(fieldMetadata);
                            field.setAssociationMetadata(fkEntityMetadata);
                        }
                    }
                }
            } catch (NotFoundException e) {
                // this should never happen
                throw new RuntimeException(e);
            }


            for (EntityMetadata entityMetadata : metaModelProcessor.getAllEntityMetadata()) {

                for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
                    if (fieldMetadata.isAssociation()) {

                    }
                }
            }


            return metaModelProcessor;
        }

    }

    public static class EntityBuilder {
        private String name;
        private String dbTableName;
        private boolean hasCompositeKey;
        private Set<ActionType> actionTypes = new HashSet<>();
        private Map<String, FieldBuilder> fieldMap = new HashMap<>();

        public EntityBuilder name(String name) {
            this.name = name;
            return this;
        }

        public EntityBuilder dbTableName(String dbTableName) {
            this.dbTableName = dbTableName;
            return this;
        }

        public EntityBuilder hasCompositeKey(boolean hasCompositeKey) {
            this.hasCompositeKey = hasCompositeKey;
            return this;
        }

        public EntityBuilder addActionType(ActionType actionType) {
            this.actionTypes.add(actionType);
            return this;
        }

        public FieldBuilder ref(String name) throws NotFoundException {
            if (fieldMap.containsKey(name)) {
                return fieldMap.get(name);
            }
            throw new NotFoundException("field not found" + name);
        }

        public FieldBuilder field(String fieldName, String columnName, ColumnType columnType) {
            FieldBuilder fieldBuilder = new FieldBuilder(this, columnName, columnType, fieldName);
            this.fieldMap.put(fieldName, fieldBuilder);
            return fieldBuilder;
        }

        public KeyFieldBuilder keyField(String fieldName, String columnName, ColumnType columnType) {
            KeyFieldBuilder fieldBuilder = new KeyFieldBuilder(this, columnName, columnType, fieldName);
            this.fieldMap.put(fieldName, fieldBuilder);
            return fieldBuilder;
        }

        public EntityMetadataImpl build() {

            Map<String, FieldMetadata> fieldMetadataMap = fieldMap.entrySet()
                    .stream()
                    .map(e -> Map.entry(e.getKey(), e.getValue().build()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            EntityMetadataImpl entityMetadata = new EntityMetadataImpl(name, dbTableName, hasCompositeKey, actionTypes, fieldMetadataMap);

            for (FieldMetadata fieldMetadata : fieldMetadataMap.values()) {
                FieldMetadataImpl fieldMetadataImpl = (FieldMetadataImpl) fieldMetadata;
                fieldMetadataImpl.setEntityMetadata(entityMetadata);
            }

            return entityMetadata;
        }
    }

    public static class FieldBuilder {
        private final EntityBuilder entityBuilder;
        private final ColumnType columnType;
        private final String dbColumnName;
        private final String outputPropertyName;
        private boolean isCollection = false;
        private boolean isAssociation = false;
        private boolean isInsertable = true;
        private boolean isUpdatable = true;
        private boolean isUnique = false;
        private boolean isNullable = false;
        private boolean isRequired = false;
        private boolean isSerializable = true;
        private boolean isDeserializable = true;
        private String associatedField;
        private String associatedEntity;

        public FieldBuilder(EntityBuilder entityBuilder, String dbColumnName, ColumnType columnType, String outputPropertyName) {
            this.entityBuilder = entityBuilder;
            this.columnType = columnType;
            this.dbColumnName = dbColumnName;
            this.outputPropertyName = outputPropertyName;
        }

        public FieldBuilder collection(boolean isCollection) {
            this.isCollection = isCollection;
            return this;
        }

        public FieldBuilder insertable(boolean isInsertable) {
            this.isInsertable = isInsertable;
            return this;
        }

        public FieldBuilder updatable(boolean isUpdatable) {
            this.isUpdatable = isUpdatable;
            return this;
        }

        public FieldBuilder unique(boolean isUnique) {
            this.isUnique = isUnique;
            return this;
        }

        public FieldBuilder nullable(boolean isNullable) {
            this.isNullable = isNullable;
            return this;
        }

        public FieldBuilder required(boolean isRequired) {
            this.isRequired = isRequired;
            return this;
        }

        public FieldBuilder serializable(boolean isSerializable) {
            this.isSerializable = isSerializable;
            return this;
        }

        public FieldBuilder deserializable(boolean isDeserializable) {
            this.isDeserializable = isDeserializable;
            return this;
        }

        public FieldBuilder associateWith(FieldBuilder associatedField) {
            this.associatedField = associatedField.outputPropertyName;
            this.associatedEntity = associatedField.entityBuilder.name;
            this.isAssociation = true;
            return this;
        }

        public FieldBuilder associateWith(String entity, String field) {
            this.associatedEntity = entity;
            this.associatedField = field;
            this.isAssociation = true;
            return this;
        }

        public FieldMetadataImpl build() {
            return new FieldMetadataImpl(
                    columnType, dbColumnName, outputPropertyName,
                    isCollection, isAssociation, isInsertable, isUpdatable, isUnique,
                    isNullable, isRequired, isSerializable, isDeserializable
            );
        }
    }

    public static class KeyFieldBuilder extends FieldBuilder {
        private boolean shouldGenerateKey;

        public KeyFieldBuilder(EntityBuilder entityBuilder, String dbColumnName, ColumnType columnType, String outputPropertyName) {
            super(entityBuilder, dbColumnName, columnType, outputPropertyName);
        }

        public KeyFieldBuilder shouldGenerateKey(boolean shouldGenerateKey) {
            this.shouldGenerateKey = shouldGenerateKey;
            return this;
        }

        @Override
        public KeyFieldMetadataImpl build() {
            return new KeyFieldMetadataImpl(
                    super.columnType, super.dbColumnName, super.outputPropertyName,
                    super.isCollection, super.isAssociation, super.isInsertable, super.isUpdatable,
                    super.isUnique, super.isNullable, super.isRequired, super.isSerializable,
                    super.isDeserializable, shouldGenerateKey
            );
        }
    }


}
