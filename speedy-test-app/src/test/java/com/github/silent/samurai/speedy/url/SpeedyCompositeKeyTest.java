package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.OrderApi;
import org.openapitools.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.in;

/// End-to-end CRUD, filtering and negative coverage for the only composite-key entity in the
/// test app: {@link org.openapitools.client.model.Order} — a two-column key of
/// {@code productId} + {@code supplierId} ({@code @IdClass(OrderId.class)}).
/// <p>
/// Each test is self-contained: it owns a distinct {@code (productId, supplierId)} tuple drawn
/// from the seeded {@code products} (1–7) / {@code suppliers} (1,2,3,5,6,7) rows, and every order
/// it creates is torn down in {@link #cleanup()}. The {@code orders} table starts empty, so tests
/// never collide. PATCH/PUT *semantics* live in {@code SpeedyCompositeKeyPatchTest}; this class
/// exercises the generated {@link OrderApi} client.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyCompositeKeyTest {

    ApiClient defaultClient;
    OrderApi apiInstance;

    @Autowired
    private MockMvc mvc;

    /// Keys created during a test, deleted afterwards so the shared H2 context stays clean.
    private final List<OrderKey> createdKeys = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
        apiInstance = new OrderApi(defaultClient);
    }

    @AfterEach
    void cleanup() {
        for (OrderKey key : createdKeys) {
            try {
                apiInstance.bulkDeleteOrder(List.of(key));
            } catch (RuntimeException ignored) {
                // already deleted by the test itself — fine
            }
        }
        createdKeys.clear();
    }

    /* -------------------------------------------------------------------- */
    /* Helpers                                                              */
    /* -------------------------------------------------------------------- */

    private OrderKey createOrder(String productId, String supplierId, double price, double discount) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(productId);
        request.setSupplierId(supplierId);
        request.setOrderDate(Instant.now().toString());
        request.setPrice(price);
        request.setDiscount(discount);

        BulkCreateOrderResponse response = apiInstance.bulkCreateOrder(List.of(request));
        Assertions.assertNotNull(response);
        List<OrderKey> payload = response.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        OrderKey key = payload.get(0);
        createdKeys.add(key);
        return key;
    }

    private OrderKey key(String productId, String supplierId) {
        OrderKey key = new OrderKey();
        key.setProductId(productId);
        key.setSupplierId(supplierId);
        return key;
    }

    /* -------------------------------------------------------------------- */
    /* CRUD — one operation per test                                        */
    /* -------------------------------------------------------------------- */

    @Test
    void create_returnsCompositeKey() {
        OrderKey key = createOrder("1", "1", 90.9, 10.0);
        Assertions.assertEquals("1", key.getProductId());
        Assertions.assertEquals("1", key.getSupplierId());
    }

    @Test
    void getByKey_returnsCreatedRow() {
        createOrder("2", "1", 90.9, 10.0);

        List<Order> payload = apiInstance.getOrder("2", "1").getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        Order order = payload.get(0);
        Assertions.assertEquals("2", order.getProductId());
        Assertions.assertEquals("1", order.getSupplierId());
        Assertions.assertEquals(90.9, order.getPrice());
        Assertions.assertEquals(10.0, order.getDiscount());
    }

    @Test
    void query_byBothKeyColumns_returnsRow() {
        createOrder("3", "1", 90.9, 10.0);

        FilteredOrderResponse response = apiInstance.queryOrder(
                SpeedyQuery.from()
                        .where(
                                condition("productId", eq("3")),
                                condition("supplierId", eq("1"))
                        )
                        .build()
        );

        List<Order> orders = response.getPayload();
        Assertions.assertEquals(1, orders.size());
        Assertions.assertEquals("3", orders.get(0).getProductId());
        Assertions.assertEquals("1", orders.get(0).getSupplierId());
    }

    @Test
    void put_fullReplace_updatesRow() {
        OrderKey key = createOrder("4", "1", 90.9, 10.0);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setProductId(key.getProductId());
        request.setSupplierId(key.getSupplierId());
        request.setDiscount(100.0);

        UpdateOrderResponse response = apiInstance.updateOrder(List.of(request));
        List<Order> payload = response.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        Assertions.assertEquals(100.0, payload.get(0).getDiscount());

        Order reread = apiInstance.getOrder("4", "1").getPayload().get(0);
        Assertions.assertEquals(100.0, reread.getDiscount());
    }

    @Test
    void delete_removesRow() {
        OrderKey key = createOrder("5", "1", 90.9, 10.0);

        BulkDeleteOrderResponse response = apiInstance.bulkDeleteOrder(List.of(key));
        Assertions.assertNotNull(response);
        List<OrderKey> payload = response.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        Assertions.assertEquals("5", payload.get(0).getProductId());
        Assertions.assertEquals("1", payload.get(0).getSupplierId());

        List<Order> after = apiInstance.getOrder("5", "1").getPayload();
        Assertions.assertTrue(after == null || after.isEmpty(), "row should be gone after delete");
    }

    @Test
    void duplicateCreate_throws() {
        createOrder("6", "1", 90.9, 10.0);
        Assertions.assertThrows(RuntimeException.class, () -> createOrder("6", "1", 90.9, 10.0));
    }

    /* -------------------------------------------------------------------- */
    /* Negative — addressing a composite key that does not exist            */
    /* -------------------------------------------------------------------- */

    @Test
    void getByKey_nonExistent_returnsEmpty() {
        // (7,7) is a valid FK pair but no such order was ever created.
        List<Order> payload = apiInstance.getOrder("7", "7").getPayload();
        Assertions.assertTrue(payload == null || payload.isEmpty());
    }

    @Test
    void delete_nonExistentKey_returns404() {
        // Unlike GET-by-key (which returns an empty payload), deleting a row that does not exist
        // addresses a specific record and fails with 404 "entity not found".
        HttpClientErrorException ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> apiInstance.bulkDeleteOrder(List.of(key("7", "6"))));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /* -------------------------------------------------------------------- */
    /* Filtering — beyond eq-on-both-key-columns                            */
    /* -------------------------------------------------------------------- */

    @Test
    void query_bySingleKeyColumn_returnsMatches() {
        createOrder("5", "2", 10.0, 1.0);
        createOrder("5", "3", 20.0, 2.0);

        FilteredOrderResponse response = apiInstance.queryOrder(
                SpeedyQuery.from()
                        .where(condition("productId", eq("5")))
                        .build()
        );

        List<Order> orders = response.getPayload();
        Assertions.assertTrue(orders.stream().anyMatch(o -> "2".equals(o.getSupplierId())));
        Assertions.assertTrue(orders.stream().anyMatch(o -> "3".equals(o.getSupplierId())));
        Assertions.assertTrue(orders.stream().allMatch(o -> "5".equals(o.getProductId())));
    }

    @Test
    void query_byNonKeyField_returnsMatches() {
        createOrder("6", "2", 55.5, 42.0); // 42.0 discount is distinctive

        FilteredOrderResponse response = apiInstance.queryOrder(
                SpeedyQuery.from()
                        .where(condition("discount", eq(42.0)))
                        .build()
        );

        List<Order> orders = response.getPayload();
        Assertions.assertFalse(orders.isEmpty());
        Assertions.assertTrue(orders.stream()
                .anyMatch(o -> "6".equals(o.getProductId()) && "2".equals(o.getSupplierId())));
        Assertions.assertTrue(orders.stream().allMatch(o -> o.getDiscount() != null && o.getDiscount() == 42.0));
    }

    @Test
    void query_withInOperator_onKeyColumn_returnsMatches() {
        createOrder("6", "3", 10.0, 3.0);
        createOrder("7", "3", 20.0, 3.0);

        FilteredOrderResponse response = apiInstance.queryOrder(
                SpeedyQuery.from()
                        .where(condition("productId", in("6", "7")))
                        .build()
        );

        List<Order> orders = response.getPayload();
        Assertions.assertTrue(orders.stream()
                .anyMatch(o -> "6".equals(o.getProductId()) && "3".equals(o.getSupplierId())));
        Assertions.assertTrue(orders.stream()
                .anyMatch(o -> "7".equals(o.getProductId()) && "3".equals(o.getSupplierId())));
        Assertions.assertTrue(orders.stream()
                .allMatch(o -> "6".equals(o.getProductId()) || "7".equals(o.getProductId())));
    }
}
