package com.github.silent.samurai.speedy.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.events.EntityEvents;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class BatchTransactionTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mvc);
        EntityEvents.throwOnNextCurrencyInsert.set(false);
        EntityEvents.throwOnNextCurrencyDelete.set(false);
    }

    private List<ObjectNode> createCurrencyNodes(int count) {
        List<ObjectNode> list = new ArrayList<>();
        long ts = System.currentTimeMillis() & 0xFFFF;
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < count; i++) {
            ObjectNode c = mapper.createObjectNode();
            c.put("currencyName", "Batch-" + ts + "-" + i);
            c.put("currencySymbol", "B" + i);
            c.put("currencyAbbr", "A" + ts + i);
            list.add(c);
        }
        return list;
    }

    @Test
    void batchAllOrNothing_validationFailure() throws Exception {
        List<ObjectNode> currencies = createCurrencyNodes(5);
        currencies.get(2).put("currencyName", "");

        client.createMany("Currency", currencies)
                .expectBadRequest();
    }

    @Test
    void batchAllOrNothing_eventHandlerFailure() throws Exception {
        List<ObjectNode> currencies = createCurrencyNodes(5);
        EntityEvents.throwOnNextCurrencyInsert.set(true);

        client.createMany("Currency", currencies)
                .expectStatus(500);
    }

    @Test
    void batchDeleteAtomicity() throws Exception {
        List<ObjectNode> currencies = createCurrencyNodes(3);

        var createResult = client.createMany("Currency", currencies)
                .expectOk();
        var idsArray = CommonUtil.json().readTree(createResult.responseBody()).get("payload");
        List<ObjectNode> deletePks = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (var node : idsArray) {
            ObjectNode pk = mapper.createObjectNode();
            pk.put("id", node.get("id").asText());
            deletePks.add(pk);
        }

        EntityEvents.throwOnNextCurrencyDelete.set(true);

        client.deleteMany("Currency").items(deletePks).execute()
                .expectStatus(500);
    }

    @Test
    void batchAllSuccess_returns200() throws Exception {
        List<ObjectNode> currencies = createCurrencyNodes(3);

        client.createMany("Currency", currencies)
                .expectOk();
    }
}
