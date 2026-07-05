package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.Speedy;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
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

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// The structured {@code $query} POST body exercised once per wire format through the
/// production {@link Speedy} client's {@link com.github.silent.samurai.speedy.client.builder.QueryBuilder}.
/// Unlike a plain GET-by-key, this drives the query DSL's WHERE clause through the request
/// reader's body-parse path, then asserts the response is correctly parsed back by the client.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoQueryTest {

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
    void queryByWhereClause(SpeedyFormat format) {
        Speedy speedy = client(format);
        String name = "io-query-" + RandomString.make(8);

        // Seed a row to match on.
        speedy.create("Category").field("name", name).execute();

        // Query it back through the structured $query body.
        SpeedyResult result = speedy.query("Category")
                .where(condition("name", eq(name)))
                .execute();

        assertEquals(1, result.size());
        assertEquals(name, result.firstRaw().get("name").asText());
    }
}
