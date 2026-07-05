package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// CRUD run once per {@link IoFormat}: create (single + bulk), get (by key + all),
/// update (PATCH), delete (single + bulk). Each asserts the response was rendered in the
/// negotiated format and verifies the effect against the repository — proving the same
/// endpoints behave identically across every wire format.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoCrudTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CategoryRepository categoryRepository;

    private static ArrayNode array(String... names) {
        ArrayNode array = CommonUtil.json().createArrayNode();
        for (String name : names) {
            array.addObject().put("name", name);
        }
        return array;
    }

    private MvcResult createCategories(IoMvc io, ArrayNode body) throws Exception {
        return io.post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix(), body);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void createSingle(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        String name = "io-crud-" + RandomString.make(8);
        MvcResult result = createCategories(io, array(name));

        JsonNode payload = io.tree(result).get("payload");
        assertEquals(1, payload.size());
        assertFalse(payload.get(0).get("id").asText().isEmpty());
        assertTrue(categoryRepository.findByName(name).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void createBulk(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        String a = "io-bulk-" + RandomString.make(8);
        String b = "io-bulk-" + RandomString.make(8);
        String c = "io-bulk-" + RandomString.make(8);
        MvcResult result = createCategories(io, array(a, b, c));

        assertEquals(3, io.tree(result).get("payload").size());
        assertTrue(categoryRepository.findByName(a).isPresent());
        assertTrue(categoryRepository.findByName(b).isPresent());
        assertTrue(categoryRepository.findByName(c).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void getByKeyAndGetAll(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        String name = "io-get-" + RandomString.make(8);
        createCategories(io, array(name));

        MvcResult byName = io.get(SpeedyConstants.URI + "/Category?name='" + name + "'");
        JsonNode payload = io.tree(byName).get("payload");
        assertEquals(1, payload.size());
        assertEquals(name, payload.get(0).get("name").asText());

        MvcResult all = io.get(SpeedyConstants.URI + "/Category/");
        assertFalse(io.tree(all).get("payload").isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void updatePatch(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        String name = "io-upd-" + RandomString.make(8);
        String newName = "io-upd-" + RandomString.make(8);
        String id = io.tree(createCategories(io, array(name))).at("/payload/0/id").asText();

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("id", id);
        body.put("name", newName);

        MvcResult result = io.patch(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.UPDATE.suffix(), body);

        io.tree(result); // asserts negotiated format
        assertEquals(newName, categoryRepository.findById(id).orElseThrow().getName());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void deleteSingle(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        String name = "io-del-" + RandomString.make(8);
        String id = io.tree(createCategories(io, array(name))).at("/payload/0/id").asText();

        ArrayNode body = CommonUtil.json().createArrayNode();
        body.addObject().put("id", id);

        MvcResult result = io.delete(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.DELETE.suffix(), body);

        io.tree(result); // asserts negotiated format
        assertFalse(categoryRepository.findByName(name).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void deleteBulk(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        String a = "io-delb-" + RandomString.make(8);
        String b = "io-delb-" + RandomString.make(8);
        JsonNode created = io.tree(createCategories(io, array(a, b))).get("payload");

        ArrayNode body = CommonUtil.json().createArrayNode();
        body.addObject().put("id", created.get(0).get("id").asText());
        body.addObject().put("id", created.get(1).get("id").asText());

        MvcResult result = io.delete(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.DELETE.suffix(), body);

        io.tree(result); // asserts negotiated format
        assertFalse(categoryRepository.findByName(a).isPresent());
        assertFalse(categoryRepository.findByName(b).isPresent());
    }
}
