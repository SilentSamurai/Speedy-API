package com.github.silent.samurai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.interfaces.SpeedyConstant;
import com.github.silent.samurai.service.CategoryRepository;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.SupplierApi;
import org.openapitools.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManagerFactory;
import java.time.Instant;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyDatetimeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyDatetimeTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;

    ApiClient defaultClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
    }

    @Test
    void CreateSupplier() throws Exception {

        Instant dateTimeInstant = Instant.now();

        CreateSupplierRequest createSupplierRequest = new CreateSupplierRequest();
        createSupplierRequest.name("new Supplier")
                .address("ABCD aiohwef")
                .createdAt(dateTimeInstant)
                .createdBy("Happy Singh")
                .email("abcd@smainsda.cs")
                .altPhoneNo("9013019322")
                .phoneNo("9013019322");

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Supplier")
                .content(objectMapper.writeValueAsString(Lists.newArrayList(createSupplierRequest)))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        BulkCreateSupplier200Response apiResponse = objectMapper.readValue(contentAsString, BulkCreateSupplier200Response.class);

        Assertions.assertNotNull(apiResponse);
        Assertions.assertNotNull(apiResponse.getPayload());
        Assertions.assertTrue(apiResponse.getPayload().size() > 0);
        Assertions.assertNotNull(apiResponse.getPayload().get(0));
        SupplierKey supplierKey = apiResponse.getPayload().get(0);

        Assertions.assertNotNull(supplierKey.getId());
        Assertions.assertFalse(supplierKey.getId().isBlank());

        SupplierApi supplierApi = new SupplierApi(defaultClient);

        GetSupplier200Response supplier = supplierApi.getSupplier(String.format("id = '%s' ", supplierKey.getId()));
        Supplier payload = supplier.getPayload();

        LOGGER.info("Supplier {}", payload);
        Assertions.assertNotNull(payload.getCreatedAt());
        Assertions.assertEquals(payload.getCreatedAt(), dateTimeInstant);
    }


}


