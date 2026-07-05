package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.Speedy;
import com.github.silent.samurai.speedy.client.exception.SpeedyException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// The error envelope ({@code status}/{@code message}/{@code timestamp}) rendered once per wire
/// format, driven through the production {@link Speedy} client. Uses validation failures on a
/// valid entity (empty and over-long name), which occur AFTER content negotiation, so the error
/// body is written by the negotiated writer and mapped to a typed {@link SpeedyException} —
/// proving errors are not silently forced to JSON on either side of the wire.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIoErrorTest {

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

    private static void assertErrorEnvelope(SpeedyException ex, int expectedStatus) {
        assertEquals(expectedStatus, ex.statusCode());
        assertNotNull(ex.serverMessage());
        assertFalse(ex.serverMessage().isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void emptyName_returns400Envelope(SpeedyFormat format) {
        Speedy speedy = client(format);

        SpeedyException ex = assertThrows(SpeedyException.class,
                () -> speedy.create("Category").field("name", "").execute());

        assertErrorEnvelope(ex, 400);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formats")
    void tooLongName_returns400Envelope(SpeedyFormat format) {
        Speedy speedy = client(format);

        SpeedyException ex = assertThrows(SpeedyException.class,
                () -> speedy.create("Category").field("name", RandomString.make(251)).execute());

        assertErrorEnvelope(ex, 400);
    }
}
