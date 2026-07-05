package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.silent.samurai.speedy.enums.SpeedyEndpoint.QUERY;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Tests error messages for invalid query operator usage via the $query endpoint.
///
/// ## What we test
/// - $matches operator on non-String fields (Integer, Double, Boolean, DateTime)
/// - $in/$nin operators on association fields
/// - $eq/$ne/$lt/$gt/$lte/$gte with object/array values on scalar fields
///
/// ## How we test
/// Integration tests via MockMvc POSTing JSON query bodies to /speedy/v1/{Entity}/$query
/// and asserting 400 Bad Request with specific error messages.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyV2OperatorErrorsTest {

    @Autowired
    MockMvc mvc;

    private ObjectNode queryBody(String entity, ObjectNode where) {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", entity);
        body.set("$where", where);
        return body;
    }

    private void postQuery(ObjectNode body) throws Exception {
        mvc.perform(post(SpeedyConstants.URI + "/" + body.get("$from").asText() + "/" + QUERY.suffix())
                        .content(CommonUtil.json().writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private void postQueryWithMessage(ObjectNode body, String message) throws Exception {
        mvc.perform(post(SpeedyConstants.URI + "/" + body.get("$from").asText() + "/" + QUERY.suffix())
                        .content(CommonUtil.json().writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(message)));
    }

    /// --- Gap 34: $matches on non-string fields ---

    @Nested
    @DisplayName("$matches on non-string fields")
    class MatchesNonString {

        @Test
        @DisplayName("$matches on Integer field returns 400")
        void matchesOnInteger() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Company");
            ObjectNode where = body.putObject("$where");
            where.putObject("invoiceNo").put("$matches", "10*");
            postQueryWithMessage(body, "only text values are supported for $matches");
        }

        @Test
        @DisplayName("$matches on Double field returns 400")
        void matchesOnDouble() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Invoice");
            ObjectNode where = body.putObject("$where");
            where.putObject("paid").put("$matches", "100*");
            postQueryWithMessage(body, "only text values are supported for $matches");
        }

        @Test
        @DisplayName("$matches on Boolean field returns 400")
        void matchesOnBoolean() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "ValueTestEntity");
            ObjectNode where = body.putObject("$where");
            where.putObject("booleanValue").put("$matches", "true");
            postQueryWithMessage(body, "only text values are supported for $matches");
        }

        /// The parser coerces "2024*" via JsonStructureReader.readField which
        /// rejects it at the DateTime-ISO-format check before JooqQueryBuilder.matchPredicate
        /// ever runs. This produces a less-precise "must be ISO_DATE_TIME" error instead
        /// of "only text values are supported for $matches". The test reflects current behaviour.
        @Test
        @DisplayName("$matches on LocalDateTime field returns 400")
        void matchesOnDateTime() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Customer");
            ObjectNode where = body.putObject("$where");
            where.putObject("createdAt").put("$matches", "2024*");
            postQueryWithMessage(body, "DateTime value must be a string with ISO_DATE_TIME");
        }
    }

    /// --- Gap 35: $in / $nin with association fields ---
    ///
    /// The parser successfully builds the condition (text values are coerced
    /// via JsonStructureReader.readField), but JooqQueryBuilder.inPredicate /
    /// notInPredicate checks fieldMetadata.isAssociation() at query execution
    /// time and throws "COLLECTION of Association Operation not supported".

    @Nested
    @DisplayName("$in / $nin with association collection")
    class InNotInAssociation {

        @Test
        @DisplayName("$in on association with collection returns 400")
        void inOnAssociationCollection() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Product");
            ObjectNode where = body.putObject("$where");
            where.putObject("category")
                    .putArray("$in")
                    .add("cat-1-1")
                    .add("cat-2-2");
            postQueryWithMessage(body, "COLLECTION of Association Operation not supported");
        }

        @Test
        @DisplayName("$nin on association with collection returns 400")
        void ninOnAssociationCollection() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Product");
            ObjectNode where = body.putObject("$where");
            where.putObject("category")
                    .putArray("$nin")
                    .add("cat-1-1");
            postQueryWithMessage(body, "COLLECTION of Association Operation not supported");
        }
    }

    /// --- Gap 36: $eq / $ne / $lt / $gt / $lte / $gte with OBJECT/COLLECTION ---
    ///
    /// The query parser at StructureToQuery.captureOperatorCondition rejects operator values
    /// that are objects or arrays when the operator expects a scalar value (the value token is
    /// neither VALUE nor NULL), returning "Invalid query" before the JooqQueryBuilder predicate
    /// methods are reached.

    @Nested
    @DisplayName("$eq / $ne with OBJECT/COLLECTION")
    class EqNeObjectCollection {

        @Test
        @DisplayName("$eq on scalar field with array value returns 400")
        void eqWithCollectionOnScalar() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Category");
            ObjectNode where = body.putObject("$where");
            ObjectNode nameNode = where.putObject("name");
            nameNode.putArray("$eq").add("a").add("b");
            postQueryWithMessage(body, "Invalid query");
        }

        @Test
        @DisplayName("$eq on scalar field with object value returns 400")
        void eqWithObjectOnScalar() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Category");
            ObjectNode where = body.putObject("$where");
            ObjectNode nameNode = where.putObject("name");
            nameNode.putObject("$eq").put("id", "something");
            postQueryWithMessage(body, "Invalid query");
        }

        @Test
        @DisplayName("$ne on scalar field with array value returns 400")
        void neWithCollectionOnScalar() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Category");
            ObjectNode where = body.putObject("$where");
            ObjectNode nameNode = where.putObject("name");
            nameNode.putArray("$ne").add("a").add("b");
            postQueryWithMessage(body, "Invalid query");
        }

        @Test
        @DisplayName("$gt on numeric field with object value returns 400")
        void gtWithObjectOnScalar() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Invoice");
            ObjectNode where = body.putObject("$where");
            ObjectNode discountNode = where.putObject("discount");
            discountNode.putObject("$gt").put("id", "x");
            postQueryWithMessage(body, "Invalid query");
        }

        @Test
        @DisplayName("$lt on numeric field with array value returns 400")
        void ltWithCollectionOnScalar() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Invoice");
            ObjectNode where = body.putObject("$where");
            ObjectNode paidNode = where.putObject("paid");
            paidNode.putArray("$lt").add(10).add(20);
            postQueryWithMessage(body, "Invalid query");
        }

        @Test
        @DisplayName("$lte on numeric field with object value returns 400")
        void lteWithObject() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Invoice");
            ObjectNode where = body.putObject("$where");
            ObjectNode dueNode = where.putObject("dueAmount");
            dueNode.putObject("$lte").put("val", 50);
            postQueryWithMessage(body, "Invalid query");
        }

        @Test
        @DisplayName("$gte on numeric field with array value returns 400")
        void gteWithCollection() throws Exception {
            ObjectNode body = CommonUtil.json().createObjectNode();
            body.put("$from", "Invoice");
            ObjectNode where = body.putObject("$where");
            ObjectNode dueNode = where.putObject("dueAmount");
            dueNode.putArray("$gte").add(0).add(100);
            postQueryWithMessage(body, "Invalid query");
        }
    }
}
