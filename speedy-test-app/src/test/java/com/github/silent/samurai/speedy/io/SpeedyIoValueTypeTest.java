package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.Speedy;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.format.JsonFormat;
import com.github.silent.samurai.speedy.client.format.SpeedyFormat;
import com.github.silent.samurai.speedy.client.format.XmlFormat;
import com.github.silent.samurai.speedy.client.format.YamlFormat;
import com.github.silent.samurai.speedy.client.test.MockMvcTransport;
import com.github.silent.samurai.speedy.entity.ValueTestEntity;
import com.github.silent.samurai.speedy.repositories.ValueTestRepository;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Round-trips every {@code ValueType} through each wire format via the production
/// {@link Speedy} client: the leaf switch each I/O provider must implement. TEXT/INT/FLOAT via
/// TypeOverrideEntity, the temporal + BOOL types via ValueTestEntity, and ENUM (string) +
/// ENUM_ORD (ordinal) via Task. Each case runs once per format in a single run.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoValueTypeTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ValueTestRepository valueTestRepository;

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
    void textIntFloat_roundTrip(SpeedyFormat format) {
        Speedy speedy = client(format);

        SpeedyResult created = speedy.create("TypeOverrideEntity")
                .field("textField", "hello io")
                .field("bigIntField", 42)
                .field("floatField", 3.14)
                .execute();
        String id = created.firstRaw().get("id").asText();
        assertFalse(id.isEmpty());

        JsonNode got = speedy.get("TypeOverrideEntity").key("id", id).execute().firstRaw();

        assertEquals("hello io", got.get("textField").asText());
        assertEquals(42, got.get("bigIntField").asInt());
        assertEquals(3.14, got.get("floatField").asDouble(), 1e-6);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void temporalAndBoolean_roundTrip(SpeedyFormat format) {
        Speedy speedy = client(format);

        SpeedyResult created = speedy.create("ValueTestEntity")
                .field("localDateTime", "2021-01-01T00:00:00")
                .field("localDate", "2021-01-01")
                .field("localTime", "00:00:00")
                .field("instantTime", "2021-01-01T00:00:00Z")
                .field("zonedDateTime", "2021-01-01T00:00+09:00")
                .field("booleanValue", true)
                .field("doubleValue", 2.718)
                .execute();
        String id = created.firstRaw().get("id").asText();
        assertFalse(id.isEmpty());

        JsonNode got = speedy.get("ValueTestEntity").key("id", id).execute().firstRaw();

        // Response leaf rendering (writer)
        assertEquals("2021-01-01T00:00:00", got.get("localDateTime").asText());
        assertEquals("2021-01-01", got.get("localDate").asText());
        assertEquals("00:00:00", got.get("localTime").asText());
        assertTrue(got.get("booleanValue").asBoolean());
        assertEquals(2.718, got.get("doubleValue").asDouble(), 1e-6);
        assertEquals(
                ZonedDateTime.parse("2021-01-01T00:00+09:00").toInstant(),
                ZonedDateTime.parse(got.get("zonedDateTime").asText()).toInstant());

        // Request leaf parsing (reader) — verified against the persisted JPA entity
        ValueTestEntity persisted = valueTestRepository.findById(id).orElseThrow();
        assertEquals(LocalDateTime.parse("2021-01-01T00:00:00"), persisted.getLocalDateTime());
        assertEquals(LocalDate.parse("2021-01-01"), persisted.getLocalDate());
        assertEquals(LocalTime.parse("00:00:00"), persisted.getLocalTime());
        assertEquals(Boolean.TRUE, persisted.getBooleanValue());
        assertEquals(2.718, persisted.getDoubleValue(), 1e-6);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void enumStringAndOrdinal_roundTrip(SpeedyFormat format) {
        Speedy speedy = client(format);

        SpeedyResult created = speedy.create("Task")
                .field("title", "io-enum-" + RandomString.make(6))
                .field("priority", "LOW")   // ENUM (string)
                .field("difficulty", 0)      // ENUM_ORD (ordinal)
                .execute();
        String id = created.firstRaw().get("id").asText();
        assertFalse(id.isEmpty());

        JsonNode got = speedy.get("Task").key("id", id).execute().firstRaw();

        assertEquals("LOW", got.get("priority").asText());
        assertEquals(0, got.get("difficulty").asInt());
    }
}
