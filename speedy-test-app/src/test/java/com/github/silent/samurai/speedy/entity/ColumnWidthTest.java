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

/// A value too long for its column is rejected by core, from the declared width in the metamodel,
/// rather than by whichever database happens to enforce it — SQLite enforces no VARCHAR length at
/// all. Create is covered by `SpeedyPostTest.createBadException`; this covers the other two write
/// verbs, which route through the same check in `ValidationProcessor`.
///
/// `Category` is the entity to check it on: it registers a custom validator with
/// `replacesDefault = true`, so it is exactly the case where the width check must still run.
///
/// Both tests assert the *message*, not just the status. A strict database rejects an over-long
/// value on its own and the request is a 400 either way, so a status-only assertion would pass on
/// H2 with the core check removed — and prove nothing about the backend the check exists for.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ColumnWidthTest {

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
