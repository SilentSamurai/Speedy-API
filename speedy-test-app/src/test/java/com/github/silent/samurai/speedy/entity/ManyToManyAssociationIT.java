package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.jpa.impl.processors.JpaMetaModelProcessorV2;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/// Tests that @ManyToMany associations are gracefully skipped at metamodel build time.
///
/// ## What we test
/// JpaMetaModelProcessorV2 warns and continues when it encounters a @ManyToMany
/// association instead of crashing. The owning entity (and its PK field) is still
/// registered in the metamodel; only the @ManyToMany field is excluded.
///
/// ## How we test
/// @SpringBootTest boots the full application with TestApplication so the metamodel
/// processor discovers the ManyToManyEntityA/B entities. We instantiate the processor
/// manually and verify that processMetaModel succeeds, both entities appear in the
/// model, and the @ManyToMany collection fields are absent.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ManyToManyAssociationIT {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("@ManyToMany fields are gracefully skipped — entity is added without the association")
    void manyToManyFieldIsSkipped() throws NotFoundException {
        ISpeedyConfiguration config = mock(ISpeedyConfiguration.class);
        JpaMetaModelProcessorV2 processor = new JpaMetaModelProcessorV2(config, entityManagerFactory);
        MetaModelBuilder builder = MetadataBuilder.builder();

        processor.processMetaModel(builder);
        MetaModel model = processor.getMetaModel();

        assertTrue(model.hasEntityMetadata("ManyToManyEntityA"));
        assertTrue(model.hasEntityMetadata("ManyToManyEntityB"));

        EntityMetadata entityA = model.findEntityMetadata("ManyToManyEntityA");
        assertTrue(entityA.getKeyFieldNames().contains("id"),
                "id key field should be present");
        assertFalse(entityA.getAllFieldNames().contains("entitiesB"),
                "@ManyToMany field should be absent from model");
        assertThrows(NotFoundException.class,
                () -> model.findFieldMetadata("ManyToManyEntityA", "entitiesB"));
    }

}
