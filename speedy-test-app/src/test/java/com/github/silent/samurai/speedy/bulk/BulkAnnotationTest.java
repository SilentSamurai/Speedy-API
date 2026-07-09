package com.github.silent.samurai.speedy.bulk;

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

/// Integration tests for issue #28 — {@code @SpeedyBulk}.
///
/// {@code BulkDisabledEntity} is annotated {@code @SpeedyBulk(false)}: single-object and
/// single-element-array create/delete still work, but multi-element arrays are rejected with 400.
/// A default entity ({@code Supplier}) is used as a regression guard that bulk stays enabled
/// when the annotation is absent.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class BulkAnnotationTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mvc);
    }

    private ObjectNode bulkDisabledNode(String suffix) {
        ObjectNode n = mapper.createObjectNode();
        n.put("name", "Bulk-" + (System.nanoTime() & 0xFFFFFF) + "-" + suffix);
        return n;
    }

    // ---- create ----

    @Test
    void multiElementCreate_isRejected() throws Exception {
        List<ObjectNode> nodes = new ArrayList<>();
        nodes.add(bulkDisabledNode("a"));
        nodes.add(bulkDisabledNode("b"));

        client.createMany("BulkDisabledEntity", nodes)
                .expectBadRequest();
    }

    @Test
    void singleElementArrayCreate_works() throws Exception {
        client.create("BulkDisabledEntity")
                .field("name", "Single-" + (System.nanoTime() & 0xFFFFFF))
                .execute()
                .expectOk();
    }

    @Test
    void bareObjectCreate_works() throws Exception {
        client.createOne("BulkDisabledEntity", bulkDisabledNode("obj"))
                .expectOk();
    }

    // ---- delete (symmetric with create) ----

    @Test
    void multiElementDelete_isRejected() throws Exception {
        List<ObjectNode> pks = new ArrayList<>();
        ObjectNode a = mapper.createObjectNode();
        a.put("id", "id-a");
        ObjectNode b = mapper.createObjectNode();
        b.put("id", "id-b");
        pks.add(a);
        pks.add(b);

        client.deleteMany("BulkDisabledEntity").items(pks).execute()
                .expectBadRequest();
    }

    @Test
    void singleElementArrayDelete_works() throws Exception {
        String id = createOneAndGetId("delArr");

        client.delete("BulkDisabledEntity").key("id", id).execute()
                .expectOk();
    }

    @Test
    void bareObjectDelete_works() throws Exception {
        String id = createOneAndGetId("delObj");

        ObjectNode pk = mapper.createObjectNode();
        pk.put("id", id);
        client.deleteOne("BulkDisabledEntity", pk)
                .expectOk();
    }

    // ---- regression: default entity keeps bulk enabled ----

    @Test
    void defaultEntity_multiElementCreate_stillWorks() throws Exception {
        List<ObjectNode> suppliers = new ArrayList<>();
        long ts = System.currentTimeMillis();
        for (int i = 0; i < 2; i++) {
            ObjectNode s = mapper.createObjectNode();
            s.put("name", "BulkGuard-" + (ts & 0xFFFFF) + "-" + i);
            s.put("phoneNo", "g1-" + (ts & 0xFFF) + "-" + i);
            s.put("altPhoneNo", "g2-" + (ts & 0xFFF) + "-" + i);
            suppliers.add(s);
        }

        client.createMany("Supplier", suppliers)
                .expectOk();
    }

    private String createOneAndGetId(String suffix) throws Exception {
        SpeedyTestResult result = client.createOne("BulkDisabledEntity", bulkDisabledNode(suffix))
                .expectOk();
        return CommonUtil.json().readTree(result.responseBody())
                .get("payload").get(0).get("id").asText();
    }
}
