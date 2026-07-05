package com.github.silent.samurai.speedy.xml.request;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import com.github.silent.samurai.speedy.models.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.github.silent.samurai.speedy.utils.ValueTypeUtil.*;

public class XmlStructureReader implements StructureReader {

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newFactory();

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    private final List<Event> events;
    private final int[] depths;
    private int pos;
    private int rootDepth;
    private int arrayWrapperPos = -1;
    private int arrayItemDepth = -1;
    private int lastReturnedKeyPos = -1;
    private int lastReturnedFieldPos = -1;

    private static final class Event {
        final EventType type;
        final String name;
        final String text;

        Event(EventType type, String name, String text) {
            this.type = type;
            this.name = name;
            this.text = text;
        }
    }

    private enum EventType { START_ELEMENT, END_ELEMENT, TEXT, END_DOCUMENT }

    public static XmlStructureReader over(byte[] rawBody) throws SpeedyHttpException {
        List<Event> events = new ArrayList<>();
        StringBuilder textBuf = new StringBuilder();
        try {
            XMLStreamReader r = XML_INPUT_FACTORY.createXMLStreamReader(new ByteArrayInputStream(rawBody));
            while (r.hasNext()) {
                int et = r.next();
                switch (et) {
                    case XMLStreamReader.START_ELEMENT:
                        flushText(textBuf, events);
                        events.add(new Event(EventType.START_ELEMENT, r.getLocalName(), null));
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        flushText(textBuf, events);
                        events.add(new Event(EventType.END_ELEMENT, r.getLocalName(), null));
                        break;
                    case XMLStreamReader.CHARACTERS:
                    case XMLStreamReader.CDATA:
                    case XMLStreamReader.SPACE:
                        textBuf.append(r.getText());
                        break;
                    case XMLStreamReader.END_DOCUMENT:
                        flushText(textBuf, events);
                        events.add(new Event(EventType.END_DOCUMENT, null, null));
                        break;
                }
            }
            flushText(textBuf, events);
        } catch (XMLStreamException e) {
            throw new BadRequestException("Invalid XML body", e);
        }
        return new XmlStructureReader(events);
    }

    private static void flushText(StringBuilder buf, List<Event> events) {
        if (buf.length() > 0) {
            events.add(new Event(EventType.TEXT, null, buf.toString()));
            buf.setLength(0);
        }
    }

    private XmlStructureReader(List<Event> events) {
        this.events = events;
        this.depths = computeDepths(events);
        this.pos = 0;
    }

    private static int[] computeDepths(List<Event> events) {
        int[] d = new int[events.size()];
        int depth = 0;
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            if (e.type == EventType.START_ELEMENT) {
                depth++;
            }
            d[i] = depth;
            if (e.type == EventType.END_ELEMENT) {
                depth--;
            }
        }
        return d;
    }

    @Override
    public Kind begin() throws SpeedyHttpException {
        pos = 0;
        lastReturnedKeyPos = -1;
        lastReturnedFieldPos = -1;
        arrayWrapperPos = -1;
        arrayItemDepth = -1;
        while (pos < events.size() && events.get(pos).type != EventType.START_ELEMENT) {
            pos++;
        }
        if (pos >= events.size()) {
            return null;
        }
        rootDepth = depths[pos];
        String firstChildName = null;
        boolean allSame = true;
        int childCount = 0;
        boolean hasStructuralChildren = false;
        int p = pos + 1;
        while (p < events.size()) {
            Event e = events.get(p);
            if (e.type == EventType.END_ELEMENT && depths[p] == rootDepth) {
                break;
            }
            if (e.type == EventType.START_ELEMENT && depths[p] == rootDepth + 1) {
                if (firstChildName == null) {
                    firstChildName = e.name;
                    childCount = 1;
                    // check if this first child contains nested elements
                    int cp = p + 1;
                    while (cp < events.size()) {
                        Event ce = events.get(cp);
                        if (ce.type == EventType.END_ELEMENT && depths[cp] == depths[p]) {
                            break;
                        }
                        if (ce.type == EventType.START_ELEMENT && depths[cp] == depths[p] + 1) {
                            hasStructuralChildren = true;
                            break;
                        }
                        cp++;
                    }
                } else if (firstChildName.equals(e.name)) {
                    childCount++;
                } else {
                    allSame = false;
                    break;
                }
            }
            p++;
        }
        if (childCount > 0 && allSame && hasStructuralChildren) {
            arrayWrapperPos = pos;
            return Kind.ARRAY;
        }
        return Kind.OBJECT;
    }

    @Override
    public Kind currentKind() throws SpeedyHttpException {
        if (pos >= events.size()) {
            return null;
        }
        Event e = events.get(pos);
        if (e.type != EventType.START_ELEMENT) {
            throw new BadRequestException("Invalid XML structure");
        }

        int currentPos = pos;
        int elementDepth = depths[currentPos];
        String elementName = e.name;

        boolean hasText = false;
        boolean hasChildren = false;
        String firstChildName = null;
        int childCount = 0;

        int p = currentPos + 1;
        while (p < events.size()) {
            Event ev = events.get(p);
            int d = depths[p];
            if (ev.type == EventType.END_ELEMENT && d == elementDepth && ev.name.equals(elementName)) {
                break;
            }
            if (ev.type == EventType.TEXT && d == elementDepth) {
                if (!ev.text.strip().isEmpty()) {
                    hasText = true;
                }
            } else if (ev.type == EventType.START_ELEMENT && d == elementDepth + 1) {
                hasChildren = true;
                String name = ev.name;
                if (firstChildName == null) {
                    firstChildName = name;
                    childCount = 1;
                } else if (firstChildName.equals(name)) {
                    childCount++;
                }
            }
            p++;
        }

        if (childCount > 1) {
            arrayWrapperPos = currentPos;
            return Kind.ARRAY;
        }

        int afterEnd = p + 1;
        while (afterEnd < events.size()) {
            Event after = events.get(afterEnd);
            if (after.type == EventType.START_ELEMENT && depths[afterEnd] == elementDepth
                    && after.name.equals(elementName)) {
                return Kind.ARRAY;
            }
            if (after.type == EventType.END_ELEMENT && depths[afterEnd] < elementDepth) {
                break;
            }
            afterEnd++;
        }

        if (!hasText && !hasChildren) {
            return Kind.NULL;
        }
        if (hasText && !hasChildren) {
            return Kind.VALUE;
        }
        return Kind.OBJECT;
    }

    @Override
    public FieldMetadata nextField(EntityMetadata entityMetadata) throws SpeedyHttpException {
        // --- Container entry detection ---
        // A prior call may have left pos sitting on the START_ELEMENT of a container
        // that hasn't been entered yet: the root element, an array item positioned by
        // nextElement(), or a nested field returned by a previous nextField() call that
        // the caller wants to walk into (rather than consume via readField/skipValue).
        // In all of those cases we must descend into it instead of treating it as a sibling.
        Event cur = events.get(pos);
        boolean shouldEnter = cur.type == EventType.START_ELEMENT
                && (depths[pos] == rootDepth || pos == lastReturnedFieldPos);
        int containerDepth = shouldEnter ? depths[pos] : depths[pos] - 1;

        if (shouldEnter) {
            pos++;
        }
        while (pos < events.size()) {
            Event e = events.get(pos);
            if (e.type == EventType.END_ELEMENT && depths[pos] == containerDepth) {
                pos++;
                lastReturnedFieldPos = -1;
                return null;
            }
            if (e.type == EventType.END_DOCUMENT) {
                return null;
            }
            if (e.type == EventType.START_ELEMENT && depths[pos] == containerDepth + 1) {
                if (entityMetadata.has(e.name)) {
                    lastReturnedFieldPos = pos;
                    return entityMetadata.field(e.name);
                }
                skipCurrentSubtree();
                continue;
            }
            pos++;
        }
        return null;
    }

    @Override
    public String nextKey() throws SpeedyHttpException {
        // --- Container entry detection ---
        // When the caller receives a key from nextKey() and then calls nextKey()
        // again without consuming the element (no readField/skipValue etc.),
        // pos hasn't changed and we must enter that element as a new container.
        if (lastReturnedKeyPos >= 0 && pos == lastReturnedKeyPos
                && events.get(pos).type == EventType.START_ELEMENT) {
            int containerDepth = depths[pos];
            pos++; // advance past the container element's START_ELEMENT
            while (pos < events.size()) {
                Event e = events.get(pos);
                if (e.type == EventType.END_ELEMENT && depths[pos] == containerDepth) {
                    pos++;
                    break; // empty or scalar container — fall through to sibling iteration
                }
                if (e.type == EventType.START_ELEMENT && depths[pos] == containerDepth + 1) {
                    lastReturnedKeyPos = pos;
                    return e.name;
                }
                if (e.type == EventType.END_DOCUMENT) {
                    return null;
                }
                pos++;
            }
            // Scalar/empty — continue to normal sibling iteration at the parent level
        }

        // --- Normal sibling iteration ---
        Event cur = events.get(pos);
        boolean atRoot = cur.type == EventType.START_ELEMENT && depths[pos] == rootDepth;
        int containerDepth = atRoot ? rootDepth : depths[pos] - 1;

        if (atRoot) {
            pos++;
        }

        while (pos < events.size()) {
            Event e = events.get(pos);
            if (e.type == EventType.END_ELEMENT && depths[pos] == containerDepth) {
                pos++;
                lastReturnedKeyPos = -1;
                return null;
            }
            if (e.type == EventType.END_DOCUMENT) {
                return null;
            }
            if (e.type == EventType.START_ELEMENT) {
                if (depths[pos] == containerDepth + 1) {
                    String name = e.name;
                    lastReturnedKeyPos = pos;
                    return name;
                }
                if (depths[pos] > containerDepth + 1) {
                    skipCurrentSubtree();
                    continue;
                }
            }
            pos++;
        }
        return null;
    }

    @Override
    public Kind nextElement() throws SpeedyHttpException {
        lastReturnedKeyPos = -1;
        if (pos == arrayWrapperPos) {
            arrayWrapperPos = -1;
            pos++;
            while (pos < events.size() && events.get(pos).type != EventType.START_ELEMENT) {
                pos++;
            }
            if (pos >= events.size()) {
                return null;
            }
            arrayItemDepth = depths[pos];
            lastReturnedFieldPos = pos;
            return Kind.OBJECT;
        }

        if (pos < events.size() && events.get(pos).type == EventType.START_ELEMENT
                && depths[pos] == arrayItemDepth) {
            lastReturnedFieldPos = pos;
            return Kind.OBJECT;
        }

        while (pos < events.size()) {
            Event e = events.get(pos);
            if (e.type == EventType.START_ELEMENT && depths[pos] == arrayItemDepth) {
                lastReturnedFieldPos = pos;
                return Kind.OBJECT;
            }
            if (e.type == EventType.END_ELEMENT && depths[pos] < arrayItemDepth) {
                return null;
            }
            pos++;
        }
        return null;
    }

    @Override
    public SpeedyValue readField(FieldMetadata field) throws SpeedyHttpException {
        int elementDepth = depths[pos];
        String elementName = events.get(pos).name;

        StringBuilder textContent = new StringBuilder();
        pos++;
        while (pos < events.size()) {
            Event e = events.get(pos);
            if (e.type == EventType.TEXT && depths[pos] == elementDepth) {
                textContent.append(e.text);
            }
            if (e.type == EventType.END_ELEMENT && depths[pos] == elementDepth && e.name.equals(elementName)) {
                break;
            }
            if (e.type == EventType.START_ELEMENT) {
                skipToEndElement(depths[pos], e.name);
                continue;
            }
            pos++;
        }

        String raw = textContent.toString().strip();
        if (raw.isEmpty()) {
            return SpeedyNull.SPEEDY_NULL;
        }

        ValueType type = field.getValueType();
        return switch (type) {
            case ENUM -> new SpeedyEnum(raw, field);
            case ENUM_ORD -> {
                try {
                    yield new SpeedyEnum(Long.parseLong(raw), field);
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("expected number for ordinal enum field " + field.getOutputPropertyName());
                }
            }
            case DATE -> {
                if (!isDateFormatValid(raw)) {
                    throw new BadRequestException(String.format("Date value must be a string with ISO_DATE(%s) format",
                            LocalDate.now().format(DateTimeFormatter.ISO_DATE)));
                }
                yield new SpeedyDate(LocalDate.parse(raw, DateTimeFormatter.ISO_DATE));
            }
            case TIME -> {
                if (!isTimeFormatValid(raw)) {
                    throw new BadRequestException(String.format("Time value must be a string with ISO_TIME(%s) format",
                            LocalTime.now().format(DateTimeFormatter.ISO_TIME)));
                }
                yield new SpeedyTime(LocalTime.parse(raw, DateTimeFormatter.ISO_TIME));
            }
            case DATE_TIME -> {
                if (!isDateTimeFormatValid(raw)) {
                    throw new BadRequestException(String.format("DateTime value must be a string with ISO_DATE_TIME(%s) format",
                            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
                }
                yield new SpeedyDateTime(LocalDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME));
            }
            case ZONED_DATE_TIME -> {
                if (!isZonedDateTimeValid(raw)) {
                    throw new BadRequestException(String.format("ZonedDateTime value must be a string with ISO_ZONED_DATE_TIME(%s) format",
                            ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
                }
                yield new SpeedyZonedDateTime(ZonedDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
            case BOOL -> {
                String lower = raw.toLowerCase();
                yield new SpeedyBoolean("true".equals(lower) || "1".equals(lower));
            }
            case TEXT -> new SpeedyText(raw);
            case INT -> {
                try {
                    yield new SpeedyInt(Long.parseLong(raw));
                } catch (NumberFormatException ex) {
                    throw new BadRequestException(String.format(
                            "Not able to parse field %s with value type %s",
                            field.getOutputPropertyName(), field.getColumnType()));
                }
            }
            case FLOAT -> {
                try {
                    yield new SpeedyDouble(Double.parseDouble(raw));
                } catch (NumberFormatException ex) {
                    throw new BadRequestException(String.format(
                            "Not able to parse field %s with value type %s",
                            field.getOutputPropertyName(), field.getColumnType()));
                }
            }
            case NULL -> SpeedyNull.SPEEDY_NULL;
            case OBJECT, COLLECTION -> throw new BadRequestException(String.format(
                    "Not able to parse field %s with value type %s",
                    field.getOutputPropertyName(), field.getColumnType()));
        };
    }

    @Override
    public String textValue() throws SpeedyHttpException {
        if (pos >= events.size()) {
            return null;
        }
        int elementDepth = depths[pos];
        int p = pos + 1;
        StringBuilder sb = new StringBuilder();
        while (p < events.size()) {
            Event e = events.get(p);
            if (e.type == EventType.END_ELEMENT && depths[p] == elementDepth) {
                break;
            }
            if (e.type == EventType.TEXT && depths[p] == elementDepth) {
                sb.append(e.text);
            }
            if (e.type == EventType.START_ELEMENT) {
                break;
            }
            p++;
        }
        String result = sb.toString().strip();
        return result.isEmpty() ? null : result;
    }

    @Override
    public int intValue() throws SpeedyHttpException {
        String val = textValue();
        if (val == null) {
            throw new BadRequestException("expected integer value");
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new BadRequestException("expected integer value, got: " + val);
        }
    }

    @Override
    public boolean boolValue() throws SpeedyHttpException {
        String val = textValue();
        if (val == null) {
            throw new BadRequestException("expected boolean value");
        }
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Override
    public boolean isBoolValue() throws SpeedyHttpException {
        String val = textValue();
        if (val == null) {
            return false;
        }
        String lower = val.toLowerCase();
        return "true".equals(lower) || "false".equals(lower) || "1".equals(val) || "0".equals(val);
    }

    @Override
    public void skipValue() throws SpeedyHttpException {
        if (pos >= events.size()) {
            return;
        }
        skipCurrentSubtree();
    }

    @Override
    public void close() {
    }

    private void skipCurrentSubtree() {
        int elementDepth = depths[pos];
        String elementName = events.get(pos).name;
        pos++;
        while (pos < events.size()) {
            Event e = events.get(pos);
            if (e.type == EventType.END_ELEMENT && depths[pos] == elementDepth && e.name.equals(elementName)) {
                pos++;
                return;
            }
            pos++;
        }
    }

    private void skipToEndElement(int targetDepth, String targetName) {
        while (pos < events.size()) {
            Event e = events.get(pos);
            pos++;
            if (e.type == EventType.END_ELEMENT && depths[pos - 1] == targetDepth && e.name.equals(targetName)) {
                return;
            }
        }
    }
}
