package com.github.silent.samurai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.service.CategoryRepository;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.CompanyApi;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyEntityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyEntityTest.class);

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
    void normal() throws Exception {

        Instant datetime = Instant.now();
        CreateCompanyRequest createCompanyRequest = new CreateCompanyRequest();
        createCompanyRequest.name("New Company")
                .address("Address")
                .defaultGenerator(12)
                .extra("extra asp")
                .createdAt(datetime)
                .deletedAt(datetime)
                .invoiceNo(12)
                .currency("INR")
                .phone("0987383762")
                .detailsTop("asd")
                .email("poasdnfi@asd.com");

        CompanyApi companyApi = new CompanyApi(defaultClient);
        BulkCreateCompany200Response bulkCreateCompany200Response = companyApi.bulkCreateCompany(Lists.newArrayList(createCompanyRequest));

        CompanyKey companyKey = bulkCreateCompany200Response.getPayload().get(0);


        GetCompany200Response company200Response = companyApi.getCompany(String.format("id = '%s'", companyKey.getId()));
        Company company = company200Response.getPayload();

        LOGGER.info("company {}", company);


    }


}


