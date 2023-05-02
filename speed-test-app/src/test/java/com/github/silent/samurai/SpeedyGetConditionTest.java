package com.github.silent.samurai;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.InventoryApi;
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

    @Autowired
    private MockMvc mvc;

    ApiClient defaultClient;
    ProductApi productApi;

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

        GetInventoryRequest getInventoryRequest = new GetInventoryRequest();
        getInventoryRequest.setWhere("cost<50");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(getInventoryRequest);

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

        GetInventoryRequest getInventoryRequest = new GetInventoryRequest();
        getInventoryRequest.setWhere("cost <= 75");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(getInventoryRequest);

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

        GetInventoryRequest inventoryRequest = new GetInventoryRequest();
        inventoryRequest.setWhere("cost > 75");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(inventoryRequest);

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

        GetInventoryRequest inventoryRequest = new GetInventoryRequest();
        inventoryRequest.setWhere("cost >= 75");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(inventoryRequest);

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

        GetInventoryRequest inventoryRequest = new GetInventoryRequest();
        inventoryRequest.setWhere("cost != 75");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(inventoryRequest);

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

        GetInventoryRequest inventoryRequest = new GetInventoryRequest();
        inventoryRequest.setWhere("cost = 75");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(inventoryRequest);
        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() == 75);
        }
    }

    @Test
    void multiple1() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        GetInventoryRequest inventoryRequest = new GetInventoryRequest();
        inventoryRequest.setWhere("cost < 75 & cost >= 25");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(inventoryRequest);
        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() < 75 && inventory.getCost() >= 25);
        }
    }

    @Test
    void multiple2() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        GetInventoryRequest inventoryRequest = new GetInventoryRequest();
        inventoryRequest.setWhere("cost < 25 | cost > 75");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(inventoryRequest);
        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() > 75 || inventory.getCost() < 25);
        }
    }

    @Test
    void multiple3() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        GetInventoryRequest inventoryRequest = new GetInventoryRequest();
        inventoryRequest.setWhere("cost > 75 & cost < 25 | cost > 45 & cost < 60 ");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(inventoryRequest);
        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() > 75 && inventory.getCost() < 25 || inventory.getCost() > 45 && inventory.getCost() < 60);
        }
    }

    @Test
    void in() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        GetInventoryRequest inventoryRequest = new GetInventoryRequest();
        inventoryRequest.setWhere("cost <> [ 25, 50, 75]");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(inventoryRequest);

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            HashSet<Double> integers = Sets.newHashSet(25.0, 50.0, 75.0);
            Assertions.assertTrue(integers.contains(inventory.getCost()));
        }

    }

    @Test
    void notin() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        GetInventoryRequest inventoryRequest = new GetInventoryRequest();
        inventoryRequest.setWhere("cost <!> [ 25, 50, 75]");

        FilteredInventoryResponse someInventory = inventoryApi.getSomeInventory(inventoryRequest);

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

        GetProductRequest productRequest = new GetProductRequest();
        productRequest.join(new GetProductRequestJoin().category("id='1'"));

        FilteredProductResponse someInventory = productApi.getSomeProduct(productRequest);

        List<LightProduct> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightProduct product : payload) {
            Assertions.assertNotNull(product.getId());
        }

    }

    @Test
    void association1Test() throws Exception {

        GetProductRequest productRequest = new GetProductRequest();
        productRequest.where("name='Product 1'");
        productRequest.join(new GetProductRequestJoin().category("id='1'"));

        FilteredProductResponse someInventory = productApi.getSomeProduct(productRequest);

        List<LightProduct> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightProduct product : payload) {
            Assertions.assertNotNull(product.getId());
        }

    }


}