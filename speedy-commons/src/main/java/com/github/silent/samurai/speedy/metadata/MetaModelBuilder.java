package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.AssociationColumn;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaModelBuilder {

    Map<String, EntityBuilder> entityMap = new HashMap<>();

    public EntityBuilder entity(String name) {
        EntityBuilder entityBuilder = new EntityBuilder().name(name);
        entityMap.put(name, entityBuilder);
        return entityBuilder;
    }

    public Iterable<EntityBuilder> entities() {
        return entityMap.values();
    }

    public boolean hasEntity(String name) {
        return entityMap.containsKey(name);
    }

    public EntityBuilder ref(String name) throws NotFoundException {
        if (entityMap.containsKey(name)) {
            return entityMap.get(name);
        }
        throw new NotFoundException("entity not found: " + name);
    }

    public MetaModel build() throws NotFoundException {
        MetaModelImpl metaModelProcessor = new MetaModelImpl();

        for (EntityBuilder entityBuilder : entityMap.values()) {
            EntityMetadataImpl build = entityBuilder.build();
            metaModelProcessor.add(build);
        }

        try {
            for (EntityBuilder eb : entityMap.values()) {
                EntityMetadataImpl entityMetadata;
                entityMetadata = (EntityMetadataImpl) metaModelProcessor.findEntityMetadata(eb.getName());
                for (FieldBuilder fb : eb.fieldMap.values()) {
                    if (fb.isAssociation) {
                        FieldMetadataImpl field = (FieldMetadataImpl) entityMetadata.field(fb.outputPropertyName);

                        EntityMetadata fkEntityMetadata = metaModelProcessor.findEntityMetadata(
                                fb.associatedEntity
                        );

                        List<AssociationColumn> columns = new ArrayList<>(fb.associationColumns.size());
                        for (AssociationColumnRef ref : fb.associationColumns) {
                            FieldMetadata targetField = metaModelProcessor.findFieldMetadata(
                                    fb.associatedEntity, ref.targetFieldName()
                            );
                            // A null local column means the owning field's own column carries the
                            // foreign key — the single-column case.
                            String localColumn = ref.localDbColumnName() == null
                                    ? field.getDbColumnName()
                                    : ref.localDbColumnName();
                            columns.add(new AssociationColumn(localColumn, targetField));
                        }

                        field.setAssociationColumns(columns);
                        field.setAssociationMetadata(fkEntityMetadata);
                    }
                }
            }
        } catch (NotFoundException e) {
            // this should never happen
            throw new IllegalStateException(e);
        }


        return metaModelProcessor;
    }

}
