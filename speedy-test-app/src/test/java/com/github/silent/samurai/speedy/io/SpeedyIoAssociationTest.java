package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.Speedy;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.format.JsonFormat;
import com.github.silent.samurai.speedy.client.format.SpeedyFormat;
import com.github.silent.samurai.speedy.client.format.XmlFormat;
import com.github.silent.samurai.speedy.client.format.YamlFormat;
import com.github.silent.samurai.speedy.client.test.MockMvcTransport;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// A nested foreign-key association round-trips once per wire format through the production
/// {@link Speedy} client: a {@code Product} is created with an inline {@code category.id}
/// field, and read back to confirm the nested object is both sent (request) and parsed
/// (response) — exercising the FK nesting the flat CRUD suite never hits.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoAssociationTest {

    /// A category seeded by the test fixtures — the FK target for the created product.
    private static final String CATEGORY_ID = "1";

    @Autowired
    private MockMvc mvc;

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
    void createAndReadNestedForeignKey(SpeedyFormat format) {
        Speedy speedy = client(format);
        String name = "io-assoc-" + RandomString.make(8);

        SpeedyResult created = speedy.create("Product")
                .field("name", name)
                .field("description", "io-assoc-desc")
                .field("category.id", CATEGORY_ID) // nested FK object
                .execute();
        String id = created.firstRaw().get("id").asText();
        assertFalse(id.isEmpty());

        SpeedyResult fetched = speedy.get("Product").key("id", id).execute();
        var got = fetched.firstRaw();

        assertEquals(name, got.get("name").asText());
        // The FK is rendered back as a nested object, not a flat scalar.
        assertEquals(CATEGORY_ID, got.get("category").get("id").asText());
    }
}
