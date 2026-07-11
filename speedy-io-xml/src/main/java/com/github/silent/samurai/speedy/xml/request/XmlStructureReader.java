package com.github.silent.samurai.speedy.xml.request;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.github.silent.samurai.speedy.xml.request.SpeedyValueDecoder;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// XML {@link StructureReader}. XML has no native arrays and no streaming array/object markers,
/// so — unlike JSON/YAML — the whole body is parsed once into an immutable element tree and the
/// token protocol is served by walking it with an explicit frame stack. Since the document is
/// fully materialised there is no streaming to preserve; the tree keeps navigation to one
/// {@link #kindOf} inference and one {@link #advanceEntry} descent rule instead of a hand-rolled
/// index/depth cursor.
///
/// ## Array shapes
/// XML expresses a "list" three ways, all handled by {@link #kindOf}:
/// - **sibling form** — repeated same-name children (`<tag>a</tag><tag>b</tag>`); grouping by
///   name yields one value whose group size is > 1.
/// - **wrapper form** — a single container element whose children are all the same name
///   (`<tags><item>a</item><item>b</item></tags>`).
/// - **root form** — the document element wrapping repeated structural children
///   (`<root><entity>…</entity><entity>…</entity></root>`).
///
/// Single-item wrapper/sibling arrays are otherwise indistinguishable from a plain object, so two
/// hints disambiguate: the reserved item element names `entity`/`item` (the wire markers the
/// client emits for array items — see the client {@code XmlFormat}) and, for entity fields, the
/// {@link FieldMetadata#isCollection()} flag carried in from {@link #nextField}.
public class XmlStructureReader implements StructureReader {

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newFactory();

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    /// An element of the parsed tree. {@code text} is the element's direct character content,
    /// stripped once at build time; text inside child elements belongs to those children.
    /// Attributes are ignored (parity with the streaming JSON/YAML readers, which have none).
    private record XmlElement(String name, String text, List<XmlElement> children) {
        boolean hasChildren() {
            return !children.isEmpty();
        }
    }

    private final XmlElement root;
    private final Deque<Frame> stack = new ArrayDeque<>();

    /// The current unconsumed value token, or {@code null}. A single-element list is one value
    /// (object / scalar / null / wrapper-array); a multi-element list is a sibling-form array.
    private List<XmlElement> pending;
    private boolean pendingIsRoot;
    private boolean pendingCollectionHint;

    private sealed interface Frame permits ObjectFrame, ArrayFrame {
    }

    private record ObjectFrame(Iterator<Map.Entry<String, List<XmlElement>>> fields) implements Frame {
    }

    private record ArrayFrame(Iterator<XmlElement> items) implements Frame {
    }

    private XmlStructureReader(XmlElement root) {
        this.root = root;
    }

    /// Parses the whole body into an element tree — the {@code byte[] -> StructureReader} factory
    /// the provider hands to the shared request parser.
    public static XmlStructureReader over(byte[] rawBody) throws SpeedyHttpException {
        Deque<Builder> builders = new ArrayDeque<>();
        XmlElement root = null;
        try {
            XMLStreamReader r = XML_INPUT_FACTORY.createXMLStreamReader(new ByteArrayInputStream(rawBody));
            while (r.hasNext()) {
                switch (r.next()) {
                    case XMLStreamConstants.START_ELEMENT -> builders.push(new Builder(r.getLocalName()));
                    case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA, XMLStreamConstants.SPACE -> {
                        if (!builders.isEmpty()) {
                            builders.peek().text.append(r.getText());
                        }
                    }
                    case XMLStreamConstants.END_ELEMENT -> {
                        Builder done = builders.pop();
                        XmlElement element = new XmlElement(done.name, done.text.toString().strip(), done.children);
                        if (builders.isEmpty()) {
                            root = element; // the document element
                        } else {
                            builders.peek().children.add(element);
                        }
                    }
                    default -> {
                        // comments, processing instructions, whitespace, the XML declaration — ignored
                    }
                }
            }
            r.close();
        } catch (XMLStreamException e) {
            throw new BadRequestException("Invalid XML body", e);
        }
        return new XmlStructureReader(root);
    }

    private static final class Builder {
        final String name;
        final StringBuilder text = new StringBuilder();
        final List<XmlElement> children = new ArrayList<>();

        Builder(String name) {
            this.name = name;
        }
    }

    @Override
    public Kind begin() throws SpeedyHttpException {
        stack.clear();
        clearPending();
        if (root == null) {
            return null;
        }
        pending = List.of(root);
        pendingIsRoot = true;
        return kindOf(pending, true, false);
    }

    @Override
    public Kind currentKind() throws SpeedyHttpException {
        if (pending == null) {
            throw new BadRequestException("Invalid XML structure");
        }
        return kindOf(pending, pendingIsRoot, pendingCollectionHint);
    }

    @Override
    public FieldMetadata nextField(EntityMetadata entityMetadata) throws SpeedyHttpException {
        Map.Entry<String, List<XmlElement>> entry;
        while ((entry = advanceEntry()) != null) {
            if (entityMetadata.has(entry.getKey())) {
                FieldMetadata field = entityMetadata.field(entry.getKey());
                pending = entry.getValue();
                pendingIsRoot = false;
                pendingCollectionHint = field.isCollection();
                return field;
            }
            // Unknown to the metadata — a tree has no token to skip; simply don't visit it.
        }
        return null;
    }

    @Override
    public String nextKey() throws SpeedyHttpException {
        Map.Entry<String, List<XmlElement>> entry = advanceEntry();
        if (entry == null) {
            return null;
        }
        pending = entry.getValue();
        pendingIsRoot = false;
        pendingCollectionHint = false;
        return entry.getKey();
    }

    @Override
    public Kind nextElement() throws SpeedyHttpException {
        if (pending != null) {
            if (kindOf(pending, pendingIsRoot, pendingCollectionHint) == Kind.ARRAY) {
                List<XmlElement> items = pending.size() > 1 ? pending : pending.get(0).children();
                clearPending();
                stack.push(new ArrayFrame(items.iterator()));
            } else {
                clearPending(); // a leftover peeked scalar (e.g. a $select/$expand entry) — drop it
            }
        }
        // Moving to the next element abandons the current one: unwind any object frames opened
        // while walking it, back to the enclosing array frame.
        while (!stack.isEmpty() && !(stack.peek() instanceof ArrayFrame)) {
            stack.pop();
        }
        if (!(stack.peek() instanceof ArrayFrame frame)) {
            return null;
        }
        if (!frame.items().hasNext()) {
            stack.pop();
            return null;
        }
        pending = List.of(frame.items().next());
        pendingIsRoot = false;
        pendingCollectionHint = false;
        return kindOf(pending, false, false);
    }

    @Override
    public SpeedyValue readField(FieldMetadata field) throws SpeedyHttpException {
        String raw = pending == null || pending.isEmpty() ? "" : pending.get(0).text();
        clearPending();
        if (raw.isEmpty()) {
            return SpeedyNull.SPEEDY_NULL;
        }
        return SpeedyValueDecoder.fromString(field, raw);
    }

    @Override
    public String textValue() throws SpeedyHttpException {
        if (pending == null || pending.size() != 1) {
            return null;
        }
        XmlElement element = pending.get(0);
        if (element.hasChildren()) {
            return null;
        }
        return element.text().isEmpty() ? null : element.text();
    }

    @Override
    public int intValue() throws SpeedyHttpException {
        String value = textValue();
        if (value == null) {
            throw new BadRequestException("expected integer value");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException("expected integer value, got: " + value);
        }
    }

    @Override
    public boolean boolValue() throws SpeedyHttpException {
        String value = textValue();
        if (value == null) {
            throw new BadRequestException("expected boolean value");
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    @Override
    public boolean isBoolValue() throws SpeedyHttpException {
        String value = textValue();
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase();
        return "true".equals(lower) || "false".equals(lower) || "1".equals(value) || "0".equals(value);
    }

    @Override
    public void skipValue() throws SpeedyHttpException {
        clearPending();
    }

    @Override
    public void close() {
    }

    /// Pulls the next object entry, first resolving any unconsumed {@link #pending} value:
    /// an object (or the document root) is descended into — this is how the walker walks into an
    /// association / condition object it was just handed — while any other leftover value is
    /// dropped. Returns {@code null} at the end of the current object, popping its frame so the
    /// parent object resumes on the following call.
    private Map.Entry<String, List<XmlElement>> advanceEntry() throws SpeedyHttpException {
        if (pending != null) {
            boolean descend = pendingIsRoot || kindOf(pending, pendingIsRoot, pendingCollectionHint) == Kind.OBJECT;
            if (descend && pending.size() == 1) {
                stack.push(new ObjectFrame(grouped(pending.get(0)).entrySet().iterator()));
            }
            clearPending();
        }
        if (!(stack.peek() instanceof ObjectFrame frame)) {
            return null;
        }
        if (frame.fields().hasNext()) {
            return frame.fields().next();
        }
        stack.pop();
        return null;
    }

    /// Classifies a value token. A group of more than one element is a sibling-form array; a
    /// single childless element is a scalar (or null when empty). A single element with children
    /// is an array when its children are a uniform list — repeated, or marked as array items by
    /// name or by the collection hint — and an object otherwise. The document root keeps the
    /// original name-agnostic rule (uniform structural children ⇒ array) so a non-{@code entity}
    /// create body still parses as an array.
    private static Kind kindOf(List<XmlElement> group, boolean isRoot, boolean collectionHint) {
        if (group.size() > 1) {
            return Kind.ARRAY;
        }
        XmlElement element = group.get(0);
        if (!element.hasChildren()) {
            return element.text().isEmpty() ? Kind.NULL : Kind.VALUE;
        }
        List<XmlElement> children = element.children();
        if (!allSameName(children)) {
            return Kind.OBJECT;
        }
        if (isRoot) {
            return children.get(0).hasChildren() ? Kind.ARRAY : Kind.OBJECT;
        }
        boolean isArray = children.size() > 1 || isArrayItemName(children.get(0).name()) || collectionHint;
        return isArray ? Kind.ARRAY : Kind.OBJECT;
    }

    private static boolean allSameName(List<XmlElement> children) {
        String first = children.get(0).name();
        for (XmlElement child : children) {
            if (!first.equals(child.name())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isArrayItemName(String name) {
        return "entity".equals(name) || "item".equals(name);
    }

    private static Map<String, List<XmlElement>> grouped(XmlElement element) {
        Map<String, List<XmlElement>> groups = new LinkedHashMap<>();
        for (XmlElement child : element.children()) {
            groups.computeIfAbsent(child.name(), k -> new ArrayList<>()).add(child);
        }
        return groups;
    }

    private void clearPending() {
        pending = null;
        pendingIsRoot = false;
        pendingCollectionHint = false;
    }
}
