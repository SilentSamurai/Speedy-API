package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// A value that cannot fit the column it is stored in is rejected by core, from what the metamodel
/// declares, rather than by whichever database happens to enforce it — SQLite enforces neither a
/// VARCHAR length nor a numeric precision, since its type system is affinity-based. These are the
/// checks `ValidationProcessor` runs outside the replaceable rule set.
///
/// Creating an over-long value is covered by `SpeedyPostTest.createBadException`; this covers the
/// other two write verbs, the unsized-text exception, and numeric precision.
///
/// `Category` is the entity to check it on: it registers a custom validator with
/// `replacesDefault = true`, so it is exactly the case where the width check must still run.
///
/// Both tests assert the *message*, not just the status. A strict database rejects an over-long
/// value on its own and the request is a 400 either way, so a status-only assertion would pass on
/// H2 with the core check removed — and prove nothing about the backend the check exists for.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SchemaConstraintTest {

    private static final String UPDATE_URL = SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.UPDATE.suffix();
    private static final int NAME_WIDTH = 250;
    private static final String EXPECTED_MESSAGE = "name must be at most " + NAME_WIDTH + " characters";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void patch_valueLongerThanTheColumn_returns400() throws Exception {
        mvc.perform(MockMvcRequestBuilders.patch(UPDATE_URL)
                        .content(renameTo("A".repeat(NAME_WIDTH + 1)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(EXPECTED_MESSAGE)));
    }

    @Test
    void put_valueLongerThanTheColumn_returns400() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put(UPDATE_URL)
                        .content(renameTo("A".repeat(NAME_WIDTH + 1)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(EXPECTED_MESSAGE)));
    }

    /// A `@Lob` column has no width to enforce: `@Column.length()` answers 255 for it just as it
    /// does for an undecorated column, and taking that number literally rejects exactly the values a
    /// TEXT/CLOB column exists to hold.
    @Test
    void create_longTextInAnUnsizedColumn_isAccepted() throws Exception {
        String content = "A".repeat(2000);

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("title", "lob-" + UUID.randomUUID());
        body.put("owner", "lob-owner");
        body.put("content", content);

        mvc.perform(MockMvcRequestBuilders.post(
                        SpeedyConstants.URI + "/Document/" + SpeedyEndpoint.CREATE.suffix())
                        .content("[" + body + "]")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());
    }

    /// `@Column(precision, scale)` is the schema's own statement of how many digits fit. It is not
    /// the same thing as `@Digits`, which is a business rule an application can replace — a value
    /// that does not fit the column is a storage fact, and SQLite stores it happily either way.
    ///
    /// The value here overruns the *scale*: `salary` holds two fraction digits and is given three.
    /// Overrunning the integer half would work identically, but needs a nine-figure salary to do it,
    /// and a bound that tight is a statement about the schema this entity does not want to make.
    @Test
    void create_moreDigitsThanTheColumnHolds_returns400() throws Exception {
        ObjectNode person = annotatedPerson();
        person.put("salary", 1234.567);

        mvc.perform(MockMvcRequestBuilders.post(
                        SpeedyConstants.URI + "/AnnotatedPerson/" + SpeedyEndpoint.CREATE.suffix())
                        .content("[" + person + "]")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("salary must fit numeric(10,2)")));
    }

    @Test
    void create_digitsWithinTheColumn_isAccepted() throws Exception {
        ObjectNode person = annotatedPerson();
        person.put("salary", 1234.56);

        mvc.perform(MockMvcRequestBuilders.post(
                        SpeedyConstants.URI + "/AnnotatedPerson/" + SpeedyEndpoint.CREATE.suffix())
                        .content("[" + person + "]")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());
    }

    /// Every field AnnotatedPerson requires, all within their rules — the test then moves only the
    /// one value it is about.
    private static ObjectNode annotatedPerson() {
        ObjectNode person = CommonUtil.json().createObjectNode();
        person.put("name", "Precision Person");
        person.put("age", 30);
        person.put("email", "precision@example.com");
        person.put("code", "ABC12");
        person.put("salary", 1000);
        person.put("score", 5);
        person.put("debt", -10);
        person.put("overdraft", 0);
        person.put("rating", 1);
        person.put("precisionVal", 123.45);
        return person;
    }

    private String renameTo(String name) {
        Category category = new Category();
        category.setName("width-" + UUID.randomUUID());
        String id = categoryRepository.save(category).getId();

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("id", id);
        body.put("name", name);
        return body.toString();
    }
}
