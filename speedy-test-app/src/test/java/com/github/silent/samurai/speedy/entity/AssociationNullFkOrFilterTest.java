package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.or;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reproduces finding #4: filtering on an association PATH ({@code category.id}) forces an INNER JOIN
 * (see {@code JooqQueryBuilder#joins}/{@code #getPath}), so a row whose FK is null is dropped from the
 * result set BEFORE the predicate is evaluated — even when that row plainly matches a non-association
 * branch of an OR. A LEFT (outer) join would retain it.
 *
 * The query below is: {@code category.id == <nonexistent>  OR  name == <our row>}. The null-FK row
 * matches the second branch and MUST be returned. It fails on the current code because the INNER JOIN
 * removes the row from the join before the OR is considered.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class AssociationNullFkOrFilterTest {

    @Autowired
    private MockMvc mvc;

    private SpeedyTest client;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void orAcrossAssociationPath_shouldStillReturnNullFkRowMatchingOtherBranch() throws Exception {
        String uniqueName = "NULLFK-" + System.nanoTime();

        // A row with NO category (FK is null).
        client.create("FkNullEntity")
                .field("name", uniqueName)
                .field("category", null)
                .execute()
                .expectOk();

        String body = CommonUtil.json().writeValueAsString(
                SpeedyQuery.from("FkNullEntity")
                        .where(or(
                                condition("category.id", eq("__no_such_category__")),
                                condition("name", eq(uniqueName))
                        ))
                        .build());

        mvc.perform(MockMvcRequestBuilders
                        .post(SpeedyConstants.URI + "/FkNullEntity/" + SpeedyEndpoint.QUERY.suffix())
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                // FAILS today: the INNER JOIN on category dropped the null-FK row before the OR ran.
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name",
                        Matchers.hasItem(uniqueName)));
    }
}
