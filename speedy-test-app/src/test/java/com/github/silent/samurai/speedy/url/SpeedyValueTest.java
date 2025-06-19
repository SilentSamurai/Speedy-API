package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.repositories.ValueTestRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.ValueTestEntityApi;
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
import java.time.*;
import java.util.List;
import java.util.Optional;

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

        ZonedDateTime asiaTokyoTime = ZonedDateTime.parse("2021-01-01T00:00+09:00");
        LocalDateTime localDateTime = LocalDateTime.parse("2021-01-01T00:00:00");
        LocalDate localDate = LocalDate.parse("2021-01-01");
        LocalTime localTime = LocalTime.parse("00:00:00");
        Instant instantDateTime = Instant.parse("2021-01-01T00:00:00Z");

        CreateValueTestEntityRequest createValueTestEntityRequest = new CreateValueTestEntityRequest();
        createValueTestEntityRequest.setLocalDateTime("2021-01-01T00:00:00");
        createValueTestEntityRequest.setLocalDate("2021-01-01");
        createValueTestEntityRequest.setLocalTime("00:00:00");
        createValueTestEntityRequest.setInstantTime("2021-01-01T00:00:00Z");
        createValueTestEntityRequest.setZonedDateTime("2021-01-01T00:00+09:00");

        BulkCreateValueTestEntityResponse bulkCreateValueTestEntityResponse = apiInstance
                .bulkCreateValueTestEntity(List.of(createValueTestEntityRequest));

        List<ValueTestEntityKey> payload = bulkCreateValueTestEntityResponse.getPayload();
        Assertions.assertEquals(1, payload.size());
        ValueTestEntityKey valueTestEntityKey = payload.get(0);


        Assertions.assertNotNull(valueTestEntityKey.getId());
        Optional<com.github.silent.samurai.speedy.entity.ValueTestEntity> optionalValueTestEntity =
                valueTestRepository.findById(valueTestEntityKey.getId());

        Assertions.assertTrue(optionalValueTestEntity.isPresent());
        com.github.silent.samurai.speedy.entity.ValueTestEntity jpaEntity = optionalValueTestEntity.get();

        LOGGER.info("jpaEntity from db {}", jpaEntity);
        Assertions.assertNotNull(jpaEntity.getLocalDateTime());
        Assertions.assertEquals(localDateTime, jpaEntity.getLocalDateTime());
        Assertions.assertNotNull(jpaEntity.getLocalDate());
        Assertions.assertEquals(localDate, jpaEntity.getLocalDate());
        Assertions.assertNotNull(jpaEntity.getLocalTime());
        Assertions.assertEquals(localTime, jpaEntity.getLocalTime());
        Assertions.assertNotNull(jpaEntity.getInstantTime());
        Assertions.assertEquals(instantDateTime, jpaEntity.getInstantTime());
        Assertions.assertNotNull(jpaEntity.getZonedDateTime());
        Assertions.assertEquals(asiaTokyoTime, jpaEntity.getZonedDateTime().withZoneSameInstant(asiaTokyoTime.getZone()));


        FilteredValueTestEntityResponse filteredValueTestEntityResponse = apiInstance.getValueTestEntity(valueTestEntityKey.getId());

        List<ValueTestEntity> valueTestEntities = filteredValueTestEntityResponse.getPayload();


        assert valueTestEntities != null;
        Assertions.assertEquals(1, valueTestEntities.size());

        ValueTestEntity valueTestEntity2 = valueTestEntities.get(0);

        LOGGER.info("jpaEntity {}", valueTestEntity2);

        Assertions.assertEquals("2021-01-01T00:00:00", valueTestEntity2.getLocalDateTime());
        Assertions.assertEquals("2021-01-01", valueTestEntity2.getLocalDate());
        Assertions.assertEquals("00:00:00", valueTestEntity2.getLocalTime());

        Assertions.assertNotNull(valueTestEntity2.getInstantTime());
        Assertions.assertEquals(
                Instant.parse("2021-01-01T00:00:00Z").atZone(ZoneOffset.UTC),
                Instant.parse(valueTestEntity2.getInstantTime()).atZone(ZoneOffset.UTC));
        Assertions.assertNotNull(valueTestEntity2.getZonedDateTime());
        ZonedDateTime szoneddatetime = ZonedDateTime.parse(valueTestEntity2.getZonedDateTime());
        Assertions.assertEquals(asiaTokyoTime, szoneddatetime.withZoneSameInstant(asiaTokyoTime.getZone()));


    }
}

