package com.github.silent.samurai.speedy.xml.response;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponseWriter;
import com.github.silent.samurai.speedy.models.*;
import jakarta.servlet.http.HttpServletResponse;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

public class XmlResponseWriter implements SpeedyResponseWriter {

    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_TIME;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String DEFAULT_ROOT = "response";
    private static final String DEFAULT_ITEM = "item";
    private static final String DEFAULT_ENTITY = "entity";

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
    private XMLStreamWriter writer;
    private final Deque<XmlContext> stack = new ArrayDeque<>();
    private String pendingFieldName;
    private boolean rootDocumentStarted;

    public XmlResponseWriter() {
        try {
            this.writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(buffer, "UTF-8");
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to create XML writer", e);
        }
    }

    private static final class XmlContext {
        String tagName;
        final boolean isArray;
        boolean opened;
        String itemTagName;

        XmlContext(String tagName, boolean isArray) {
            this.tagName = tagName;
            this.isArray = isArray;
        }
    }

    @Override
    public void startObject() throws SpeedyHttpException {
        String tag;
        if (pendingFieldName != null) {
            tag = pendingFieldName;
            pendingFieldName = null;
        } else if (!stack.isEmpty() && stack.peek().isArray) {
            XmlContext arrayCtx = stack.peek();
            tag = arrayCtx.itemTagName;
        } else if (stack.isEmpty()) {
            tag = DEFAULT_ROOT;
        } else {
            tag = DEFAULT_ITEM;
        }
        stack.push(new XmlContext(tag, false));
    }

    @Override
    public void endObject() throws SpeedyHttpException {
        XmlContext ctx = stack.pop();
        try {
            if (ctx.opened) {
                writer.writeEndElement();
            } else {
                ensureRootDocumentStarted();
                if (ctx.tagName != null) {
                    writer.writeEmptyElement(ctx.tagName);
                }
            }
        } catch (XMLStreamException e) {
            throw wrap(e);
        }
    }

    @Override
    public void field(String name) throws SpeedyHttpException {
        if (!stack.isEmpty()) {
            XmlContext current = stack.peek();
            if (!current.opened && current.tagName == null) {
                current.tagName = DEFAULT_ENTITY;
            }
        }
        pendingFieldName = name;
    }

    @Override
    public void field(FieldMetadata field) throws SpeedyHttpException {
        if (!stack.isEmpty()) {
            XmlContext current = stack.peek();
            if (!current.opened && current.tagName == null) {
                String entityName = field.getEntityMetadata().getName();
                current.tagName = entityName;
                for (XmlContext ctx : stack) {
                    if (ctx.isArray && ctx.itemTagName == null) {
                        ctx.itemTagName = entityName;
                        break;
                    }
                }
            }
        }
        pendingFieldName = field.getOutputPropertyName();
    }

    @Override
    public void startArray() throws SpeedyHttpException {
        ensureCurrentContextOpened();

        String tag;
        if (pendingFieldName != null) {
            tag = pendingFieldName;
            pendingFieldName = null;
        } else {
            tag = "items";
        }

        try {
            ensureRootDocumentStarted();
            writer.writeStartElement(tag);
        } catch (XMLStreamException e) {
            throw wrap(e);
        }

        XmlContext ctx = new XmlContext(tag, true);
        ctx.opened = true;
        stack.push(ctx);
    }

    @Override
    public void endArray() throws SpeedyHttpException {
        XmlContext ctx = stack.pop();
        try {
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw wrap(e);
        }
    }

    @Override
    public void writeNull() throws SpeedyHttpException {
        if (pendingFieldName == null) {
            return;
        }
        ensureCurrentContextOpened();
        try {
            writer.writeEmptyElement(pendingFieldName);
        } catch (XMLStreamException e) {
            throw wrap(e);
        }
        pendingFieldName = null;
    }

    @Override
    public void writeSpeedyInt(SpeedyInt value) throws SpeedyHttpException {
        writeScalarElement(Long.toString(value.getValue()));
    }

    @Override
    public void writeSpeedyText(SpeedyText value) throws SpeedyHttpException {
        writeScalarElement(value.getValue());
    }

    @Override
    public void writeSpeedyDouble(SpeedyDouble value) throws SpeedyHttpException {
        writeScalarElement(Double.toString(value.getValue()));
    }

    @Override
    public void writeSpeedyBoolean(SpeedyBoolean value) throws SpeedyHttpException {
        writeScalarElement(Boolean.toString(value.getValue()));
    }

    @Override
    public void writeSpeedyDate(SpeedyDate value) throws SpeedyHttpException {
        writeScalarElement(value.getValue().format(ISO_DATE));
    }

    @Override
    public void writeSpeedyDateTime(SpeedyDateTime value) throws SpeedyHttpException {
        writeScalarElement(value.getValue().format(ISO_DATE_TIME));
    }

    @Override
    public void writeSpeedyTime(SpeedyTime value) throws SpeedyHttpException {
        writeScalarElement(value.getValue().format(ISO_TIME));
    }

    @Override
    public void writeSpeedyZonedDateTime(SpeedyZonedDateTime value) throws SpeedyHttpException {
        writeScalarElement(value.getValue().format(ISO_OFFSET_DATE_TIME));
    }

    @Override
    public void writeSpeedyEnum(SpeedyEnum value) throws SpeedyHttpException {
        if (value.getValueType() == ValueType.ENUM_ORD) {
            writeScalarElement(Long.toString(value.asEnumOrd()));
        } else {
            writeScalarElement(value.asEnum());
        }
    }

    @Override
    public void writeInt(long value) throws SpeedyHttpException {
        writeScalarElement(Long.toString(value));
    }

    @Override
    public void writeText(String value) throws SpeedyHttpException {
        if (value == null) {
            writeNull();
        } else {
            writeScalarElement(value);
        }
    }

    @Override
    public void writeBool(boolean value) throws SpeedyHttpException {
        writeScalarElement(Boolean.toString(value));
    }

    @Override
    public void reset() throws SpeedyHttpException {
        buffer.reset();
        stack.clear();
        pendingFieldName = null;
        rootDocumentStarted = false;
        try {
            writer.close();
        } catch (XMLStreamException ignored) {
        }
        try {
            writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(buffer, "UTF-8");
        } catch (XMLStreamException e) {
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
            writer.writeEndDocument();
            writer.close();
            buffer.writeTo(out.getOutputStream());
        } catch (XMLStreamException | IOException e) {
            throw new InternalServerError("Internal Server Error", e);
        }
    }

    private void writeScalarElement(String value) throws SpeedyHttpException {
        if (pendingFieldName == null) {
            return;
        }
        ensureCurrentContextOpened();
        try {
            writer.writeStartElement(pendingFieldName);
            writer.writeCharacters(value);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw wrap(e);
        }
        pendingFieldName = null;
    }

    private void ensureCurrentContextOpened() throws SpeedyHttpException {
        if (stack.isEmpty()) {
            return;
        }
        XmlContext current = stack.peek();
        if (current.opened) {
            return;
        }
        ensureRootDocumentStarted();
        try {
            if (current.tagName != null) {
                writer.writeStartElement(current.tagName);
                current.opened = true;
            }
        } catch (XMLStreamException e) {
            throw wrap(e);
        }
    }

    private void ensureRootDocumentStarted() throws SpeedyHttpException {
        if (rootDocumentStarted) {
            return;
        }
        try {
            writer.writeStartDocument("UTF-8", "1.0");
            rootDocumentStarted = true;
        } catch (XMLStreamException e) {
            throw wrap(e);
        }
    }

    private static InternalServerError wrap(XMLStreamException e) {
        return new InternalServerError("Internal Server Error", e);
    }
}
