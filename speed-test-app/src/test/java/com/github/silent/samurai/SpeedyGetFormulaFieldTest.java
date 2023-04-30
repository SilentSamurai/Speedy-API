package com.github.silent.samurai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.InventoryApi;
import org.openapitools.client.model.AllInventoryResponse;
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
class SpeedyGetFormulaFieldTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyGetFormulaFieldTest.class);

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
    void getViaPrimaryKey() throws Exception {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        AllInventoryResponse allInventory = inventoryApi.getAllInventory(null, null, null, null);
        List<LightInventory> payload = allInventory.getPayload();
        LightInventory inventory = payload.get(0);
        Assertions.assertNotNull(inventory.getProfit());
    }


}