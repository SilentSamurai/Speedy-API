package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.Speedy;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.SpeedyException;
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

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/// Drives the production {@link Speedy} client end-to-end through the real server
/// (via {@link MockMvcTransport}) once per wire format. This is the proof that the
/// client-side codecs produce request shapes the server's {@code ISpeedyIoProvider}s
/// accept, and parse the responses those providers render.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyClientFormatTest {

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
    void fullCrudRoundTrip(SpeedyFormat format) {
        Speedy speedy = client(format);
        String name = "cli-fmt-" + RandomString.make(8);
        String newName = "cli-fmt-" + RandomString.make(8);

        // create
        SpeedyResult created = speedy.create("Category").field("name", name).execute();
        assertEquals(1, created.size());
        String id = created.firstRaw().get("id").asText();
        assertFalse(id.isEmpty());
        assertTrue(categoryRepository.findByName(name).isPresent());

        // get by key
        SpeedyResult fetched = speedy.get("Category").key("id", id).execute();
        assertEquals(name, fetched.firstRaw().get("name").asText());

        // update
        speedy.update("Category").key("id", id).field("name", newName).execute();
        assertEquals(newName, categoryRepository.findById(id).orElseThrow().getName());

        // query (unfiltered) should include the updated entity
        SpeedyResult queried = speedy.query("Category").pageSize(1000).execute();
        assertTrue(queried.raw().findValuesAsText("id").contains(id));

        // delete
        speedy.delete("Category").key("id", id).execute();
        assertFalse(categoryRepository.findByName(newName).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void bulkCreateAndDelete(SpeedyFormat format) {
        Speedy speedy = client(format);
        String a = "cli-blk-" + RandomString.make(8);
        String b = "cli-blk-" + RandomString.make(8);

        ObjectMapper json = new ObjectMapper();
        SpeedyResult created = speedy.createMany("Category")
                .items(java.util.List.of(
                        json.createObjectNode().put("name", a),
                        json.createObjectNode().put("name", b)))
                .execute();
        assertEquals(2, created.size());

        speedy.deleteMany("Category", java.util.List.of(
                json.createObjectNode().put("id", created.raw().get(0).get("id").asText()),
                json.createObjectNode().put("id", created.raw().get(1).get("id").asText())));
        assertFalse(categoryRepository.findByName(a).isPresent());
        assertFalse(categoryRepository.findByName(b).isPresent());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void serverValidationErrorShouldMapToTypedException(SpeedyFormat format) {
        Speedy speedy = client(format);

        // Empty name fails validation after negotiation, so the error envelope itself
        // is rendered by the negotiated writer (see SpeedyIoErrorTest for the server-side proof).
        SpeedyException ex = assertThrows(SpeedyException.class,
                () -> speedy.create("Category").field("name", "").execute());

        assertEquals(400, ex.statusCode());
        assertNotNull(ex.serverMessage());
        assertFalse(ex.serverMessage().isEmpty());
    }
}
