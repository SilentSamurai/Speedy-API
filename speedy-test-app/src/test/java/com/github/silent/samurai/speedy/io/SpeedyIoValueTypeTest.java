package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.entity.ValueTestEntity;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.repositories.ValueTestRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Round-trips every {@code ValueType} through each {@link IoFormat} reader (request) and
/// writer (response): the leaf switch each I/O provider must implement. TEXT/INT/FLOAT via
/// TypeOverrideEntity, the temporal + BOOL types via ValueTestEntity, and ENUM (string) +
/// ENUM_ORD (ordinal) via Task. Each case runs once per format in a single run.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoValueTypeTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ValueTestRepository valueTestRepository;

    /// POSTs a single-object create body and returns the created id.
    private String create(IoMvc io, String entity, ObjectNode fields) throws Exception {
        ArrayNode body = CommonUtil.json().createArrayNode();
        body.add(fields);
        MvcResult result = io.post(SpeedyConstants.URI + "/" + entity + "/" + SpeedyEndpoint.CREATE.suffix(), body);
        String id = io.tree(result).at("/payload/0/id").asText();
        assertFalse(id.isEmpty());
        return id;
    }

    private JsonNode get(IoMvc io, String entity, String id) throws Exception {
        MvcResult result = io.get(SpeedyConstants.URI + "/" + entity + "?id='" + id + "'");
        JsonNode payload = io.tree(result).get("payload");
        assertEquals(1, payload.size());
        return payload.get(0);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void textIntFloat_roundTrip(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        ObjectNode fields = CommonUtil.json().createObjectNode();
        fields.put("textField", "hello io");
        fields.put("bigIntField", 42);
        fields.put("floatField", 3.14);

        String id = create(io, "TypeOverrideEntity", fields);
        JsonNode got = get(io, "TypeOverrideEntity", id);

        assertEquals("hello io", got.get("textField").asText());
        assertEquals(42, got.get("bigIntField").asInt());
        assertEquals(3.14, got.get("floatField").asDouble(), 1e-6);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(IoFormat.class)
    void temporalAndBoolean_roundTrip(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        ObjectNode fields = CommonUtil.json().createObjectNode();
        fields.put("localDateTime", "2021-01-01T00:00:00");
        fields.put("localDate", "2021-01-01");
        fields.put("localTime", "00:00:00");
        fields.put("instantTime", "2021-01-01T00:00:00Z");
        fields.put("zonedDateTime", "2021-01-01T00:00+09:00");
        fields.put("booleanValue", true);
        fields.put("doubleValue", 2.718);

        String id = create(io, "ValueTestEntity", fields);
        JsonNode got = get(io, "ValueTestEntity", id);

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
    @EnumSource(IoFormat.class)
    void enumStringAndOrdinal_roundTrip(IoFormat fmt) throws Exception {
        IoMvc io = new IoMvc(mvc, fmt);
        ObjectNode fields = CommonUtil.json().createObjectNode();
        fields.put("title", "io-enum-" + RandomString.make(6));
        fields.put("priority", "LOW");   // ENUM (string)
        fields.put("difficulty", 0);      // ENUM_ORD (ordinal)

        String id = create(io, "Task", fields);
        JsonNode got = get(io, "Task", id);

        assertEquals("LOW", got.get("priority").asText());
        assertEquals(0, got.get("difficulty").asInt());
    }
}
