package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyV2OrderByTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyV2OrderByTest.class);

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
           "from": "Category",
           "orderBy": {
               "name": "desc"
           }
       }
       * */
    @Test
    void testQuery1() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$orderBy")
                .put("name", "DESC");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();

    }


    /*
       {
           "from": "Category",
           "orderBy": {
               "name": "asc"
           }
       }
       * */
    @Test
    void testQuery2() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$orderBy")
                .put("name", "ASC");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();

    }


}
