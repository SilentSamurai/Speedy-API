package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.entity.Supplier;
import com.github.silent.samurai.speedy.repositories.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Bulk-update coverage for issue #97 — {@code $update} (both PATCH and PUT) now accepts an
/// array of items, mirroring bulk create/delete ({@link SpeedyBulkTest}). {@link Supplier}
/// ({@code @SpeedyBulk}) is used for the happy-path/mode tests; {@link
/// com.github.silent.samurai.speedy.entity.Order} (composite key, also {@code @SpeedyBulk})
/// covers the composite-key case. Per-row PATCH-vs-PUT semantics (leave vs. null an omitted
/// nullable field) are the same ones {@code SpeedyReplaceSemanticsTest} verifies for a single
/// entity — here we confirm they still hold when several items are written in one request.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyBulkUpdateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierRepository supplierRepository;

    private SpeedyTest client;

    /// (productId, supplierId) pairs created by the composite-key tests, torn down afterward
    /// so the shared H2 context stays clean — the same convention as SpeedyCompositeKeyTest.
    private final List<ObjectNode> createdOrderKeys = new ArrayList<>();

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
    }

    @AfterEach
    void cleanupOrders() {
        if (!createdOrderKeys.isEmpty()) {
            client.deleteMany("Order").items(createdOrderKeys).execute();
            createdOrderKeys.clear();
        }
    }

    @Test
    void bulkUpdate_patchesMultipleSuppliers_leavesOmittedFieldUnchanged() throws Exception {
        Supplier s1 = createSupplier("BulkPatch1", "Addr One", "one@example.com");
        Supplier s2 = createSupplier("BulkPatch2", "Addr Two", "two@example.com");

        // address & email omitted on both -> PATCH must leave them untouched
        client.updateMany("Supplier")
                .item(i -> i.key("id", s1.getId()).field("name", "BulkPatch1-renamed"))
                .item(i -> i.key("id", s2.getId()).field("name", "BulkPatch2-renamed"))
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(2));

        Supplier reloaded1 = reload(s1.getId());
        Supplier reloaded2 = reload(s2.getId());
        assertEquals("BulkPatch1-renamed", reloaded1.getName());
        assertEquals("Addr One", reloaded1.getAddress(), "PATCH must not touch the omitted address");
        assertEquals("BulkPatch2-renamed", reloaded2.getName());
        assertEquals("Addr Two", reloaded2.getAddress(), "PATCH must not touch the omitted address");
    }

    @Test
    void bulkReplace_replacesMultipleSuppliers_resetsOmittedFieldToNull() throws Exception {
        Supplier s1 = createSupplier("BulkPut1", "Addr One", "one@example.com");
        Supplier s2 = createSupplier("BulkPut2", "Addr Two", "two@example.com");

        // address & email omitted on both -> full replace resets them
        client.replaceMany("Supplier")
                .item(i -> i.key("id", s1.getId())
                        .field("name", "BulkPut1-replaced")
                        .field("phoneNo", s1.getPhoneNo())
                        .field("altPhoneNo", s1.getAltPhoneNo()))
                .item(i -> i.key("id", s2.getId())
                        .field("name", "BulkPut2-replaced")
                        .field("phoneNo", s2.getPhoneNo())
                        .field("altPhoneNo", s2.getAltPhoneNo()))
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(2));

        Supplier reloaded1 = reload(s1.getId());
        Supplier reloaded2 = reload(s2.getId());
        assertNull(reloaded1.getAddress(), "PUT must reset the omitted nullable address to null");
        assertNull(reloaded2.getAddress(), "PUT must reset the omitted nullable address to null");
    }

    @Test
    void emptyUpdateArray_shouldSucceed() throws Exception {
        client.updateMany("Supplier").execute()
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(0));
    }

    @Test
    void emptyReplaceArray_shouldSucceed() throws Exception {
        client.replaceMany("Supplier").execute()
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(0));
    }

    /// A single-element array must still return the full updated entity (not just its key) —
    /// regression guard for the single-update "returns the resource" contract, since bulk
    /// success responses are now built by the same code path as the single-item case.
    @Test
    void singleElementBulkUpdate_returnsFullEntity() throws Exception {
        Supplier s = createSupplier("SingleBulk", "Some Address", "single@example.com");

        client.updateMany("Supplier")
                .item(i -> i.key("id", s.getId()).field("name", "SingleBulk-renamed"))
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(1))
                .expectJsonPath("$.payload[0].name", equalTo("SingleBulk-renamed"))
                .expectJsonPath("$.payload[0].phoneNo", equalTo(s.getPhoneNo()))
                .expectJsonPath("$.payload[0].address", equalTo("Some Address"));
    }

    /// PER_ENTITY mode (Supplier's default): one item targets a non-existent PK, the other is
    /// valid -> 207 with the valid item committed despite the other's failure.
    @Test
    void perEntityMode_partialFailure_returns207AndCommitsTheValidItem() throws Exception {
        Supplier s = createSupplier("PartialUpd", "Addr", "partial@example.com");

        client.updateMany("Supplier")
                .item(i -> i.key("id", s.getId()).field("name", "PartialUpd-renamed"))
                .item(i -> i.key("id", "non-existent-id-99999").field("name", "ghost-rename"))
                .transaction("per-entity")
                .execute()
                .expectStatus(207)
                .expectJsonPath("$.succeeded[*]", hasSize(1))
                .expectJsonPath("$.failed[*]", hasSize(1))
                .expectJsonPath("$.failed[0].index", equalTo(1))
                .expectJsonPath("$.failed[0].status", equalTo(404));

        Supplier reloaded = reload(s.getId());
        assertEquals("PartialUpd-renamed", reloaded.getName(), "the valid item must still be committed");
    }

    /// BATCH mode: one item targets a non-existent PK -> the whole transaction rolls back,
    /// including the item that would otherwise have succeeded.
    @Test
    void batchMode_oneFailure_rollsBackTheWholeBatch() throws Exception {
        Supplier s = createSupplier("BatchUpd", "Addr", "batch@example.com");
        String originalName = s.getName();

        client.updateMany("Supplier")
                .item(i -> i.key("id", s.getId()).field("name", "BatchUpd-shouldNotStick"))
                .item(i -> i.key("id", "non-existent-id-88888").field("name", "ghost-rename"))
                .transaction("batch")
                .execute()
                .expectStatus(404);

        Supplier reloaded = reload(s.getId());
        assertEquals(originalName, reloaded.getName(), "batch failure must roll back the other item's write too");
    }

    @Test
    void bulkUpdate_compositeKeyOrders_patchLeavesOmittedFieldUnchanged() throws Exception {
        createOrder("4", "3", 50.0, 5.0);
        createOrder("6", "1", 60.0, 6.0);

        // price omitted on both -> PATCH must leave it unchanged
        client.updateMany("Order")
                .item(i -> i.key("productId", "4").key("supplierId", "3").field("discount", 55.0))
                .item(i -> i.key("productId", "6").key("supplierId", "1").field("discount", 66.0))
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(2));

        client.get("Order").key("productId", "4").key("supplierId", "3").execute()
                .expectOk()
                .expectJsonPath("$.payload[0].discount", equalTo(55.0))
                .expectJsonPath("$.payload[0].price", equalTo(50.0));
        client.get("Order").key("productId", "6").key("supplierId", "1").execute()
                .expectOk()
                .expectJsonPath("$.payload[0].discount", equalTo(66.0))
                .expectJsonPath("$.payload[0].price", equalTo(60.0));
    }

    @Test
    void bulkReplace_compositeKeyOrders_putResetsOmittedFieldToNull() throws Exception {
        createOrder("5", "2", 70.0, 7.0);
        createOrder("2", "1", 80.0, 8.0);

        // price omitted on both -> full replace nulls it
        client.replaceMany("Order")
                .item(i -> i.key("productId", "5").key("supplierId", "2").field("discount", 77.0))
                .item(i -> i.key("productId", "2").key("supplierId", "1").field("discount", 88.0))
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(2));

        client.get("Order").key("productId", "5").key("supplierId", "2").execute()
                .expectOk()
                .expectJsonPath("$.payload[0].discount", equalTo(77.0))
                .expectJsonPath("$.payload[0].price", nullValue());
        client.get("Order").key("productId", "2").key("supplierId", "1").execute()
                .expectOk()
                .expectJsonPath("$.payload[0].discount", equalTo(88.0))
                .expectJsonPath("$.payload[0].price", nullValue());
    }

    /* -------------------------------------------------------------------- */
    /* Helpers                                                              */
    /* -------------------------------------------------------------------- */

    private Supplier createSupplier(String namePrefix, String address, String email) {
        String phone = uniquePhone();
        Supplier supplier = new Supplier();
        supplier.setName(namePrefix + "-" + System.nanoTime());
        supplier.setAddress(address);
        supplier.setEmail(email);
        supplier.setPhoneNo(phone);
        supplier.setAltPhoneNo("a" + phone);
        supplierRepository.save(supplier);
        return supplier;
    }

    private Supplier reload(String id) {
        Optional<Supplier> found = supplierRepository.findById(id);
        assertTrue(found.isPresent(), "supplier should still exist after write");
        return found.get();
    }

    private String uniquePhone() {
        return "b" + (System.nanoTime() % 100000000L);
    }

    private void createOrder(String productId, String supplierId, double price, double discount) {
        ObjectNode body = client.create("Order")
                .field("productId", productId)
                .field("supplierId", supplierId)
                .field("orderDate", java.time.Instant.now().toString())
                .field("price", price)
                .field("discount", discount)
                .build();
        client.createOne("Order", body).expectOk();
        createdOrderKeys.add(client.delete("Order").key("productId", productId).key("supplierId", supplierId).build());
    }
}
