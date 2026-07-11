package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class EntityBuilder {
    boolean hasCompositeKey;
    Set<ActionType> actionTypes = new HashSet<>(List.of(ActionType.ALL));
    Map<String, FieldBuilder> fieldMap = new HashMap<>();
    private String name;
    private String dbTableName;
    private boolean isSensitive = false;
    private TransactionMode transactionMode = TransactionMode.PER_ENTITY;
    private boolean bulkAllowed = false;
    private String softDeleteFieldName = null;
    private boolean viewDeletedAllowed = false;
    private boolean hardDeleteAllowed = false;

    /// Value types a soft-delete marker field may use: a boolean flag or a temporal timestamp.
    private static final Set<ValueType> SOFT_DELETE_TYPES =
            Set.of(ValueType.BOOL, ValueType.DATE, ValueType.DATE_TIME, ValueType.ZONED_DATE_TIME);

    public Iterable<FieldBuilder> fields() {
        return fieldMap.values();
    }

    public Iterable<KeyFieldBuilder> keyFields() {
        return fieldMap.values().stream()
                .filter(KeyFieldBuilder.class::isInstance)
                .map(KeyFieldBuilder.class::cast)
                .collect(Collectors.toList());
    }

    public EntityBuilder name(String name) {
        this.name = name;
        return this;
    }

    public EntityBuilder dbTableName(String dbTableName) {
        Objects.requireNonNull(dbTableName);
        this.dbTableName = dbTableName;
        return this;
    }

    public EntityBuilder hasCompositeKey(boolean hasCompositeKey) {
        this.hasCompositeKey = hasCompositeKey;
        return this;
    }

    public EntityBuilder addActionType(ActionType actionType) {
        // The default permission set is {ALL}. As soon as a specific action is
        // declared (e.g. @SpeedyAction(READ)), drop the implicit ALL so the
        // restriction actually takes effect — otherwise isCreateAllowed() and
        // friends always return true via the lingering ALL entry.
        if (actionType != ActionType.ALL) {
            this.actionTypes.remove(ActionType.ALL);
        }
        this.actionTypes.add(actionType);
        return this;
    }

    public EntityBuilder sensitive(boolean isSensitive) {
        this.isSensitive = isSensitive;
        return this;
    }

    public EntityBuilder transactionMode(TransactionMode transactionMode) {
        this.transactionMode = transactionMode;
        return this;
    }

    public EntityBuilder bulkAllowed(boolean bulkAllowed) {
        this.bulkAllowed = bulkAllowed;
        return this;
    }

    /// Marks {@code fieldName} as this entity's soft-delete marker (see {@code @SpeedySoftDelete}).
    /// The field is resolved and type-checked in {@link #build()}, once all fields are known.
    public EntityBuilder softDelete(String fieldName, boolean allowViewDeleted, boolean allowHardDelete) {
        this.softDeleteFieldName = fieldName;
        this.viewDeletedAllowed = allowViewDeleted;
        this.hardDeleteAllowed = allowHardDelete;
        return this;
    }

    public FieldBuilder ref(String name) throws NotFoundException {
        if (fieldMap.containsKey(name)) {
            return fieldMap.get(name);
        }
        throw new NotFoundException(String.format("Field '%s' not found in entity %s", name, this.name));
    }

    public FieldBuilder field(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be null or blank");
        }
        FieldBuilder fieldBuilder = new FieldBuilder(this, fieldName);
        this.fieldMap.put(fieldName, fieldBuilder);
        return fieldBuilder;
    }

    // Overload to support specifying output property name, DB column name and column type
    public FieldBuilder field(String outputPropertyName, String dbColumnName, ColumnType columnType) {
        Objects.requireNonNull(outputPropertyName);
        Objects.requireNonNull(dbColumnName);
        Objects.requireNonNull(columnType);
        FieldBuilder fieldBuilder = new FieldBuilder(this, outputPropertyName)
                .dbColumnName(dbColumnName)
                .columnType(columnType);
        this.fieldMap.put(outputPropertyName, fieldBuilder);
        return fieldBuilder;
    }

    public KeyFieldBuilder keyField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Key field name cannot be null or blank");
        }
        KeyFieldBuilder fieldBuilder = new KeyFieldBuilder(this, fieldName);
        this.fieldMap.put(fieldName, fieldBuilder);
        return fieldBuilder;
    }

    // Overload to support specifying output property name, DB column name and column type for key fields
    public KeyFieldBuilder keyField(String outputPropertyName, String dbColumnName, ColumnType columnType) {
        Objects.requireNonNull(outputPropertyName);
        Objects.requireNonNull(dbColumnName);
        Objects.requireNonNull(columnType);
        KeyFieldBuilder fieldBuilder = new KeyFieldBuilder(this, outputPropertyName);
        fieldBuilder.dbColumnName(dbColumnName)
                .columnType(columnType);
        this.fieldMap.put(outputPropertyName, fieldBuilder);
        return fieldBuilder;
    }


    public EntityMetadataImpl build() throws NotFoundException {

        Map<String, FieldMetadata> fieldMetadataMap = new HashMap<>();
        for (Map.Entry<String, FieldBuilder> e : fieldMap.entrySet()) {
            Map.Entry<String, FieldMetadataImpl> entry = Map.entry(e.getKey(), e.getValue().build());
            if (fieldMetadataMap.put(entry.getKey(), entry.getValue()) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }

        EntityMetadataImpl entityMetadata = new EntityMetadataImpl(name, dbTableName, hasCompositeKey, isSensitive, actionTypes, fieldMetadataMap);
        entityMetadata.setTransactionMode(transactionMode);
        entityMetadata.setBulkAllowed(bulkAllowed);

        if (softDeleteFieldName != null) {
            FieldMetadata softDeleteField = fieldMetadataMap.get(softDeleteFieldName);
            if (softDeleteField == null) {
                throw new NotFoundException(String.format(
                        "Soft-delete field '%s' not found in entity %s", softDeleteFieldName, name));
            }
            if (!SOFT_DELETE_TYPES.contains(softDeleteField.getValueType())) {
                throw new IllegalStateException(String.format(
                        "Soft-delete field '%s' on entity %s must be a boolean or temporal type, but is %s",
                        softDeleteFieldName, name, softDeleteField.getValueType()));
            }
            entityMetadata.setSoftDeleteField(softDeleteField);
            entityMetadata.setViewDeletedAllowed(viewDeletedAllowed);
            entityMetadata.setHardDeleteAllowed(hardDeleteAllowed);
        }

        for (FieldMetadata fieldMetadata : fieldMetadataMap.values()) {
            FieldMetadataImpl fieldMetadataImpl = (FieldMetadataImpl) fieldMetadata;
            fieldMetadataImpl.setEntityMetadata(entityMetadata);
        }

        return entityMetadata;
    }
}
