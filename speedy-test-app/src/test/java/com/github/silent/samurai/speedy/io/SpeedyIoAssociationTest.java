package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
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

/// A nested foreign-key association round-trips once per {@link IoFormat}: a {@code Product} is
/// created with an inline {@code category: {id: "1"}} object, and read back to confirm the
/// nested object is both parsed (request) and rendered (response) — exercising the
/// {@code startObject}/{@code endObject} nesting inside {@code payload} that the flat CRUD suite
/// never hits.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoAssociationTest {

    /// A category seeded by the test fixtures — the FK target for the created product.
    private static final String CATEGORY_ID = "1";

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void createAndReadNestedForeignKey(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        String name = "io-assoc-" + RandomString.make(8);

        ObjectNode product = CommonUtil.json().createObjectNode();
        product.put("name", name);
        product.put("description", "io-assoc-desc");
        product.putObject("category").put("id", CATEGORY_ID); // nested FK object
        ArrayNode body = CommonUtil.json().createArrayNode();
        body.add(product);

        MvcResult created = io.post(SpeedyConstants.URI + "/Product/" + SpeedyEndpoint.CREATE.suffix(), body);
        String id = io.tree(created).at("/payload/0/id").asText();
        assertFalse(id.isEmpty());

        MvcResult read = io.get(SpeedyConstants.URI + "/Product?id='" + id + "'");
        JsonNode got = io.tree(read).get("payload").get(0);

        assertEquals(name, got.get("name").asText());
        // The FK is rendered back as a nested object, not a flat scalar.
        assertEquals(CATEGORY_ID, got.get("category").get("id").asText());
    }
}
