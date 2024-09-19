package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.SpeedyQuery;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.hamcrest.Matchers;
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

import javax.persistence.EntityManagerFactory;

import static com.github.silent.samurai.speedy.SpeedyQuery.$condition;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyV2WhereValueTestClauseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyV2WhereValueTestClauseTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    private MockMvc mvc;

    /*
       {
              "$from": "ValueTestEntity",
              "$where": {
                "localTime": { "$eq": "11:00:00" }
              }
       }
       * */
    @Test
    void testQuery0() throws Exception {
        JsonNode body = SpeedyQuery.builder()
                .$from("ValueTestEntity")
                .$where(
                        $condition("localTime", SpeedyQuery.$eq("10:00:00"))
                )
                .prettyPrint()
                .build();

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].localTime").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].localTime").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].localTime")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].localTime")
                        .value(Matchers.everyItem(
                                Matchers.is("10:00:00")
                        )))
                .andReturn();

    }

    /*
       {
              "$from": "ValueTestEntity",
              "$where": {
                "localTime": { "$lt": "11:00:00" }
              }
       }
       * */
    @Test
    void testQuery1() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "ValueTestEntity");
        body.putObject("$where")
                .putObject("localTime")
                .put("$lt", "11:00:00");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].localTime").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].localTime").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].localTime")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].localTime")
                        .value(Matchers.everyItem(
                                Matchers.lessThan("11:00:00")
                        )))
                .andReturn();

    }

    /*
       {
              "$from": "ValueTestEntity",
              "$where": {
                "localTime": { "$gte": "11:00:00" }
              }
       }
       * */
    @Test
    void testQuery2() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "ValueTestEntity");
        body.putObject("$where")
                .putObject("localTime")
                .put("$gte", "11:00:00");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(0)))
                .andReturn();

    }


}
