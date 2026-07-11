package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.Speedy;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.format.JsonFormat;
import com.github.silent.samurai.speedy.client.format.SpeedyFormat;
import com.github.silent.samurai.speedy.client.format.XmlFormat;
import com.github.silent.samurai.speedy.client.format.YamlFormat;
import com.github.silent.samurai.speedy.client.test.MockMvcTransport;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// CRUD driven through the production {@link Speedy} client (via {@link MockMvcTransport}) once
/// per wire format: create (single + bulk), get (by key + query), update/replace (single + bulk),
/// delete (single + bulk). Proves the same endpoints behave identically across every wire format
/// from the client's perspective, not just the server's.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoCrudTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CategoryRepository categoryRepository;

    static Stream<Arguments> formats() {
        return Stream.of(
                Arguments.of(Named.of("JSON", (SpeedyFormat) new JsonFormat(new ObjectMapper()))),
                Arguments.of(Named.of("YAML", (SpeedyFormat) new YamlFormat())),
                Arguments.of(Named.of("XML", (SpeedyFormat) new XmlFormat())));
    }

    private Speedy client(SpeedyFormat format) {
        return Speedy.builder()
                .baseUrl("http://localhost")
                .transport(new MockMvcTransport(mvc))
                .format(format)
                .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void createSingle(SpeedyFormat format) {
        Speedy speedy = client(format);
        String name = "io-crud-" + RandomString.make(8);

        SpeedyResult created = speedy.create("Category").field("name", name).execute();

        assertEquals(1, created.size());
        assertFalse(created.firstRaw().get("id").asText().isEmpty());
        assertTrue(categoryRepository.findByName(name).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void createBulk(SpeedyFormat format) {
        Speedy speedy = client(format);
        String a = "io-bulk-" + RandomString.make(8);
        String b = "io-bulk-" + RandomString.make(8);
        String c = "io-bulk-" + RandomString.make(8);

        ObjectMapper json = new ObjectMapper();
        SpeedyResult created = speedy.createMany("Category")
                .items(List.of(
                        json.createObjectNode().put("name", a),
                        json.createObjectNode().put("name", b),
                        json.createObjectNode().put("name", c)))
                .execute();

        assertEquals(3, created.size());
        assertTrue(categoryRepository.findByName(a).isPresent());
        assertTrue(categoryRepository.findByName(b).isPresent());
        assertTrue(categoryRepository.findByName(c).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void getByKeyAndQueryAll(SpeedyFormat format) {
        Speedy speedy = client(format);
        String name = "io-get-" + RandomString.make(8);
        String id = speedy.create("Category").field("name", name).execute().firstRaw().get("id").asText();

        SpeedyResult byKey = speedy.get("Category").key("id", id).execute();
        assertEquals(name, byKey.firstRaw().get("name").asText());

        SpeedyResult all = speedy.query("Category").pageSize(1000).execute();
        assertFalse(all.isEmpty());
        assertTrue(all.raw().findValuesAsText("id").contains(id));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void updatePatch(SpeedyFormat format) {
        Speedy speedy = client(format);
        String name = "io-upd-" + RandomString.make(8);
        String newName = "io-upd-" + RandomString.make(8);
        String id = speedy.create("Category").field("name", name).execute().firstRaw().get("id").asText();

        speedy.update("Category").key("id", id).field("name", newName).execute();

        assertEquals(newName, categoryRepository.findById(id).orElseThrow().getName());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void updateBulk(SpeedyFormat format) {
        Speedy speedy = client(format);
        String a = "io-updb-" + RandomString.make(8);
        String b = "io-updb-" + RandomString.make(8);
        String newA = "io-updb-" + RandomString.make(8);
        String newB = "io-updb-" + RandomString.make(8);

        ObjectMapper json = new ObjectMapper();
        SpeedyResult created = speedy.createMany("Category")
                .items(List.of(
                        json.createObjectNode().put("name", a),
                        json.createObjectNode().put("name", b)))
                .execute();

        ObjectNode itemA = json.createObjectNode();
        itemA.put("id", created.raw().get(0).get("id").asText());
        itemA.put("name", newA);
        ObjectNode itemB = json.createObjectNode();
        itemB.put("id", created.raw().get(1).get("id").asText());
        itemB.put("name", newB);

        SpeedyResult updated = speedy.updateMany("Category", List.of(itemA, itemB));

        assertEquals(2, updated.size());
        assertTrue(categoryRepository.findByName(newA).isPresent());
        assertTrue(categoryRepository.findByName(newB).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void replaceSingle(SpeedyFormat format) {
        Speedy speedy = client(format);
        String name = "io-rep-" + RandomString.make(8);
        String newName = "io-rep-" + RandomString.make(8);
        String id = speedy.create("Category").field("name", name).execute().firstRaw().get("id").asText();

        speedy.replace("Category").key("id", id).field("name", newName).execute();

        assertEquals(newName, categoryRepository.findById(id).orElseThrow().getName());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void replaceBulk(SpeedyFormat format) {
        Speedy speedy = client(format);
        String a = "io-repb-" + RandomString.make(8);
        String b = "io-repb-" + RandomString.make(8);
        String newA = "io-repb-" + RandomString.make(8);
        String newB = "io-repb-" + RandomString.make(8);

        ObjectMapper json = new ObjectMapper();
        SpeedyResult created = speedy.createMany("Category")
                .items(List.of(
                        json.createObjectNode().put("name", a),
                        json.createObjectNode().put("name", b)))
                .execute();

        ObjectNode itemA = json.createObjectNode();
        itemA.put("id", created.raw().get(0).get("id").asText());
        itemA.put("name", newA);
        ObjectNode itemB = json.createObjectNode();
        itemB.put("id", created.raw().get(1).get("id").asText());
        itemB.put("name", newB);

        SpeedyResult replaced = speedy.replaceMany("Category", List.of(itemA, itemB));

        assertEquals(2, replaced.size());
        assertTrue(categoryRepository.findByName(newA).isPresent());
        assertTrue(categoryRepository.findByName(newB).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void deleteSingle(SpeedyFormat format) {
        Speedy speedy = client(format);
        String name = "io-del-" + RandomString.make(8);
        String id = speedy.create("Category").field("name", name).execute().firstRaw().get("id").asText();

        speedy.delete("Category").key("id", id).execute();

        assertFalse(categoryRepository.findByName(name).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void deleteBulk(SpeedyFormat format) {
        Speedy speedy = client(format);
        String a = "io-delb-" + RandomString.make(8);
        String b = "io-delb-" + RandomString.make(8);

        ObjectMapper json = new ObjectMapper();
        SpeedyResult created = speedy.createMany("Category")
                .items(List.of(
                        json.createObjectNode().put("name", a),
                        json.createObjectNode().put("name", b)))
                .execute();

        ObjectNode pkA = json.createObjectNode().put("id", created.raw().get(0).get("id").asText());
        ObjectNode pkB = json.createObjectNode().put("id", created.raw().get(1).get("id").asText());
        speedy.deleteMany("Category", List.of(pkA, pkB));

        assertFalse(categoryRepository.findByName(a).isPresent());
        assertFalse(categoryRepository.findByName(b).isPresent());
    }
}
