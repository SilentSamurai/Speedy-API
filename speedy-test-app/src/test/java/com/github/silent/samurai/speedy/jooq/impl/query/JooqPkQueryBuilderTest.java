package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.data.UniqueProduct;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.jooq.impl.conversion.TypeConverter;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyText;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JooqPkQueryBuilderTest {

    private DSLContext dslContext;
    private TypeConverter converter;
    private EntityMetadata productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
    private EntityMetadata uniqueProductMetadata = StaticEntityMetadata.createEntityMetadata(UniqueProduct.class);

    @BeforeEach
    void setUp() {
        dslContext = DSL.using(SQLDialect.H2);
        converter = TypeConverter.defaults();
    }

    private static SpeedyEntityKey singleKey(EntityMetadata meta, String value) {
        SpeedyEntityKey pk = new SpeedyEntityKey(meta);
        Set<KeyFieldMetadata> kfs = meta.getKeyFields();
        KeyFieldMetadata kf = kfs.iterator().next();
        pk.put(kf, new SpeedyText(value));
        return pk;
    }

    // Covers pkCondition with a single key — equality condition on the primary key column
    @Test
    void pkCondition_singleKey_rendersEquality() throws SpeedyHttpException {
        SpeedyEntityKey pk = singleKey(productMetadata, "P1");
        var builder = new JooqPkQueryBuilder(dslContext, dslContext.dialect(), converter);
        Condition cond = builder.pkCondition(pk);
        assertNotNull(cond);
        assertTrue(cond.toString().contains("\"PRODUCT\".\"ID\" = 'P1'"));
    }

    // Covers findByPrimaryKeys with an empty list — early return guard
    @Test
    void findByPrimaryKeys_emptyList_returnsEmptyResult() throws SpeedyHttpException {
        var builder = new JooqPkQueryBuilder(dslContext, dslContext.dialect(), converter);
        var result = builder.findByPrimaryKeys(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Covers findByPrimaryKeys with null — same empty-result guard path
    @Test
    void findByPrimaryKeys_nullList_returnsEmptyResult() throws SpeedyHttpException {
        var builder = new JooqPkQueryBuilder(dslContext, dslContext.dialect(), converter);
        var result = builder.findByPrimaryKeys(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Covers keysCondition for a single-key entity — generates an IN clause
    @Test
    void keysCondition_singleKey_rendersInClause() throws SpeedyHttpException {
        SpeedyEntityKey pk1 = singleKey(productMetadata, "P1");
        SpeedyEntityKey pk2 = singleKey(productMetadata, "P2");
        var builder = new JooqPkQueryBuilder(dslContext, dslContext.dialect(), converter);
        Condition cond = builder.keysCondition(List.of(pk1, pk2));
        assertNotNull(cond);
        assertTrue(cond.toString().contains("in ("));
    }

}
