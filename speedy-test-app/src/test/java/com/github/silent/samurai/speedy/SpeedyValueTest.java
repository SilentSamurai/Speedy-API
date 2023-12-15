package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.repositories.ValueTestRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.ValueTestEntityApi;
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

import javax.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyValueTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyValueTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    ValueTestRepository valueTestRepository;
    ApiClient defaultClient;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
    }


    @Test
    void normalTest() throws Exception {
        ValueTestEntityApi apiInstance = new ValueTestEntityApi(defaultClient);

        CreateValueTestEntityRequest createValueTestEntityRequest = new CreateValueTestEntityRequest();
        createValueTestEntityRequest.setLocalDateTime("2021-01-01T00:00:00");
        createValueTestEntityRequest.setLocalDate("2021-01-01");
        createValueTestEntityRequest.setLocalTime("00:00:00");
        createValueTestEntityRequest.setInstantTime("2021-01-01T00:00:00Z");
        createValueTestEntityRequest.setZonedDateTime("2021-01-01T00:00:00+09:00[Asia/Tokyo]");

        BulkCreateValueTestEntityResponse bulkCreateValueTestEntityResponse = apiInstance
                .bulkCreateValueTestEntity(List.of(createValueTestEntityRequest));

        List<ValueTestEntityKey> payload = bulkCreateValueTestEntityResponse.getPayload();
        Assertions.assertEquals(1, payload.size());
        ValueTestEntityKey valueTestEntityKey = payload.get(0);


        valueTestRepository.findById(valueTestEntityKey.getId()).ifPresent(valueTestEntity -> {
            LOGGER.info("valueTestEntity from db {}", valueTestEntity);
            Assertions.assertNotNull(valueTestEntity.getLocalDateTime());
            Assertions.assertEquals("2021-01-01T00:00", valueTestEntity.getLocalDateTime().toString());
            Assertions.assertNotNull(valueTestEntity.getLocalDate());
            Assertions.assertEquals("2021-01-01", valueTestEntity.getLocalDate().toString());
            Assertions.assertNotNull(valueTestEntity.getLocalTime());
            Assertions.assertEquals("00:00", valueTestEntity.getLocalTime().toString());
            Assertions.assertNotNull(valueTestEntity.getInstantTime());
            Assertions.assertEquals("2021-01-01T00:00:00Z", valueTestEntity.getInstantTime().toString());
            Assertions.assertNotNull(valueTestEntity.getZonedDateTime());
            Assertions.assertEquals("2021-01-01T00:00+09:00[Asia/Tokyo]", valueTestEntity.getZonedDateTime().toString());
        });


        FilteredValueTestEntityResponse filteredValueTestEntityResponse = apiInstance.getValueTestEntity(valueTestEntityKey.getId());

        List<ValueTestEntity> valueTestEntities = filteredValueTestEntityResponse.getPayload();


        Assertions.assertEquals(1, valueTestEntities.size());

        ValueTestEntity valueTestEntity = valueTestEntities.get(0);

        LOGGER.info("valueTestEntity {}", valueTestEntity);

        Assertions.assertEquals("2021-01-01T00:00:00", valueTestEntity.getLocalDateTime());
        Assertions.assertEquals("2021-01-01", valueTestEntity.getLocalDate());
        Assertions.assertEquals("00:00:00", valueTestEntity.getLocalTime());
        Assertions.assertEquals("2021-01-01T00:00:00Z", valueTestEntity.getInstantTime());
        Assertions.assertEquals("2021-01-01T00:00:00+09:00[Asia/Tokyo]", valueTestEntity.getZonedDateTime());


    }
}

