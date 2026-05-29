package com.github.silent.samurai.speedy.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class PerEntityTransactionTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mvc);
    }

    private List<ObjectNode> createSupplierNodes(int count, String prefix) {
        List<ObjectNode> list = new ArrayList<>();
        long ts = System.currentTimeMillis();
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < count; i++) {
            ObjectNode s = mapper.createObjectNode();
            s.put("name", prefix + "-" + (ts & 0xFFFFF) + "-" + i);
            s.put("phoneNo", "1-" + (ts & 0xFFF) + "-" + i);
            s.put("altPhoneNo", "2-" + (ts & 0xFFF) + "-" + i);
            list.add(s);
        }
        return list;
    }

    @Test
    void allSucceed_returns200() throws Exception {
        List<ObjectNode> suppliers = createSupplierNodes(5, "AllOk");

        client.createMany("Supplier", suppliers)
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(5));
    }

    @Test
    void allFail_returns400() throws Exception {
        List<ObjectNode> suppliers = new ArrayList<>();
        long ts = System.currentTimeMillis();
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < 3; i++) {
            ObjectNode s = mapper.createObjectNode();
            s.put("name", "");
            s.put("phoneNo", "f-" + (ts & 0xFFF) + "-" + i);
            s.put("altPhoneNo", "a-" + (ts & 0xFFF) + "-" + i);
            suppliers.add(s);
        }

        client.createMany("Supplier", suppliers)
                .expectBadRequest()
                .expectJsonPath("$.succeeded[*]", hasSize(0))
                .expectJsonPath("$.failed[*]", hasSize(3))
                .expectJsonPath("$.pageIndex", equalTo(0));
    }

    @Test
    void partialSuccess_returns207() throws Exception {
        List<ObjectNode> suppliers = createSupplierNodes(5, "Partial");
        suppliers.get(2).put("name", "");

        client.createMany("Supplier", suppliers)
                .expectStatus(207)
                .expectJsonPath("$.succeeded[*]", hasSize(4))
                .expectJsonPath("$.failed[*]", hasSize(1))
                .expectJsonPath("$.pageIndex", equalTo(0))
                .expectJsonPath("$.failed[0].index", equalTo(2))
                .expectJsonPath("$.failed[0].status", equalTo(400));
    }

    @Test
    void entity5Fails_othersPersisted() throws Exception {
        List<ObjectNode> suppliers = createSupplierNodes(10, "Bulk");
        suppliers.get(4).put("name", "");

        client.createMany("Supplier", suppliers)
                .expectStatus(207)
                .expectJsonPath("$.succeeded[*]", hasSize(9))
                .expectJsonPath("$.failed[*]", hasSize(1))
                .expectJsonPath("$.failed[0].index", equalTo(4));
    }

    @Test
    void deletePartialSuccess_returns207() throws Exception {
        List<ObjectNode> suppliers = createSupplierNodes(5, "DelPartial");

        SpeedyTestResult createResult = client.createMany("Supplier", suppliers)
                .expectOk();
        var idsArray = CommonUtil.json().readTree(createResult.responseBody()).get("payload");
        List<ObjectNode> deletePks = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (var node : idsArray) {
            ObjectNode pk = mapper.createObjectNode();
            pk.put("id", node.get("id").asText());
            deletePks.add(pk);
        }

        ObjectNode fakePk = mapper.createObjectNode();
        fakePk.put("id", "non-existent-id-99999");
        deletePks.add(fakePk);

        client.deleteMany("Supplier").items(deletePks).execute()
                .expectStatus(207)
                .expectJsonPath("$.succeeded", notNullValue())
                .expectJsonPath("$.failed", notNullValue())
                .expectJsonPath("$.failed[*]", hasSize(1))
                .expectJsonPath("$.failed[0].index", equalTo(5))
                .expectJsonPath("$.pageIndex", equalTo(0));
    }

    @Test
    void emptyArray_returns200WithEmptyArrays() throws Exception {
        client.createMany("Supplier", List.of())
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(0));
    }
}
