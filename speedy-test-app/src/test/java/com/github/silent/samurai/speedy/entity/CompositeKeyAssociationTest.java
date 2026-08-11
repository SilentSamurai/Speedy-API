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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

// Covers OrderShipment.order -- the only association in this app whose target (Order) has a
// composite primary key, so it is mapped through two foreign-key columns via @JoinColumns.
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
        // The single-column accessor keeps naming the first pair.
        assertThat(field.getAssociatedFieldMetadata().getOutputPropertyName(), is("productId"));
        assertThat(field.getDbColumnName(), is("order_product_id"));
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
        speedyClient.query("OrderShipment")
                .where(condition("order.price", eq(44.0)))
                .execute()
                .expectOk()
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
}
