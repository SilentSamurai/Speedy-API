package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.AssociationColumn;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.isnotnull;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.isnull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

// Covers OrderShipment's two associations to Order -- the only target in this app with a composite
// primary key, so each is mapped through two foreign-key columns via @JoinColumns (`order` required,
// `returnOrder` nullable).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CompositeKeyAssociationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SpeedyFactory speedyFactory;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    private String createProduct() {
        return speedyClient.create("Product")
                .field("name", "prod-" + UUID.randomUUID())
                .field("category.id", "1")
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");
    }

    private String createSupplier() {
        String phone = "c" + (System.nanoTime() % 100000000L);
        return speedyClient.create("Supplier")
                .field("name", "sup-" + UUID.randomUUID())
                .field("address", "addr")
                .field("email", "sup@example.com")
                .field("phoneNo", phone)
                .field("altPhoneNo", "a" + phone)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");
    }

    private void createOrder(String productId, String supplierId, double price) {
        speedyClient.create("Order")
                .field("productId", productId)
                .field("supplierId", supplierId)
                .field("orderDate", java.time.Instant.now().toString())
                .field("price", price)
                .field("discount", 0.0)
                .execute()
                .expectOk();
    }

    private String createShipment(String productId, String supplierId, String carrier) {
        return speedyClient.create("OrderShipment")
                .field("carrier", carrier)
                .field("order.productId", productId)
                .field("order.supplierId", supplierId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");
    }

    @Test
    void metamodel_joinColumns_mapEveryKeyColumnInTargetKeyOrder() throws NotFoundException {
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata field = metaModel.findFieldMetadata("OrderShipment", "order");

        assertThat(field.isAssociation(), is(true));
        assertThat(field.isCompositeAssociation(), is(true));
        assertThat(field.getAssociationMetadata().getName(), is("Order"));

        List<AssociationColumn> columns = field.getAssociationColumns();
        assertThat(columns.stream().map(AssociationColumn::localDbColumnName).toList(),
                contains("order_product_id", "order_supplier_id"));
        assertThat(columns.stream().map(c -> c.targetKeyField().getOutputPropertyName()).toList(),
                contains("productId", "supplierId"));
        // The single-column accessor keeps naming the first pair, and the reported column is the
        // one that pair maps -- the two are read together as (column, type-of-column).
        assertThat(field.getAssociatedFieldMetadata().getOutputPropertyName(), is("productId"));
        assertThat(field.getDbColumnName(), is("order_product_id"));
    }

    @Test
    void metamodel_joinColumnFlagsAgreeingAcrossColumns() throws NotFoundException {
        MetaModel metaModel = speedyFactory.getMetaModel();

        // `order`'s two @JoinColumns are both nullable = false, `returnOrder`'s both nullable = true.
        FieldMetadata order = metaModel.findFieldMetadata("OrderShipment", "order");
        assertThat(order.isNullable(), is(false));
        assertThat(order.isRequired(), is(true));

        FieldMetadata returnOrder = metaModel.findFieldMetadata("OrderShipment", "returnOrder");
        assertThat(returnOrder.isCompositeAssociation(), is(true));
        assertThat(returnOrder.isNullable(), is(true));
        assertThat(returnOrder.getAssociationColumns().stream()
                        .map(AssociationColumn::localDbColumnName).toList(),
                contains("return_order_product_id", "return_order_supplier_id"));
    }

    @Test
    void metamodel_joinColumnFlagsDisagreeingAcrossColumns_areCombined() throws NotFoundException {
        // OrderAudit.auditedOrder's first @JoinColumn takes JPA's defaults and only its *second*
        // marks itself unique. The foreign key is unique as soon as any single column pins it down,
        // so this reads true -- where taking the first-declared column's flags would read false.
        //
        // `unique` is the only flag this can be shown with: Hibernate refuses to map a property
        // whose join columns disagree on insertable, updatable or nullable (see OrderAudit), so
        // combining those three is defensive only and they stay at their agreed values here.
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata auditedOrder = metaModel.findFieldMetadata("OrderAudit", "auditedOrder");

        assertThat(auditedOrder.isCompositeAssociation(), is(true));
        assertThat(auditedOrder.getAssociationColumns().stream()
                        .map(AssociationColumn::localDbColumnName).toList(),
                contains("audited_product_id", "audited_supplier_id"));

        assertThat(auditedOrder.isUnique(), is(true));

        assertThat(auditedOrder.isNullable(), is(true));
        assertThat(auditedOrder.isInsertable(), is(true));
        assertThat(auditedOrder.isUpdatable(), is(true));
    }

    @Test
    void create_roundTripsAsKeysOnlyReferenceCarryingEveryKeyField() {
        String productId = createProduct();
        String supplierId = createSupplier();
        createOrder(productId, supplierId, 10.0);

        String shipmentId = createShipment(productId, supplierId, "carrier-" + UUID.randomUUID());

        speedyClient.get("OrderShipment")
                .key("id", shipmentId)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].order.productId", productId)
                .expectJsonPath("$.payload[0].order.supplierId", supplierId);
    }

    @Test
    void expand_resolvesTheRowMatchingEveryKeyColumn() {
        // The two orders share a product and differ only in supplier. Matching on the first FK column
        // alone would resolve either shipment to whichever row came back first.
        String productId = createProduct();
        String supplierA = createSupplier();
        String supplierB = createSupplier();
        createOrder(productId, supplierA, 11.0);
        createOrder(productId, supplierB, 22.0);

        String shipmentA = createShipment(productId, supplierA, "carrier-" + UUID.randomUUID());
        String shipmentB = createShipment(productId, supplierB, "carrier-" + UUID.randomUUID());

        speedyClient.query("OrderShipment")
                .where(condition("id", eq(shipmentA)))
                .expand("Order")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].order.supplierId", supplierA)
                .expectJsonPath("$.payload[0].order.price", 11.0);

        speedyClient.query("OrderShipment")
                .where(condition("id", eq(shipmentB)))
                .expand("Order")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].order.supplierId", supplierB)
                .expectJsonPath("$.payload[0].order.price", 22.0);
    }

    @Test
    void query_filterByAssociatedField_joinsOnEveryKeyColumn() {
        String productId = createProduct();
        String supplierA = createSupplier();
        String supplierB = createSupplier();
        createOrder(productId, supplierA, 33.0);
        createOrder(productId, supplierB, 44.0);

        createShipment(productId, supplierA, "carrier-" + UUID.randomUUID());
        String shipmentB = createShipment(productId, supplierB, "carrier-b-" + UUID.randomUUID());

        // Both shipments share order.productId; the join must still bring back the row whose
        // *other* key column matches too, so filtering on the target's price picks exactly one.
        // Joining on product_id alone would match both orders and return both shipments, so the
        // row count is the assertion that actually catches it.
        speedyClient.query("OrderShipment")
                .where(condition("order.price", eq(44.0)))
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1))
                .expectJsonPath("$.payload[0].id", shipmentB);
    }

    @Test
    void update_reassigningAssociation_rewritesEveryKeyColumn() {
        String productId = createProduct();
        String supplierA = createSupplier();
        String supplierB = createSupplier();
        createOrder(productId, supplierA, 55.0);
        createOrder(productId, supplierB, 66.0);

        String shipmentId = createShipment(productId, supplierA, "carrier-" + UUID.randomUUID());

        speedyClient.update("OrderShipment")
                .key("id", shipmentId)
                .field("order.productId", productId)
                .field("order.supplierId", supplierB)
                .execute()
                .expectOk();

        speedyClient.get("OrderShipment")
                .key("id", shipmentId)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].order.supplierId", supplierB);
    }

    @Test
    void create_withPartialTargetKey_isRejected() {
        String productId = createProduct();
        String supplierId = createSupplier();
        createOrder(productId, supplierId, 77.0);

        speedyClient.create("OrderShipment")
                .field("carrier", "carrier-" + UUID.randomUUID())
                .field("order.productId", productId)
                .execute()
                .expectBadRequest()
                .expectJsonPath("$.message", containsString("order.supplierId"));
    }

    @Test
    void query_comparingTheForeignKeyToASingleValue_isRejected() {
        speedyClient.query("OrderShipment")
                .where(condition("order", eq("anything")))
                .execute()
                .expectBadRequest()
                .expectJsonPath("$.message", containsString("spanning 2 columns"));
    }

    @Test
    void select_narrowedToTheAssociation_stillReturnsEveryKeyColumn() {
        // $select emits an explicit column list; a multi-column foreign key has to contribute all
        // of its columns or the association decodes to a partial key and reads back null.
        String productId = createProduct();
        String supplierId = createSupplier();
        createOrder(productId, supplierId, 88.0);
        String shipmentId = createShipment(productId, supplierId, "carrier-" + UUID.randomUUID());

        speedyClient.query("OrderShipment")
                .where(condition("id", eq(shipmentId)))
                .select("id", "order")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].order.productId", productId)
                .expectJsonPath("$.payload[0].order.supplierId", supplierId);
    }

    @Test
    void isNull_isTrueOnlyWhenEveryForeignKeyColumnIsNull() {
        String productId = createProduct();
        String supplierId = createSupplier();
        createOrder(productId, supplierId, 99.0);

        String withoutReturn = createShipment(productId, supplierId, "carrier-" + UUID.randomUUID());
        String withReturn = speedyClient.create("OrderShipment")
                .field("carrier", "carrier-" + UUID.randomUUID())
                .field("order.productId", productId)
                .field("order.supplierId", supplierId)
                .field("returnOrder.productId", productId)
                .field("returnOrder.supplierId", supplierId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        speedyClient.get("OrderShipment")
                .key("id", withoutReturn)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].returnOrder", nullValue());

        speedyClient.query("OrderShipment")
                .where(condition("id", eq(withReturn)), condition("returnOrder", isnull()))
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(0));

        speedyClient.query("OrderShipment")
                .where(condition("id", eq(withReturn)), condition("returnOrder", isnotnull()))
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1));

        speedyClient.query("OrderShipment")
                .where(condition("id", eq(withoutReturn)), condition("returnOrder", isnull()))
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1));
    }

    @Test
    void orderBy_theForeignKeySortsByItsColumnsInKeyOrder() {
        // Both rows share the first key column, so the ordering can only come from the second one —
        // sorting by the first column alone would leave the two in arbitrary relative order.
        String productId = createProduct();
        String supplierA = createSupplier();
        String supplierB = createSupplier();
        createOrder(productId, supplierA, 1.0);
        createOrder(productId, supplierB, 2.0);

        String shipmentA = createShipment(productId, supplierA, "carrier-" + UUID.randomUUID());
        String shipmentB = createShipment(productId, supplierB, "carrier-" + UUID.randomUUID());

        // Supplier ids are generated UUIDs, so which shipment sorts first follows from the ids.
        boolean aFirst = supplierA.compareTo(supplierB) < 0;
        String firstAsc = aFirst ? shipmentA : shipmentB;
        String lastAsc = aFirst ? shipmentB : shipmentA;

        speedyClient.query("OrderShipment")
                .where(condition("order.productId", eq(productId)))
                .orderByAsc("order")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(2))
                .expectJsonPath("$.payload[0].id", firstAsc);

        speedyClient.query("OrderShipment")
                .where(condition("order.productId", eq(productId)))
                .orderByDesc("order")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", lastAsc);
    }

    @Test
    void replace_omittingANullableAssociation_clearsEveryKeyColumn() {
        String productId = createProduct();
        String supplierId = createSupplier();
        createOrder(productId, supplierId, 100.0);

        String shipmentId = speedyClient.create("OrderShipment")
                .field("carrier", "carrier-" + UUID.randomUUID())
                .field("order.productId", productId)
                .field("order.supplierId", supplierId)
                .field("returnOrder.productId", productId)
                .field("returnOrder.supplierId", supplierId)
                .execute()
                .expectOk()
                .jsonPath("$.payload[0].id");

        // PUT semantics: the omitted nullable association is reset, which must null *both* of its
        // columns -- nulling only the first would leave a half-written key behind.
        speedyClient.replace("OrderShipment")
                .key("id", shipmentId)
                .field("carrier", "carrier-replaced")
                .field("order.productId", productId)
                .field("order.supplierId", supplierId)
                .execute()
                .expectOk();

        speedyClient.get("OrderShipment")
                .key("id", shipmentId)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].returnOrder", nullValue())
                .expectJsonPath("$.payload[0].order.supplierId", supplierId);
    }
}
