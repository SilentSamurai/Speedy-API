package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;

import java.util.HashMap;
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
        throw new NotFoundException("entity not found" + name);
    }

    public MetaModel build() {
        MetaModelImpl metaModelProcessor = new MetaModelImpl();

        entityMap.values().stream().map(EntityBuilder::build)
                .forEach(metaModelProcessor::add);

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
