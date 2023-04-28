package com.github.silent.samurai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.InventoryApi;
import org.openapitools.client.model.GetAllInventory200Response;
import org.openapitools.client.model.LightInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManagerFactory;
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
    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
//        defaultClient.setDebugging(true);
    }

    @Test
    void lessThanCondition() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost<50");

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

        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost <= 75");

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

        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost > 75");

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

        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost >= 75");

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

        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost != 75");

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
        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost = 75");
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
        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost < 75 & cost >= 25");
        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() < 75 && inventory.getCost() >= 25);
        }
    }

    @Test
    @Disabled
    void multiple2() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost < 25 | cost > 75");
        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() > 75 && inventory.getCost() < 25);
        }
    }

    @Test
    @Disabled
    void multiple3() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost > 75 & cost < 25 | cost > 45 & cost < 60 ");
        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() > 75 && inventory.getCost() < 25 && inventory.getCost() > 45 && inventory.getCost() < 60);
        }
    }

    @Test
    @Disabled
    void in() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost <> [ 25, 50, 75]");

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() == 75);
        }

    }

    @Test
    @Disabled
    void notin() throws Exception {

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        GetAllInventory200Response someInventory = inventoryApi.getSomeInventory("cost <!> [ 25, 50, 75]");

        List<LightInventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        for (LightInventory inventory : payload) {
            Assertions.assertNotNull(inventory.getCost());
            Assertions.assertTrue(inventory.getCost() == 75);
        }

    }


}