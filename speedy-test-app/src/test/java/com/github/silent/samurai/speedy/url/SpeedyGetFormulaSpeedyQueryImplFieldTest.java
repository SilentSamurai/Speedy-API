package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.InventoryApi;
import org.openapitools.client.model.FilteredInventoryResponse;
import org.openapitools.client.model.Inventory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyGetFormulaSpeedyQueryImplFieldTest {

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
    void getViaPrimaryKey() {
        InventoryApi inventoryApi = new InventoryApi(defaultClient);
        FilteredInventoryResponse someInventory = inventoryApi.queryInventory(
                SpeedyQuery.from().build()
        );
        List<Inventory> payload = someInventory.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
//        Assertions.assertNotNull(inventory.getProfit());
    }


}