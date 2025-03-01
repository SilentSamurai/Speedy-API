package com.github.silent.samurai.speedy.parser;


import com.github.silent.samurai.speedy.data.ComposedProduct;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import com.github.silent.samurai.speedy.models.conditions.EqCondition;
import com.github.silent.samurai.speedy.query.SpeedyQueryHelper;
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
    MetaModel metaModel;

    EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(ComposedProduct.class);

    String UriRoot = SpeedyConstant.URI;

    @Test
    void processRequest1_1() throws Exception {
        Mockito.when(metaModel.findEntityMetadata("ComposedProduct")).thenReturn(entityMetadata);

        SpeedyUriContext parser = new SpeedyUriContext(metaModel, UriRoot + "/ComposedProduct(productItem.id='1')");
        SpeedyQuery speedyQuery = parser.parse();
        SpeedyQueryHelper speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);

        assertEquals("ComposedProduct", speedyQuery.getFrom().getName());
        EqCondition condition = (EqCondition) speedyQuery.getWhere().getConditions().get(0);
        FieldMetadata fieldMetadata = condition.getField().getAssociatedFieldMetadata();
        String condValue = SpeedyValueFactory.toJavaType(fieldMetadata, condition.getSpeedyValue());
        assertEquals("1", condValue);
        assertFalse(speedyQueryHelper.isOnlyIdentifiersPresent());
    }

}
