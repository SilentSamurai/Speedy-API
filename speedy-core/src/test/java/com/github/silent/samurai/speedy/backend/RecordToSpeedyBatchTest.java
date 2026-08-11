package com.github.silent.samurai.speedy.backend;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.backend.RowReader;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.FieldBuilder;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.Speedy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Covers the batched `$expand` resolution: association targets are fetched one query per
/// expansion level for the whole result set, instead of one query per row (the N+1 this replaces).
class RecordToSpeedyBatchTest {

    private EntityMetadata inventory;
    private EntityMetadata product;
    private EntityMetadata category;

    @BeforeEach
    void setUp() throws Exception {
        MetaModelBuilder builder = MetadataBuilder.builder();
        EntityBuilder categoryBuilder = builder.entity("Category");
        EntityBuilder productBuilder = builder.entity("Product");
        EntityBuilder inventoryBuilder = builder.entity("Inventory");

        categoryBuilder.keyField("id", "ID", ColumnType.VARCHAR);
        categoryBuilder.field("name", "NAME", ColumnType.VARCHAR);

        productBuilder.keyField("id", "ID", ColumnType.VARCHAR);
        productBuilder.field("name", "NAME", ColumnType.VARCHAR);
        FieldBuilder categoryField = productBuilder.field("category", "CATEGORY_ID", ColumnType.VARCHAR);
        categoryField.associateWith(categoryBuilder.keyFields().iterator().next());

        inventoryBuilder.keyField("id", "ID", ColumnType.VARCHAR);
        FieldBuilder productField = inventoryBuilder.field("product", "PRODUCT_ID", ColumnType.VARCHAR);
        productField.associateWith(productBuilder.keyFields().iterator().next());

        MetaModel metaModel = builder.build();
        inventory = metaModel.findEntityMetadata("Inventory");
        product = metaModel.findEntityMetadata("Product");
        category = metaModel.findEntityMetadata("Category");
    }

    @Test
    void expandingManyRowsFetchesTheAssociationOnce() throws Exception {
        RecordingRowReader reader = new RecordingRowReader();
        reader.addRow(categoryRow("c1", "Tools"));
        reader.addRow(categoryRow("c2", "Toys"));

        List<SpeedyEntity> rows = List.of(
                productRow("p1", "Hammer", "c1"),
                productRow("p2", "Saw", "c1"),
                productRow("p3", "Kite", "c2"));

        List<SpeedyEntity> result = new RecordToSpeedy(reader)
                .fromRows(rows, product, Set.of("Category"));

        assertEquals(1, reader.fkFetches.size(),
                "expected one batched fetch for the Category expansion, got " + reader.fkFetches);
        assertEquals(2, reader.fkFetches.get(0).size(),
                "expected the two distinct category foreign keys in a single fetch");

        assertEquals("Tools", expandedCategoryName(result.get(0)));
        assertEquals("Tools", expandedCategoryName(result.get(1)));
        assertEquals("Toys", expandedCategoryName(result.get(2)));
    }

    @Test
    void nestedExpansionCostsOneFetchPerLevelNotPerRow() throws Exception {
        RecordingRowReader reader = new RecordingRowReader();
        reader.addRow(categoryRow("c1", "Tools"));
        reader.addRow(categoryRow("c2", "Toys"));
        reader.addRow(productRow("p1", "Hammer", "c1"));
        reader.addRow(productRow("p2", "Saw", "c1"));
        reader.addRow(productRow("p3", "Kite", "c2"));

        List<SpeedyEntity> rows = List.of(
                inventoryRow("i1", "p1"),
                inventoryRow("i2", "p2"),
                inventoryRow("i3", "p3"));

        List<SpeedyEntity> result = new RecordToSpeedy(reader)
                .fromRows(rows, inventory, Set.of("Product", "Product.Category"));

        assertEquals(2, reader.fkFetches.size(),
                "expected one fetch per expansion level (Product, then Category), got " + reader.fkFetches);

        SpeedyEntity firstProduct = result.get(0).get(inventory.field("product")).asObject();
        assertEquals("Hammer", firstProduct.get(product.field("name")).asText());
        assertEquals("Tools", firstProduct.get(product.field("category")).asObject()
                .get(category.field("name")).asText());
    }

    @Test
    void rowsWithoutAForeignKeyResolveToNullAndAreLeftOutOfTheFetch() throws Exception {
        RecordingRowReader reader = new RecordingRowReader();
        reader.addRow(categoryRow("c1", "Tools"));

        SpeedyEntity withoutFkColumn = new SpeedyEntity(product);
        withoutFkColumn.put(product.field("id"), Speedy.from("p2"));
        withoutFkColumn.put(product.field("name"), Speedy.from("Saw"));

        SpeedyEntity withNullFk = productRow("p3", "Kite", "c9");
        withNullFk.put(product.field("category"), Speedy.fromNull());

        List<SpeedyEntity> rows = List.of(
                productRow("p1", "Hammer", "c1"),
                withoutFkColumn,
                withNullFk);

        List<SpeedyEntity> result = new RecordToSpeedy(reader)
                .fromRows(rows, product, Set.of("Category"));

        assertEquals(1, reader.fkFetches.size());
        assertEquals(List.of(Speedy.from("c1")), reader.fkFetches.get(0),
                "only the one resolvable foreign key should be fetched");

        assertEquals("Tools", expandedCategoryName(result.get(0)));
        assertTrue(result.get(1).get(product.field("category")).isNull(),
                "a row whose foreign-key column was never selected should read as null");
        assertTrue(result.get(2).get(product.field("category")).isNull(),
                "a row whose foreign key is SQL NULL should read as null");
    }

    @Test
    void aForeignKeyWithNoMatchingTargetResolvesToNull() throws Exception {
        RecordingRowReader reader = new RecordingRowReader();
        reader.addRow(categoryRow("c1", "Tools"));

        List<SpeedyEntity> rows = List.of(
                productRow("p1", "Hammer", "c1"),
                productRow("p2", "Saw", "gone"));

        List<SpeedyEntity> result = new RecordToSpeedy(reader)
                .fromRows(rows, product, Set.of("Category"));

        assertEquals("Tools", expandedCategoryName(result.get(0)));
        assertTrue(result.get(1).get(product.field("category")).isNull(),
                "a dangling foreign key should read as null, not drop the row");
        assertEquals(2, result.size());
    }

    @Test
    void aForeignKeyBatchLargerThanTheChunkSizeIsSplitAcrossFetches() throws Exception {
        RecordingRowReader reader = new RecordingRowReader();
        List<SpeedyEntity> rows = new ArrayList<>();
        for (int i = 0; i < 620; i++) {
            reader.addRow(categoryRow("c" + i, "Category " + i));
            rows.add(productRow("p" + i, "Product " + i, "c" + i));
        }

        List<SpeedyEntity> result = new RecordToSpeedy(reader)
                .fromRows(rows, product, Set.of("Category"));

        assertEquals(2, reader.fkFetches.size(),
                "620 distinct foreign keys should split into two fetches, got " + reader.fkFetches.size());
        assertEquals(500, reader.fkFetches.get(0).size());
        assertEquals(120, reader.fkFetches.get(1).size());

        assertEquals("Category 0", expandedCategoryName(result.get(0)));
        assertEquals("Category 619", expandedCategoryName(result.get(619)));
    }

    @Test
    void anAssociationOutsideTheExpandSetIsNotFetchedAtAll() throws Exception {
        RecordingRowReader reader = new RecordingRowReader();
        reader.addRow(categoryRow("c1", "Tools"));

        List<SpeedyEntity> result = new RecordToSpeedy(reader)
                .fromRows(List.of(productRow("p1", "Hammer", "c1")), product, Set.of());

        assertEquals(0, reader.fkFetches.size(), "a keys-only reference must not hit the backend");
        SpeedyValue categoryRef = result.get(0).get(product.field("category"));
        assertTrue(categoryRef.isObject());
        assertEquals("c1", categoryRef.asObject().get(category.field("id")).asText());
    }

    private SpeedyEntity inventoryRow(String id, String productFk) throws Exception {
        SpeedyEntity row = new SpeedyEntity(inventory);
        row.put(inventory.field("id"), Speedy.from(id));
        row.put(inventory.field("product"), Speedy.from(productFk));
        return row;
    }

    private String expandedCategoryName(SpeedyEntity productEntity) throws Exception {
        SpeedyValue value = productEntity.get(product.field("category"));
        assertTrue(value.isObject(), "category should be expanded to an object");
        return value.asObject().get(category.field("name")).asText();
    }

    private SpeedyEntity productRow(String id, String name, String categoryFk) throws Exception {
        SpeedyEntity row = new SpeedyEntity(product);
        row.put(product.field("id"), Speedy.from(id));
        row.put(product.field("name"), Speedy.from(name));
        row.put(product.field("category"), Speedy.from(categoryFk));
        return row;
    }

    private SpeedyEntity categoryRow(String id, String name) throws Exception {
        SpeedyEntity row = new SpeedyEntity(category);
        row.put(category.field("id"), Speedy.from(id));
        row.put(category.field("name"), Speedy.from(name));
        return row;
    }

    /// Hand-written {@link RowReader} that serves rows from an in-memory table and records the
    /// foreign-key batches it was asked for, so a test can assert how many round-trips happened.
    private static final class RecordingRowReader implements RowReader {

        private final List<SpeedyEntity> table = new ArrayList<>();
        private final List<List<SpeedyValue>> fkFetches = new ArrayList<>();

        void addRow(SpeedyEntity row) {
            table.add(row);
        }

        @Override
        public List<SpeedyEntity> selectByFks(FieldMetadata association, List<SpeedyValue> fkValues) {
            fkFetches.add(List.copyOf(fkValues));
            FieldMetadata targetField = association.getAssociatedFieldMetadata();
            List<SpeedyEntity> matches = new ArrayList<>();
            for (SpeedyEntity row : table) {
                if (row.getMetadata().equals(association.getAssociationMetadata())
                        && row.has(targetField)
                        && fkValues.contains(row.get(targetField))) {
                    matches.add(row);
                }
            }
            return matches;
        }

        @Override
        public List<SpeedyEntity> select(SpeedyQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BigInteger count(SpeedyQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SpeedyEntity> selectByKeys(List<SpeedyEntityKey> keys) {
            throw new UnsupportedOperationException();
        }
    }
}
