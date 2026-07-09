package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.repositories.PkUuidTestRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class PkUuidTestTest {

    @Autowired
    PkUuidTestRepository pkUuidTestRepository;

    @Autowired
    private MockMvc mvc;

    public static Matcher<String> isValidUUID() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(String item) {
                try {
                    UUID.fromString(item);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a valid UUID");
            }
        };
    }

    @BeforeEach
    void setUp() {
    }

    /// Tests creating a PkUuidTest entity with an auto-generated UUID primary key
    /// via the POST `/speedy/v1/PkUuidTest/$create` endpoint.
    @Test
    void save() throws Exception {
        ArrayNode arrayNode = CommonUtil.json()
                .createArrayNode();
        arrayNode.add(CommonUtil.json().createObjectNode()
                .put("name", "test")
                .put("description", "test des"));
        MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders.post(SpeedyConstants.URI + "/PkUuidTest/" + SpeedyEndpoint.CREATE.suffix())
                .content(arrayNode.toPrettyString())
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        mvc.perform(createRequest)
                .andExpect(status().isOk());
    }

    /// Tests querying PkUuidTest entities via the POST `/speedy/v1/PkUuidTest/$query` endpoint.
    /// Verifies that UUID primary keys are returned as valid UUID strings in the JSON response payload.
    @Test
    void query() throws Exception {

        PkUuidTest test = new PkUuidTest();
        test.setName("test");
        test.setDescription("desc");

        pkUuidTestRepository.save(test);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstants.URI + "/PkUuidTest/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("PkUuidTest")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(isValidUUID())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.hasItem("test")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].description").value(Matchers.hasItem("desc")))
                .andReturn();
    }

    /// Tests updating a PkUuidTest entity identified by its UUID primary key
    /// via the PATCH `/speedy/v1/PkUuidTest/$update` endpoint.
    /// Verifies the field value is persisted correctly after the update.
    @Test
    void update() throws Exception {
        PkUuidTest test = new PkUuidTest();
        test.setName("test-update");
        test.setDescription("desc-update");
        pkUuidTestRepository.save(test);
        assertNotNull(test.getId());

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
                .patch(SpeedyConstants.URI + "/PkUuidTest/" + SpeedyEndpoint.UPDATE.suffix())
                .content(CommonUtil.json().createObjectNode()
                        .put("id", test.getId().toString())
                        .put("name", "updated-name")
                        .toPrettyString())
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        mvc.perform(updateRequest)
                .andExpect(status().isOk());

        Optional<PkUuidTest> updated = pkUuidTestRepository.findById(test.getId());
        assertTrue(updated.isPresent());
        assertEquals("updated-name", updated.get().getName());
    }

    /// Regression test for issue #115: the "id" in a PATCH body is only ever used to locate
    /// the row (SQL WHERE clause) and is never written into the SET clause — there is no way
    /// to "change" an entity's PK via $update, since the same id value is used both to find
    /// the row and (if it were writable) to set it. Sending a body whose id resolves to a
    /// *different* existing row updates that row's non-key fields only, leaving the row the
    /// caller actually meant to touch completely untouched.
    @Test
    void update_keyIdentifiesRowOnly_originalRowUntouchedByOtherKeysUpdate() throws Exception {
        PkUuidTest first = new PkUuidTest();
        first.setName("first");
        first.setDescription("desc-first");
        pkUuidTestRepository.save(first);

        PkUuidTest second = new PkUuidTest();
        second.setName("second");
        second.setDescription("desc-second");
        pkUuidTestRepository.save(second);

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
                .patch(SpeedyConstants.URI + "/PkUuidTest/" + SpeedyEndpoint.UPDATE.suffix())
                .content(CommonUtil.json().createObjectNode()
                        .put("id", second.getId().toString())
                        .put("name", "updated-second")
                        .toPrettyString())
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        mvc.perform(updateRequest)
                .andExpect(status().isOk());

        Optional<PkUuidTest> untouched = pkUuidTestRepository.findById(first.getId());
        assertTrue(untouched.isPresent());
        assertEquals("first", untouched.get().getName(), "row not addressed by the request must be unaffected");

        Optional<PkUuidTest> updated = pkUuidTestRepository.findById(second.getId());
        assertTrue(updated.isPresent());
        assertEquals(second.getId(), updated.get().getId(), "key column is never part of the SET clause");
        assertEquals("updated-second", updated.get().getName());
    }

    /// Tests deleting a PkUuidTest entity identified by its UUID primary key
    /// via the DELETE `/speedy/v1/PkUuidTest/$delete` endpoint.
    /// Verifies the entity count decreases by one after the delete operation.
    @Test
    void delete() throws Exception {
        PkUuidTest test = new PkUuidTest();
        test.setName("test-delete");
        test.setDescription("desc-delete");
        pkUuidTestRepository.save(test);
        assertNotNull(test.getId());

        long count = pkUuidTestRepository.count();

        MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders
                .delete(SpeedyConstants.URI + "/PkUuidTest/" + SpeedyEndpoint.DELETE.suffix())
                .content("[{\"id\":\"" + test.getId() + "\"}]")
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        mvc.perform(deleteRequest)
                .andExpect(status().isOk());

        assertEquals(count - 1, pkUuidTestRepository.count());
    }
}