package com.github.silent.samurai.speedy.url;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.InventoryApi;
import org.openapitools.client.api.ProcurementApi;
import org.openapitools.client.api.ProductApi;
import org.openapitools.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyGetConditionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyGetConditionTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;
    ApiClient defaultClient;
    ProductApi productApi;
    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
        productApi = new ProductApi(defaultClient);
//        defaultClient.setDebugging(true);
    }

    @Test
    void lessThanCondition() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);
//        "(cost < 50)"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from().where(condition("cost", lt(50))).build()
        );

        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() < 50);
        }

    }

    @Test
    void lessThanEquals() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost <= 75)"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from().where(
                        condition("cost", lte(75))
                ).build()
        );

        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() <= 75);
        }

    }

    @Test
    void greaterThan() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost > 75)"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from()
                        .where(condition("cost", gt(75)))
                        .build()
        );

        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() > 75);
        }

    }

    @Test
    void greaterThanEquals() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost >= 75)"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from()
                        .where(condition("cost", gte(75)))
                        .build()
        );

        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(!payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() >= 75);
        }

    }

    @Test
    void notEquals() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        //"(cost != 75)"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from()
                        .where(condition("cost", ne(75)))
                        .build()
        );

        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() != 75);
        }

    }

    @Test
    void equals() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost = 75)"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from()
                        .where(condition("cost", eq(75)))
                        .build()
        );
        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertEquals(75, (double) inventory.getCost());
        }
    }

    @Test
    void multiple1() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost < 75 & cost >= 25)"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from()
                        .where(and(condition("cost", lt(75)), condition("cost", gte(25))))
                        .prettyPrint()
                        .build()
        );
        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() < 75 && inventory.getCost() >= 25);
        }
    }


    @Test
    void multiple2() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost < 25 | cost > 75)"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from()
                        .where(
                                or(
                                        condition("cost", lt(25)),
                                        condition("cost", gt(75))
                                )
                        )
                        .prettyPrint()
                        .build()
        );
        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() > 75 || inventory.getCost() < 25);
        }
    }


    @Test
    void multiple3() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        // "(cost > 75 & cost < 25 | cost > 45 & cost < 60)"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from()
                        .where(
                                or(
                                        and(
                                                condition("cost", gt(75)),
                                                condition("cost", lt(25))
                                        ),
                                        and(
                                                condition("cost", gt(45)),
                                                condition("cost", lt(60))
                                        )
                                )
                        ).prettyPrint()
                        .build()
        );
        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() > 75 && inventory.getCost() < 25 || inventory.getCost() > 45 && inventory.getCost() < 60);
        }
    }

    @Test
    void in() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost <> [ 25, 50, 75])"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from().where(condition("cost", SpeedyQuery.in(25, 50, 75))).build());
        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        Assertions.assertEquals(3, payload.size());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Set<Double> integers = Set.of(25.0, 50.0, 75.0);
            Assertions.assertTrue(integers.contains(inventory.getCost()));
        }
    }

    @Test
    void in_single_value() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.putObject("$where").putObject("cost").put("$in", 50);

        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost <> [ 25, 50, 75])"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(body);
        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        Assertions.assertEquals(1, payload.size());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Set<Double> integers = Set.of(50.0);
            Assertions.assertTrue(integers.contains(inventory.getCost()));
        }
    }

    @Test
    void notin_single_value() throws Exception {

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.putObject("$where")
                .putObject("cost")
                .put("$nin", 50);

        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost <!> [ 25, 50, 75])"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(body);

        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Set<Double> integers = Set.of(50.0);
            Assertions.assertFalse(integers.contains(inventory.getCost()));
        }

    }

    @Test
    void notin() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        // "(cost <!> [ 25, 50, 75])"
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from()
                        .where(
                                condition("cost", nin(25, 50, 75))
                        ).build()
        );

        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Inventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Set<Double> integers = Set.of(25.0, 50.0, 75.0);
            Assertions.assertFalse(integers.contains(inventory.getCost()));
        }

    }


    @Test
    void associationTest() throws Exception {

        // " (category.id = '1') "
        FilteredProductResponse someInventory = productApi.queryProduct(
                SpeedyQuery.from()
                        .where(condition("category.id", eq("1")))
                        .build()
        );

        List<Product> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Product product : payload) {
            Assertions.assertNotNull(product.getId());
        }

    }

    @Test
    void association1Test() throws Exception {

        // "( name = 'Product 1' & category.id = '1')"
        FilteredProductResponse someInventory = productApi.queryProduct(
                SpeedyQuery.from()
                        .where(
                                condition("name", eq("Product 1")),
                                condition("category.id", eq("1")))
                        .build()
        );

        List<Product> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Product product : payload) {
            Assertions.assertNotNull(product.getId());
        }

    }

    @Test
    void dateTest() throws Exception {

        ProcurementApi procurementApi = new ProcurementApi(defaultClient);

        // "(purchaseDate < '2023-05-11T18:13:38.626Z' )"
        FilteredProcurementResponse someInventory = procurementApi.queryProcurement(
                SpeedyQuery.from()
                        .where(
                                condition("purchaseDate", lt("2023-05-11T18:13:38.626Z")))
                        .build()
        );

        List<Procurement> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (Procurement inventory : payload) {
            Assertions.assertNotNull(inventory.getPurchaseDate());
            Instant purchaseDate = ZonedDateTime.parse(inventory.getPurchaseDate()).toInstant();
            Assertions.assertTrue(purchaseDate.isBefore(Instant.parse("2023-05-11T18:13:38.626Z")));
        }

    }


}