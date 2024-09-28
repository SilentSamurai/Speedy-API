package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyRequest;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.OrderApi;
import org.openapitools.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.$condition;
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.$eq;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyCompositeKeyTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyCompositeKeyTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;
    ApiClient defaultClient;
    OrderApi apiInstance;
    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
        apiInstance = new OrderApi(defaultClient);
    }


    OrderKey createOrder() throws Exception {

        CreateOrderRequest createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setProductId("1");
        createOrderRequest.setSupplierId("1");
        createOrderRequest.setOrderDate(Instant.now().toString());
        createOrderRequest.setDiscount(10.0);
        createOrderRequest.setPrice(90.9);
        BulkCreateOrderResponse bulkCreateOrderResponse = apiInstance
                .bulkCreateOrder(List.of(createOrderRequest));
        Assertions.assertNotNull(bulkCreateOrderResponse);
        List<OrderKey> payload = bulkCreateOrderResponse.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        OrderKey orderKey = payload.get(0);
        Assertions.assertNotNull(orderKey);

        Assertions.assertNotNull(orderKey.getProductId());
        Assertions.assertNotEquals("", orderKey.getProductId());

        Assertions.assertNotNull(orderKey.getSupplierId());
        Assertions.assertNotEquals("", orderKey.getSupplierId());

        return orderKey;
    }

    Order getOrder(OrderKey orderKey) throws Exception {

//        String query = String.format("(productId=\"%s\",supplierId=\"%s\")", orderKey.getProductId(), orderKey.getSupplierId());

        FilteredOrderResponse orderResponse = apiInstance.queryOrder(
                SpeedyRequest.query()
                        .$where(
                                $condition("productId", $eq(orderKey.getProductId())),
                                $condition("supplierId", $eq(orderKey.getSupplierId()))
                        )
                        .build()
        );

        Assertions.assertNotNull(orderResponse);
        List<Order> orderList = orderResponse.getPayload();
        Assertions.assertNotNull(orderList);
        Order order = orderList.get(0);
        Assertions.assertNotNull(order.getProductId());
        Assertions.assertNotEquals("", order.getProductId());
        Assertions.assertEquals("1", order.getProductId());

        Assertions.assertNotNull(order.getSupplierId());
        Assertions.assertNotEquals("", order.getSupplierId());
        Assertions.assertEquals("1", order.getProductId());

        Assertions.assertEquals(10.0, order.getDiscount());
        Assertions.assertEquals(90.9, order.getPrice());
        return order;
    }

    void updateOrder(OrderKey orderKey) throws Exception {

        UpdateOrderRequest updateOrderRequest = new UpdateOrderRequest();
        updateOrderRequest.setDiscount(100.0);
        UpdateOrderResponse response = apiInstance.updateOrder(orderKey.getProductId(), orderKey.getSupplierId(), updateOrderRequest);

        Order payload = response.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertEquals(100.0, payload.getDiscount());

        List<Order> payload1 = apiInstance.getOrder(orderKey.getProductId(), orderKey.getSupplierId()).getPayload();

        Assertions.assertNotNull(payload1);
        Order lightOrder = payload1.get(0);
        Assertions.assertNotNull(lightOrder);
        Assertions.assertEquals(100.0, lightOrder.getDiscount());
    }

    void deleteOrder(OrderKey orderKey) throws Exception {

        BulkDeleteOrderResponse response = apiInstance.bulkDeleteOrder(
                List.of(orderKey)
        );
        Assertions.assertNotNull(response);
        List<OrderKey> payload = response.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        OrderKey orderKey1 = payload.get(0);
        Assertions.assertNotNull(orderKey1);

        Assertions.assertEquals(orderKey1.getSupplierId(), orderKey.getSupplierId());
        Assertions.assertEquals(orderKey1.getProductId(), orderKey.getProductId());
    }

    @Test
    void test() throws Exception {

        OrderKey orderKey = createOrder();
        Order order = getOrder(orderKey);

        updateOrder(orderKey);

        deleteOrder(orderKey);

    }

    @Test
    void duplicateCreate() throws Exception {
        OrderKey orderKey = createOrder();
        Assertions.assertThrows(RuntimeException.class, () -> {
            // Code that should throw a RuntimeException
            createOrder();
        });
    }

}


