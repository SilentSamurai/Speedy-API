package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.VirtualEntityApi;
import org.openapitools.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyVirtualEntityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyVirtualEntityTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;
    ApiClient defaultClient;
    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
    }


    VirtualEntityKey createVirtualEntity() throws Exception {
        VirtualEntityApi apiInstance = new VirtualEntityApi(defaultClient);
        CreateVirtualEntityRequest createVirtualEntityRequest = new CreateVirtualEntityRequest();
        createVirtualEntityRequest.name("ABCD");
        createVirtualEntityRequest.description("desc");
        BulkCreateVirtualEntityResponse createResponse = apiInstance.bulkCreateVirtualEntity(Arrays.asList(createVirtualEntityRequest));
        List<VirtualEntityKey> payload = createResponse.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        VirtualEntityKey virtualEntityKey = payload.get(0);
        Assertions.assertNotNull(virtualEntityKey);
        Assertions.assertNotNull(virtualEntityKey.getId());
        Assertions.assertNotEquals("", virtualEntityKey.getId());
        return virtualEntityKey;
    }

    VirtualEntity getVirtualEntity(VirtualEntityKey virtualEntityKey) throws Exception {
        VirtualEntityApi virtualEntityApi = new VirtualEntityApi(defaultClient);

        List<VirtualEntity> payload = virtualEntityApi
                .getVirtualEntity(virtualEntityKey.getId()).getPayload();

        Assertions.assertNotNull(payload);
        VirtualEntity virtualEntity = payload.get(0);
        Assertions.assertNotNull(virtualEntity.getId());
        Assertions.assertNotEquals("", virtualEntity.getId());

        Assertions.assertNotEquals("Product1", virtualEntity.getName());
        return virtualEntity;
    }


    void updateVirtualEntity(VirtualEntityKey virtualEntityKey) throws Exception {
        VirtualEntityApi virtualEntityApi = new VirtualEntityApi(defaultClient);

        UpdateVirtualEntityRequest entityRequest = new UpdateVirtualEntityRequest();
        entityRequest.description("Updated Description");
        entityRequest.setId(virtualEntityKey.getId());

        UpdateVirtualEntityResponse response = virtualEntityApi.updateVirtualEntity(entityRequest);
        Assertions.assertNotNull(response.getPayload());
        VirtualEntity payload = response.getPayload();

        Assertions.assertEquals("Updated Description", payload.getDescription());
    }

    void deleteVirtualEntity(VirtualEntityKey virtualEntityKey) throws Exception {
        VirtualEntityApi virtualEntityApi = new VirtualEntityApi(defaultClient);

        BulkDeleteVirtualEntityResponse response = virtualEntityApi.bulkDeleteVirtualEntity(
                List.of(virtualEntityKey)
        );
        Assertions.assertNotNull(response);
        List<VirtualEntityKey> payload = response.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        VirtualEntityKey virtualEntityKey1 = payload.get(0);
        Assertions.assertNotNull(virtualEntityKey1);

        Assertions.assertEquals(virtualEntityKey1.getId(), virtualEntityKey.getId());
    }

    @Test
    void normalTest() throws Exception {
        VirtualEntityKey virtualEntityKey = createVirtualEntity();

        VirtualEntity virtualEntity = getVirtualEntity(virtualEntityKey);

        updateVirtualEntity(virtualEntityKey);

        virtualEntity = getVirtualEntity(virtualEntityKey);

        Assertions.assertEquals("Updated Description", virtualEntity.getDescription());

        deleteVirtualEntity(virtualEntityKey);
    }


}


