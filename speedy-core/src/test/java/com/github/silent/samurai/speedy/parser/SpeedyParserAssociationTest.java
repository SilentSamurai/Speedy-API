package com.github.silent.samurai.speedy.parser;


import com.github.silent.samurai.speedy.data.AssociatedEntityTestClass;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SpeedyParserAssociationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyParserAssociationTest.class);

    @Mock
    MetaModelProcessor metaModelProcessor;

    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(AssociatedEntityTestClass.class);

    String UriRoot = SpeedyConstant.URI;

    @Test
    void processRequest1_1() throws Exception {
        Mockito.when(metaModelProcessor.findEntityMetadata(Mockito.anyString())).thenReturn(entityMetadata);

        SpeedyUriContext parser = new SpeedyUriContext(metaModelProcessor, UriRoot + "/Customer(associationEntity.id='1')");
        parser.parse();

        assertEquals("Customer", parser.getPrimaryResource().getResource());
        assertEquals("1", parser.getPrimaryResource().getFirstFilterValue("associationEntity.id", String.class));
        assertFalse(parser.getPrimaryResource().isOnlyIdentifiersPresent());
    }

}
