package com.github.silent.samurai.speedy.yaml.response;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponseWriter;
import com.github.silent.samurai.speedy.models.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/// YAML {@link SpeedyResponseWriter}. Realizes the format-agnostic token stream by writing
/// straight to a streaming Jackson {@link JsonGenerator} produced by a {@link YAMLFactory} —
/// the write-side mirror of the
/// {@link com.github.silent.samurai.speedy.yaml.request.YamlStructureReader}'s cursor. The
/// generator handles all YAML syntax (nesting, indentation, quoting) internally, so the
/// writer holds no document tree and allocates no per-node objects: O(1) allocation beyond
/// the generator's pooled internal buffers.
///
/// The generator targets an in-memory {@link ByteArrayOutputStream} rather than the socket,
/// so nothing reaches the {@link HttpServletResponse} until {@link #finish}. This preserves
/// transactional error handling — a mid-walk failure propagates before any status or body is
/// committed.
///
/// A near-verbatim port of the JSON writer: YAML's streaming backend accepts the same
/// generator token calls, so the sole difference is the {@link YAMLFactory}.
public class YamlResponseWriter implements SpeedyResponseWriter {

    private static final YAMLFactory FACTORY = new YAMLFactory();
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_TIME;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
    private JsonGenerator gen;

    public YamlResponseWriter() {
        try {
            // Targets the in-memory buffer (UTF-8); never actually throws for a ByteArrayOutputStream.
            this.gen = FACTORY.createGenerator(buffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void startObject() throws SpeedyHttpException {
        try {
            gen.writeStartObject();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void endObject() throws SpeedyHttpException {
        try {
            gen.writeEndObject();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void startArray() throws SpeedyHttpException {
        try {
            gen.writeStartArray();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void endArray() throws SpeedyHttpException {
        try {
            gen.writeEndArray();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void field(String name) throws SpeedyHttpException {
        try {
            gen.writeFieldName(name);
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeNull() throws SpeedyHttpException {
        try {
            gen.writeNull();
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeSpeedyInt(SpeedyInt value) throws SpeedyHttpException {
        try {
            gen.writeNumber(value.getValue());
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeSpeedyText(SpeedyText value) throws SpeedyHttpException {
        try {
            gen.writeString(value.getValue());
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeSpeedyDouble(SpeedyDouble value) throws SpeedyHttpException {
        try {
            gen.writeNumber(value.getValue());
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeSpeedyBoolean(SpeedyBoolean value) throws SpeedyHttpException {
        try {
            gen.writeBoolean(value.getValue());
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeSpeedyDate(SpeedyDate value) throws SpeedyHttpException {
        try {
            gen.writeString(value.getValue().format(ISO_DATE));
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeSpeedyDateTime(SpeedyDateTime value) throws SpeedyHttpException {
        try {
            gen.writeString(value.getValue().format(ISO_DATE_TIME));
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeSpeedyTime(SpeedyTime value) throws SpeedyHttpException {
        try {
            gen.writeString(value.getValue().format(ISO_TIME));
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeSpeedyZonedDateTime(SpeedyZonedDateTime value) throws SpeedyHttpException {
        try {
            gen.writeString(value.getValue().format(ISO_OFFSET_DATE_TIME));
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeSpeedyEnum(SpeedyEnum value) throws SpeedyHttpException {
        try {
            if (value.getValueType() == ValueType.ENUM_ORD) {
                gen.writeNumber(value.asEnumOrd());
            } else {
                gen.writeString(value.asEnum());
            }
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeInt(long value) throws SpeedyHttpException {
        try {
            gen.writeNumber(value);
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeText(String value) throws SpeedyHttpException {
        try {
            if (value == null || value.isEmpty()) {
                gen.writeNull();
            } else {
                gen.writeString(value);
            }
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeBool(boolean value) throws SpeedyHttpException {
        try {
            gen.writeBoolean(value);
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void reset() throws SpeedyHttpException {
        buffer.reset();
        try {
            gen = FACTORY.createGenerator(buffer);
        } catch (IOException e) {
            throw wrap(e);
        }
    }

    @Override
    public void finish(HttpServletResponse out, int status, Map<String, String> headers, String contentType)
            throws SpeedyHttpException {
        out.setStatus(status);
        out.setContentType(contentType);
        headers.forEach(out::setHeader);
        try {
            gen.close(); // flush the complete document into the in-memory buffer
            buffer.writeTo(out.getOutputStream());
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error", e);
        }
    }

    private static InternalServerError wrap(IOException e) {
        return new InternalServerError("Internal Server Error", e);
    }
}
