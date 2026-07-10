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
/// {@code Supplier} is annotated {@code @SpeedyBulk(true)} and used as a regression guard that
/// bulk stays enabled for entities that explicitly opt in. {@code Task} carries no
/// {@code @SpeedyBulk} annotation and is used to prove bulk is rejected by default.
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

    // ---- update ----

    @Test
    void multiElementUpdate_isRejected() throws Exception {
        String id1 = createOneAndGetId("updA");
        String id2 = createOneAndGetId("updB");

        client.updateMany("BulkDisabledEntity", List.of(
                        renameNode(id1, "upd-a"),
                        renameNode(id2, "upd-b")))
                .expectBadRequest();
    }

    @Test
    void singleElementArrayUpdate_works() throws Exception {
        String id = createOneAndGetId("updArr");

        client.updateMany("BulkDisabledEntity", List.of(renameNode(id, "upd-single")))
                .expectOk();
    }

    @Test
    void bareObjectUpdate_works() throws Exception {
        String id = createOneAndGetId("updObj");

        client.update("BulkDisabledEntity")
                .key("id", id)
                .field("name", "Bulk-" + (System.nanoTime() & 0xFFFFFF) + "-upd-bare")
                .execute()
                .expectOk();
    }

    // ---- replace (symmetric with update) ----

    @Test
    void multiElementReplace_isRejected() throws Exception {
        String id1 = createOneAndGetId("repA");
        String id2 = createOneAndGetId("repB");

        client.replaceMany("BulkDisabledEntity", List.of(
                        renameNode(id1, "rep-a"),
                        renameNode(id2, "rep-b")))
                .expectBadRequest();
    }

    @Test
    void singleElementArrayReplace_works() throws Exception {
        String id = createOneAndGetId("repArr");

        client.replaceMany("BulkDisabledEntity", List.of(renameNode(id, "rep-single")))
                .expectOk();
    }

    @Test
    void bareObjectReplace_works() throws Exception {
        String id = createOneAndGetId("repObj");

        client.replace("BulkDisabledEntity")
                .key("id", id)
                .field("name", "Bulk-" + (System.nanoTime() & 0xFFFFFF) + "-rep-bare")
                .execute()
                .expectOk();
    }

    // ---- regression: explicitly-enabled entity keeps bulk working ----

    @Test
    void explicitlyEnabledEntity_multiElementCreate_stillWorks() throws Exception {
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

    @Test
    void explicitlyEnabledEntity_multiElementUpdate_stillWorks() throws Exception {
        long ts = System.currentTimeMillis();
        String id1 = createSupplierAndGetId(ts, 0);
        String id2 = createSupplierAndGetId(ts, 1);

        ObjectNode item1 = mapper.createObjectNode();
        item1.put("id", id1);
        item1.put("name", "BulkGuardUpdated-" + (ts & 0xFFFFF) + "-0");
        ObjectNode item2 = mapper.createObjectNode();
        item2.put("id", id2);
        item2.put("name", "BulkGuardUpdated-" + (ts & 0xFFFFF) + "-1");

        client.updateMany("Supplier", List.of(item1, item2))
                .expectOk();
    }

    // ---- regression: unannotated entity rejects bulk by default ----

    @Test
    void unannotatedEntity_multiElementCreate_isRejectedByDefault() throws Exception {
        List<ObjectNode> tasks = new ArrayList<>();
        long ts = System.currentTimeMillis();
        for (int i = 0; i < 2; i++) {
            ObjectNode t = mapper.createObjectNode();
            t.put("title", "NoBulkByDefault-" + (ts & 0xFFFFF) + "-" + i);
            tasks.add(t);
        }

        client.createMany("Task", tasks)
                .expectBadRequest();
    }

    @Test
    void unannotatedEntity_multiElementUpdate_isRejectedByDefault() throws Exception {
        // Task rows are seeded by x-data.sql (task-0000-...-0001 / -0002); reuse them rather
        // than creating new ones, since Task carries no @SpeedyBulk annotation.
        ObjectNode item1 = mapper.createObjectNode();
        item1.put("id", "task-0000-0000-0000-000000000001");
        item1.put("title", "NoBulkUpdateByDefault-1");
        ObjectNode item2 = mapper.createObjectNode();
        item2.put("id", "task-0000-0000-0000-000000000002");
        item2.put("title", "NoBulkUpdateByDefault-2");

        client.updateMany("Task", List.of(item1, item2))
                .expectBadRequest();
    }

    private String createOneAndGetId(String suffix) throws Exception {
        SpeedyTestResult result = client.createOne("BulkDisabledEntity", bulkDisabledNode(suffix))
                .expectOk();
        return CommonUtil.json().readTree(result.responseBody())
                .get("payload").get(0).get("id").asText();
    }

    private ObjectNode renameNode(String id, String suffix) {
        ObjectNode n = mapper.createObjectNode();
        n.put("id", id);
        n.put("name", "Bulk-" + (System.nanoTime() & 0xFFFFFF) + "-" + suffix);
        return n;
    }

    private String createSupplierAndGetId(long ts, int index) throws Exception {
        ObjectNode s = mapper.createObjectNode();
        s.put("name", "BulkGuardUpd-" + (ts & 0xFFFFF) + "-" + index);
        s.put("phoneNo", "u1-" + (ts & 0xFFF) + "-" + index);
        s.put("altPhoneNo", "u2-" + (ts & 0xFFF) + "-" + index);
        SpeedyTestResult result = client.createOne("Supplier", s).expectOk();
        return CommonUtil.json().readTree(result.responseBody())
                .get("payload").get(0).get("id").asText();
    }
}
