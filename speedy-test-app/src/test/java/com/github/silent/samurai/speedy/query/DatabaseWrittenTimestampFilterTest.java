package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.gte;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// A value a filter compares against is not always one Speedy wrote. `Currency.createdAt` is
/// `@Generated(ALWAYS)` over a column declared `TIMESTAMP DEFAULT CURRENT_TIMESTAMP`, so the
/// database writes it and Speedy only ever reads it back.
///
/// That matters on a backend with no temporal storage class. SQLite keeps these columns as text, and
/// its own `CURRENT_TIMESTAMP` writes `yyyy-MM-dd HH:mm:ss` while Speedy encodes a filter bound as
/// `yyyy-MM-dd HH:mm:ss.SSS`. Compared as text the stored value is a prefix of the bound and sorts
/// before it, so `$eq` matched nothing and `$gte` skipped the row — on a timestamp the caller had
/// just read out of that very row.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class DatabaseWrittenTimestampFilterTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void eqOnADatabaseWrittenTimestampFindsTheRow() throws Exception {
        String abbr = createCurrency();

        query(condition("currencyAbbr", eq(abbr)), condition("createdAt", eq(createdAtOf(abbr))))
                .andExpect(jsonPath("$.payload[*]", hasSize(1)));
    }

    @Test
    void gteOnADatabaseWrittenTimestampIncludesTheRow() throws Exception {
        String abbr = createCurrency();

        query(condition("currencyAbbr", eq(abbr)), condition("createdAt", gte(createdAtOf(abbr))))
                .andExpect(jsonPath("$.payload[*]", hasSize(greaterThanOrEqualTo(1))));
    }

    /// The timestamp as the caller sees it: read back from the row the database just wrote.
    private String createdAtOf(String abbr) throws Exception {
        String response = query(condition("currencyAbbr", eq(abbr)))
                .andExpect(jsonPath("$.payload[*]", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        JsonNode createdAt = CommonUtil.json().readTree(response).at("/payload/0/createdAt");
        assertNotNull(createdAt);
        return createdAt.asText();
    }

    private ResultActions query(JsonNode... conditions) throws Exception {
        JsonNode query = SpeedyQuery.from("Currency").where(conditions).build();
        return mvc.perform(MockMvcRequestBuilders.post(
                        SpeedyConstants.URI + "/Currency/" + SpeedyEndpoint.QUERY.suffix())
                        .content(CommonUtil.json().writeValueAsString(query))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    /// Creates a Currency and returns its abbreviation, which the queries use to find it again.
    /// `createdAt` is deliberately not supplied — the column default is what writes it.
    private String createCurrency() throws Exception {
        String abbr = UUID.randomUUID().toString().substring(0, 8);

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("currencyName", "Generated " + abbr);
        body.put("currencySymbol", "G");
        body.put("currencyAbbr", abbr);
        body.put("country", "Testland");

        mvc.perform(MockMvcRequestBuilders.post(
                        SpeedyConstants.URI + "/Currency/" + SpeedyEndpoint.CREATE.suffix())
                        .content("[" + body + "]")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        return abbr;
    }
}
