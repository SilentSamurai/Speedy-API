package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.PkUuidTestRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.persistence.EntityManagerFactory;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class PkUuidTestTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PkUuidTestTest.class);

    /// rust like java doc
    /// ```
    ///
    ///```
    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

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

    @Test
    void save() throws Exception {
        ArrayNode arrayNode = CommonUtil.json()
                .createArrayNode();
        arrayNode.add(CommonUtil.json().createObjectNode()
                .put("name", "test")
                .put("description", "test des"));
        MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/PkUuidTest/$create")
                .content(arrayNode.toPrettyString())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(createRequest)
                .andExpect(status().isOk());
    }

    @Test
    void query() throws Exception {

        PkUuidTest test = new PkUuidTest();
        test.setName("test");
        test.setDescription("desc");

        pkUuidTestRepository.save(test);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/PkUuidTest/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("PkUuidTest")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
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
}