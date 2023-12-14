package com.github.silent.samurai.speedy;

import com.google.common.collect.Sets;
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

import javax.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;

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

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost < 50)");

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() < 50);
        }

    }

    @Test
    void lessThanEquals() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost <= 75)");

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() <= 75);
        }

    }

    @Test
    void greaterThan() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost > 75)");

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() > 75);
        }

    }

    @Test
    void greaterThanEquals() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost >= 75)");

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() >= 75);
        }

    }

    @Test
    void notEquals() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost != 75)");

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() != 75);
        }

    }

    @Test
    void equals() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost = 75)");
        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() == 75);
        }
    }

    @Test
    void multiple1() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost < 75 & cost >= 25)");
        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() < 75 && inventory.getCost() >= 25);
        }
    }


//    @Ignore
//    @Test
//    void multiple2() throws Exception {
//        InventoryApi inventoryApi = new InventoryApi(defaultClient);
//
//        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost < 25 | cost > 75)");
//        List<LightInventory> payload = someInventory.getPayload();
//        Assertions.assertNotNull(payload);
//        Assertions.assertFalse(payload.isEmpty());
//        for (LightInventory inventory : payload) {
//            Assertions.assertNotNull(inventory.getCost());
//            Assertions.assertTrue(inventory.getCost() > 75 || inventory.getCost() < 25);
//        }
//    }


//    @Ignore
//    @Test
//    void multiple3() throws Exception {
//        InventoryApi inventoryApi = new InventoryApi(defaultClient);
//
//        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost > 75 & cost < 25 | cost > 45 & cost < 60 )");
//        List<LightInventory> payload = someInventory.getPayload();
//        Assertions.assertNotNull(payload);
//        Assertions.assertFalse(payload.isEmpty());
//        for (LightInventory inventory : payload) {
//            Assertions.assertNotNull(inventory.getCost());
//            Assertions.assertTrue(inventory.getCost() > 75 && inventory.getCost() < 25 || inventory.getCost() > 45 && inventory.getCost() < 60);
//        }
//    }

    @Test
    void in() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost <> [ 25, 50, 75])");

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        Assertions.assertEquals(3, payload.size());
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            HashSet<Double> integers = Sets.newHashSet(25.0, 50.0, 75.0);
            Assertions.assertTrue(integers.contains(inventory.getCost()));
        }

    }

    @Test
    void notin() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory("(cost <!> [ 25, 50, 75])");

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            HashSet<Double> integers = Sets.newHashSet(25.0, 50.0, 75.0);
            Assertions.assertFalse(integers.contains(inventory.getCost()));
        }

    }


    @Test
    void associationTest() throws Exception {

        FilteredProductResponse someInventory = productApi.getSomeProduct(" (category.id = '1') ");

        List<LightProduct> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightProduct product : payload) {
            Assertions.assertNotNull(product.getId());
        }

    }

    @Test
    void association1Test() throws Exception {

        FilteredProductResponse someInventory = productApi.getSomeProduct("( name = 'Product 1' & category.id = '1')");

        List<LightProduct> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightProduct product : payload) {
            Assertions.assertNotNull(product.getId());
        }

    }

    @Test
    void dateTest() throws Exception {

        ProcurementApi procurementApi = new ProcurementApi(defaultClient);

        FilteredProcurementResponse someInventory = procurementApi.getSomeProcurement("( purchaseDate < '2023-05-11T18:13:38.626Z' )");

        List<LightProcurement> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightProcurement inventory : payload) {
            Assertions.assertNotNull(inventory.getPurchaseDate());
            Instant purchaseDate = LocalDateTime.parse(inventory.getPurchaseDate()).atZone(ZoneId.of("UTC")).toInstant();
            Assertions.assertTrue(purchaseDate.isBefore(Instant.parse("2023-05-11T18:13:38.626Z")));
        }

    }


}