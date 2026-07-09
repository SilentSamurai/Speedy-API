package com.github.silent.samurai.speedy.url;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.OrderApi;
import org.openapitools.client.model.CreateOrderRequest;
import org.openapitools.client.model.Order;
import org.openapitools.client.model.OrderKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Verifies PUT (full replace) vs PATCH (partial update) semantics — issue #125 — on the
/// composite-key {@link Order} entity ({@code productId} + {@code supplierId}), the HTTP-layer
/// counterpart to {@code SpeedyReplaceSemanticsTest} (which covers the single-key {@code Supplier}).
/// <p>
/// The generated OpenAPI client exposes no {@code patch} method, so PATCH/PUT are driven as raw
/// HTTP verbs against {@code $update} with the composite key in the body; the row is seeded and
/// re-read through the {@link OrderApi} client. {@code Order}'s three non-key fields
/// ({@code orderDate}, {@code price}, {@code discount}) are all nullable, so omitting one lets us
/// tell PATCH (leaves it) from PUT (nulls it) apart.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyCompositeKeyPatchTest {

    private static final String UPDATE_URL = SpeedyConstants.URI + "/Order/" + SpeedyEndpoint.UPDATE.suffix();

    @Autowired
    private MockMvc mvc;

    private OrderApi apiInstance;
    private final List<OrderKey> createdKeys = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        ApiClient client = new ApiClient(new RestTemplate(requestFactory));
        apiInstance = new OrderApi(client);
    }

    @AfterEach
    void cleanup() {
        for (OrderKey key : createdKeys) {
            try {
                apiInstance.bulkDeleteOrder(List.of(key));
            } catch (RuntimeException ignored) {
                // best-effort teardown
            }
        }
        createdKeys.clear();
    }

    /// PATCH omitting a nullable field must leave it untouched (partial update).
    @Test
    void patch_omittingNullableField_leavesItUnchanged() throws Exception {
        createOrder("1", "1", 90.9, 10.0);

        ObjectNode body = keyBody("1", "1");
        body.put("discount", 100.0); // price omitted

        mvc.perform(MockMvcRequestBuilders.patch(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());

        Order reloaded = reload("1", "1");
        assertEquals(100.0, reloaded.getDiscount());
        assertEquals(90.9, reloaded.getPrice(), "PATCH must not touch the omitted price");
    }

    /// PUT omitting a nullable field must reset it to null (full replace).
    @Test
    void put_omittingNullableField_resetsItToNull() throws Exception {
        createOrder("2", "1", 90.9, 10.0);

        ObjectNode body = keyBody("2", "1");
        body.put("discount", 100.0); // price omitted -> full replace nulls it

        mvc.perform(MockMvcRequestBuilders.put(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());

        Order reloaded = reload("2", "1");
        assertEquals(100.0, reloaded.getDiscount());
        assertNull(reloaded.getPrice(), "PUT must reset the omitted nullable price to null");
    }

    /// PATCH against a well-formed but non-existent composite key must return 404.
    @Test
    void patch_onNonExistentCompositeKey_returns404() throws Exception {
        ObjectNode body = keyBody("7", "7"); // valid FK pair, never created
        body.put("discount", 5.0);

        mvc.perform(MockMvcRequestBuilders.patch(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isNotFound());
    }

    /// PATCH with one key column missing must be rejected as an incomplete composite key (400).
    @Test
    void patch_incompleteKey_returns400() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("productId", "1"); // supplierId deliberately omitted
        body.put("discount", 5.0);

        mvc.perform(MockMvcRequestBuilders.patch(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest());
    }

    /// The composite key in the body is only ever used to locate the row (the SQL WHERE
    /// clause) — it is never written back into the SET clause. Regression test for issue #115:
    /// sending a key that resolves to a *different* existing row must update that row's
    /// non-key fields only, and must leave the original row's key and fields untouched — there
    /// is no way for a $update body to "change" a row's key, since the same key value is used
    /// both to find the row and (if it were writable) to set it.
    @Test
    void patch_keyIdentifiesRowOnly_originalRowUntouchedByOtherKeysUpdate() throws Exception {
        createOrder("3", "1", 50.0, 5.0);
        createOrder("3", "2", 75.0, 7.5);

        ObjectNode body = keyBody("3", "2");
        body.put("discount", 99.0);

        mvc.perform(MockMvcRequestBuilders.patch(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());

        Order untouched = reload("3", "1");
        assertEquals("3", untouched.getProductId());
        assertEquals("1", untouched.getSupplierId());
        assertEquals(5.0, untouched.getDiscount(), "row not addressed by the request must be unaffected");

        Order updated = reload("3", "2");
        assertEquals("3", updated.getProductId(), "key columns are never part of the SET clause");
        assertEquals("2", updated.getSupplierId(), "key columns are never part of the SET clause");
        assertEquals(99.0, updated.getDiscount());
    }

    /* -------------------------------------------------------------------- */
    /* Helpers                                                              */
    /* -------------------------------------------------------------------- */

    private ObjectNode keyBody(String productId, String supplierId) {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("productId", productId);
        body.put("supplierId", supplierId);
        return body;
    }

    private void createOrder(String productId, String supplierId, double price, double discount) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(productId);
        request.setSupplierId(supplierId);
        request.setOrderDate(Instant.now().toString());
        request.setPrice(price);
        request.setDiscount(discount);
        List<OrderKey> payload = apiInstance.bulkCreateOrder(List.of(request)).getPayload();
        createdKeys.add(payload.get(0));
    }

    private Order reload(String productId, String supplierId) {
        List<Order> payload = apiInstance.getOrder(productId, supplierId).getPayload();
        return payload.get(0);
    }
}
