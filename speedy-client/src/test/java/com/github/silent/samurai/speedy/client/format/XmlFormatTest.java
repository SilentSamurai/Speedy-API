package com.github.silent.samurai.speedy.client.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.client.SpeedyResult;
import com.github.silent.samurai.speedy.client.exception.SpeedyBadRequestException;
import com.github.silent.samurai.speedy.client.exception.SpeedyDeserializationException;
import com.github.silent.samurai.speedy.client.exception.SpeedyException;
import com.github.silent.samurai.speedy.client.internal.ResponseParser;
import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XmlFormatTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final XmlFormat format = new XmlFormat();

    // ─── write ──────────────────────────────────────────────────────────

    @Test
    void entityArrayShouldWrapItemsInEntityElements() {
        ObjectNode entity = mapper.createObjectNode();
        entity.put("name", "Widget");
        entity.put("cost", 12);
        ArrayNode body = mapper.createArrayNode();
        body.add(entity);

        String xml = format.write(body);

        assertTrue(xml.contains("<root><entity><name>Widget</name><cost>12</cost></entity></root>"), xml);
    }

    @Test
    void queryTreeShouldStripDollarPrefixes() {
        ObjectNode query = mapper.createObjectNode();
        query.put("$from", "Product");
        query.putObject("$where").putObject("name").put("$eq", "A&B");
        ObjectNode page = query.putObject("$page");
        page.put("$index", 0);
        page.put("$size", 10);

        String xml = format.write(query);

        assertTrue(xml.contains("<from>Product</from>"), xml);
        assertTrue(xml.contains("<where><name><eq>A&amp;B</eq></name></where>"), xml);
        assertTrue(xml.contains("<page><index>0</index><size>10</size></page>"), xml);
    }

    @Test
    void scalarArraysShouldUseItemElements() {
        ObjectNode query = mapper.createObjectNode();
        query.putArray("$select").add("id").add("name");

        String xml = format.write(query);

        assertTrue(xml.contains("<select><item>id</item><item>name</item></select>"), xml);
    }

    @Test
    void nullFieldShouldBecomeEmptyElement() {
        ObjectNode entity = mapper.createObjectNode();
        entity.putNull("description");
        entity.put("name", "Widget");

        String xml = format.write(entity);

        assertTrue(xml.contains("<description/>"), xml);
    }

    // ─── read ───────────────────────────────────────────────────────────

    @Test
    void repeatedPayloadEntitiesShouldBecomeArray() {
        String xml = "<response><payload>"
                + "<Category><id>1</id><name>a</name></Category>"
                + "<Category><id>2</id><name>b</name></Category>"
                + "</payload><pageIndex>0</pageIndex><pageSize>10</pageSize>"
                + "<totalCount>2</totalCount><totalPages>1</totalPages></response>";

        JsonNode root = format.read(xml);

        assertTrue(root.get("payload").isArray());
        assertEquals(2, root.get("payload").size());
        assertEquals("a", root.get("payload").get(0).get("name").asText());
        assertEquals(2, root.get("totalCount").asLong());
    }

    @Test
    void singlePayloadEntityShouldBecomeOneElementArray() {
        String xml = "<response><payload><Category><id>1</id><name>a</name></Category></payload></response>";

        JsonNode root = format.read(xml);

        assertTrue(root.get("payload").isArray());
        assertEquals(1, root.get("payload").size());
        assertEquals("1", root.get("payload").get(0).get("id").asText());
    }

    @Test
    void singleAssociationObjectShouldStayObject() {
        String xml = "<response><payload><Product><id>1</id>"
                + "<category><id>9</id><name>c</name></category>"
                + "</Product></payload></response>";

        JsonNode root = format.read(xml);

        JsonNode product = root.get("payload").get(0);
        assertTrue(product.get("category").isObject());
        assertEquals("9", product.get("category").get("id").asText());
    }

    @Test
    void expandedCollectionShouldBecomeNestedArray() {
        String xml = "<response><payload><Category><id>1</id>"
                + "<products><Product><id>7</id><name>x</name></Product><Product><id>8</id><name>y</name></Product></products>"
                + "</Category></payload></response>";

        JsonNode root = format.read(xml);

        JsonNode category = root.get("payload").get(0);
        assertTrue(category.get("products").isArray());
        assertEquals(2, category.get("products").size());
    }

    @Test
    void invalidXmlShouldThrowDeserializationException() {
        assertThrows(SpeedyDeserializationException.class, () -> format.read("{\"payload\":[]}"));
    }

    // ─── through ResponseParser ─────────────────────────────────────────

    @Test
    void parserShouldReadXmlEnvelope() {
        ResponseParser parser = new ResponseParser(mapper, format);
        String xml = "<response><payload><Category><id>1</id></Category></payload>"
                + "<pageIndex>0</pageIndex><pageSize>10</pageSize><totalCount>1</totalCount><totalPages>1</totalPages></response>";

        SpeedyResult result = parser.parseEntityResponse(new SpeedyRawResponse(200, Map.of(), xml));

        assertEquals(1, result.size());
        assertEquals(1, result.totalCount());
    }

    @Test
    void parserShouldReadXmlCount() {
        ResponseParser parser = new ResponseParser(mapper, format);
        String xml = "<response><count>42</count></response>";

        assertEquals(42, parser.parseCountResponse(new SpeedyRawResponse(200, Map.of(), xml)));
    }

    @Test
    void parserShouldReadXmlError() {
        ResponseParser parser = new ResponseParser(mapper, format);
        String xml = "<response><message>bad input</message><timestamp>now</timestamp></response>";

        SpeedyException ex = parser.parseError(new SpeedyRawResponse(400, Map.of(), xml));

        assertInstanceOf(SpeedyBadRequestException.class, ex);
        assertEquals("bad input", ex.serverMessage());
    }

    @Test
    void parserShouldFallBackToJsonErrorBody() {
        // Errors thrown before negotiation are serialized with the server's JSON baseline.
        ResponseParser parser = new ResponseParser(mapper, format);
        String json = "{\"status\":400,\"message\":\"bad input\",\"timestamp\":\"now\"}";

        SpeedyException ex = parser.parseError(new SpeedyRawResponse(400, Map.of(), json));

        assertEquals("bad input", ex.serverMessage());
    }
}
