package com.github.silent.samurai.speedy.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
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
class TransactionModeConfigTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedy;

    @BeforeEach
    void setUp() {
        speedy = SpeedyTest.mockMvc(mvc);
    }

    private List<ObjectNode> createCurrencyNodes(int count) {
        List<ObjectNode> list = new ArrayList<>();
        long ts = System.currentTimeMillis() & 0xFFFF;
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < count; i++) {
            ObjectNode c = mapper.createObjectNode();
            c.put("currencyName", "Cfg-" + ts + "-" + i);
            c.put("currencySymbol", "C" + i);
            c.put("currencyAbbr", "A" + ts + i);
            list.add(c);
        }
        return list;
    }

    @Test
    void entityLevelConfig_batchBehaviorWithoutFlag() throws Exception {
        List<ObjectNode> currencies = createCurrencyNodes(3);
        currencies.get(1).put("currencyName", "");

        speedy.createMany("Currency", currencies)
                .expectBadRequest();
    }

    @Test
    void defaultPerEntity_usesPerEntityMode() throws Exception {
        List<ObjectNode> suppliers = new ArrayList<>();
        long ts = System.currentTimeMillis() & 0xFFFF;
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode ok = mapper.createObjectNode();
        ok.put("name", "DefTest-" + ts + "-ok");
        ok.put("phoneNo", "1-" + ts + "-a");
        ok.put("altPhoneNo", "2-" + ts + "-a");
        suppliers.add(ok);

        ObjectNode bad = mapper.createObjectNode();
        bad.put("name", "");
        bad.put("phoneNo", "3-" + ts + "-b");
        bad.put("altPhoneNo", "4-" + ts + "-b");
        suppliers.add(bad);

        speedy.createMany("Supplier", suppliers)
                .expectStatus(207);
    }

    @Test
    void upgradeOverride_perEntityToBatch() throws Exception {
        List<ObjectNode> suppliers = new ArrayList<>();
        long ts = System.currentTimeMillis() & 0xFFFF;
        ObjectMapper mapper = new ObjectMapper();

        for (int i = 0; i < 3; i++) {
            ObjectNode s = mapper.createObjectNode();
            s.put("name", "Upgrade-" + ts + "-" + i);
            s.put("phoneNo", "5-" + ts + "-" + i);
            s.put("altPhoneNo", "6-" + ts + "-" + i);
            suppliers.add(s);
        }

        speedy.createMany("Supplier").items(suppliers).transaction("batch").execute()
                .expectOk();
    }

    @Test
    void downgradeRejection_batchToPerEntity() throws Exception {
        List<ObjectNode> currencies = createCurrencyNodes(2);

        speedy.createMany("Currency").items(currencies).transaction("per-entity").execute()
                .expectBadRequest();
    }

    @Test
    void invalidTransactionValue() throws Exception {
        List<ObjectNode> currencies = createCurrencyNodes(2);

        speedy.createMany("Currency").items(currencies).transaction("invalid").execute()
                .expectBadRequest();
    }
}
