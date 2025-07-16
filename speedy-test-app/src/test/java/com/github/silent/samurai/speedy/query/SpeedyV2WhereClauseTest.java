package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.persistence.EntityManagerFactory;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyV2WhereClauseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyV2WhereClauseTest.class);

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
           "where": {
               "name": "cat-1-1"
           }
       }
       * */
    @Test
    void testQuery1() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$where")
                .put("name", "cat-1-1");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.is("cat-1-1"))))
                .andReturn();

    }

    /*
       {
           "from": "Category",
           "where": {
               "id": "1"
           }
       }
       */
    @Test
    void testQuery2() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$where")
                .put("id", "2");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.is("2"))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.is("cat-2-2"))))
                .andReturn();

    }

    /*
       {
           "from": "Currency",
           "where": {
               "currencyAbbr": "INR"
           }
       }
       */
    @Test
    void testQuery3() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Currency");
        body.putObject("$where")
                .put("currencyAbbr", "INR");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Currency/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].currencyAbbr").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].currencyAbbr").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].currencyAbbr")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].currencyAbbr")
                        .value(Matchers.everyItem(Matchers.is("INR"))))
                .andReturn();

    }

    /*
      {
          "from": "Inventory",
          "where": {
              "cost": { "$in" : [15, 30, 50] }
          }
      }
      */
    @Test
    void testQuery4() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        ArrayNode inArray = body.putObject("$where")
                .putObject("cost")
                .putArray("$in");
        inArray.add(15);
        inArray.add(30);
        inArray.add(50);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(0))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(
                                Matchers.anyOf(
                                        Matchers.equalTo(15.0),
                                        Matchers.equalTo(30.0),
                                        Matchers.equalTo(50.0)
                                )
                        )))
                .andReturn();

    }

    /*
      {
          "from": "Inventory",
          "where": {
              "cost": { "$eq" : 15 }
          }
      }
      */
    @Test
    void testQuery5() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        body.putObject("$where")
                .putObject("cost")
                .put("$eq", 15);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(Matchers.equalTo(15.0))))
                .andReturn();

    }

    /*
      {
          "from": "Inventory",
          "where": {
              "cost": { "$ne" : 15 }
          }
      }
      */
    @Test
    void testQuery6() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        body.putObject("$where")
                .putObject("cost")
                .put("$ne", 15);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(0))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(Matchers.not(Matchers.equalTo(15.0)))))
                .andReturn();

    }

    /*
      {
          "from": "Category",
          "where": {
              "name": { "$ne" : "cat-12-12" }
          }
      }
      */
    @Test
    void testQuery7() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$where")
                .putObject("name")
                .put("$ne", "cat-12-12");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.not(Matchers.equalTo("cat-12-12")))))
                .andReturn();

    }

    /*
       {
           "from": "Inventory",
           "where": {
               "cost": { "$gte" : 10 }
           }
       }
       */
    @Test
    void testQuery8() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        body.putObject("$where")
                .putObject("cost")
                .put("$gte", 10);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(Matchers.greaterThanOrEqualTo(10.0))))
                .andReturn();

    }

    /*
       {
           "from": "Inventory",
           "where": {
               "cost": { "$gt" : 15 }
           }
       }
       */
    @Test
    void testQuery9() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        body.putObject("$where")
                .putObject("cost")
                .put("$gt", 15);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(Matchers.greaterThan(15.0))))
                .andReturn();

    }

    /*
       {
           "from": "Inventory",
           "where": {
               "cost": { "$lt" : 50 }
           }
       }
       */
    @Test
    void testQuery10() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        body.putObject("$where")
                .putObject("cost")
                .put("$lt", 50);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(Matchers.lessThan(50.0))))
                .andReturn();

    }

    /*
       {
           "from": "Inventory",
           "where": {
               "cost": { "$lte" : 15 }
           }
       }
       */
    @Test
    void testQuery11() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        body.putObject("$where")
                .putObject("cost")
                .put("$lte", 15);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(Matchers.lessThanOrEqualTo(15.0))))
                .andReturn();

    }

    /*
       {
           "from": "Inventory",
           "where": {
                "$and": [
                    { "cost": { "$gte" : 10 } },
                    { "cost": { "$lte" : 20 } }
                ]
           }
       }
       */
    @Test
    void testQuery12() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        ArrayNode jsonNodes = body.putObject("$where")
                .putArray("$and");
        jsonNodes.addObject()
                .putObject("cost")
                .put("$gte", 10);
        jsonNodes.addObject()
                .putObject("cost")
                .put("$lte", 20);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(0))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(
                                Matchers.allOf(
                                        Matchers.greaterThanOrEqualTo(10.0),
                                        Matchers.lessThanOrEqualTo(20.0)
                                )
                        )))
                .andReturn();

    }

    /*
       {
           "from": "Inventory",
           "where": {
                "$or": [
                    { "cost": { "$lte" : 15 } },
                    { "cost": { "$gte" : 50 } }
                ]
           }
       }
       */
    @Test
    void testQuery13() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        ArrayNode jsonNodes = body.putObject("$where")
                .putArray("$or");
        jsonNodes.addObject()
                .putObject("cost")
                .put("$lte", 15);
        jsonNodes.addObject()
                .putObject("cost")
                .put("$gte", 50);


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(
                                Matchers.anyOf(
                                        Matchers.lessThanOrEqualTo(15.0),
                                        Matchers.greaterThanOrEqualTo(50.0)
                                )
                        )))
                .andReturn();

    }


    /*
       {
           "from": "Inventory",
           "where": {
                "$and": [
                    { "cost": { "$gte" : 15 } },
                    { "cost": { "$lte" : 50 } }
                ]
           }
       }
       */
    @Test
    void testQuery14() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        ArrayNode jsonNodes = body.putObject("$where")
                .putArray("$and");
        jsonNodes.addObject()
                .putObject("cost")
                .put("$gte", 15);
        jsonNodes.addObject()
                .putObject("cost")
                .put("$lte", 50);


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(
                                Matchers.allOf(
                                        Matchers.lessThanOrEqualTo(50.0),
                                        Matchers.greaterThanOrEqualTo(15.0)
                                )
                        )))
                .andReturn();

    }


    /*
       {
           "from": "Category",
           "where": {
                "$or": [
                    { "$and": { "name" : "cat-1-1" } },
                    { "$and": { "name" : "cat-12-12" } }
                ]
           }
       }
       */
    @Test
    void testQuery15() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        ArrayNode jsonNodes = body.putObject("$where")
                .putArray("$or");
        jsonNodes.addObject()
                .putObject("$and")
                .put("name", "cat-1-1");
        jsonNodes.addObject()
                .putObject("$and")
                .put("name", "cat-12-12");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(
                                Matchers.anyOf(
                                        Matchers.equalTo("cat-1-1"),
                                        Matchers.equalTo("cat-12-12")
                                )
                        )))
                .andReturn();

    }

    /*
       {
           "from": "Procurement",
           "where": {
                "modifiedAt": null
           }
       }
       */
    @Test
    void testQuery16() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where")
                .putNull("modifiedAt");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].modifiedAt").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].modifiedAt").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].modifiedAt", Matchers.allOf(
                        Matchers.everyItem(
                                Matchers.is(IsNull.nullValue())
                        )
                )))
                .andReturn();

    }


    /*
       {
           "from": "Procurement",
           "where": {
                "createdAt": { "$neq": null }
           }
       }
       */
    @Test
    void testQuery17() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where")
                .putObject("createdAt")
                .put("$neq", (String) null);


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].createdAt").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].createdAt",
                        Matchers.everyItem(
                                Matchers.is(IsNull.notNullValue())
                        )
                ))
                .andReturn();

    }

    /*
       {
           "from": "Procurement",
           "where": {
           }
       }
       */
    @Test
    void testQuery18() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();

    }


    /*
       {
           "from": "Procurement"
       }
       */
    @Test
    void testQuery19() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();

    }

    @Test
    void query_with_pattern_matching() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$where")
                .putObject("name")
                .put("$matches", "*10*");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(
                                Matchers.anyOf(
                                        Matchers.equalTo("cat-10-10")
                                )
                        )))
                .andReturn();

    }

    @Test
    void query_with_pattern_matching_with_dot() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$where")
                .putObject("name")
                .put("$matches", ".10*");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(0))))
                .andReturn();

    }

    @Test
    void query_with_pattern_matching_with_special_symbols() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$where")
                .putObject("name")
                .put("$matches", "cat-1*");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(
                                Matchers.anyOf(
                                        Matchers.equalTo("cat-10-10"),
                                        Matchers.equalTo("cat-11-11"),
                                        Matchers.equalTo("cat-12-12"),
                                        Matchers.equalTo("cat-13-13"),
                                        Matchers.equalTo("cat-1-1")
                                )
                        )))
                .andReturn();

    }

    @Test
    void query_with_field_reference() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Invoice");
        body.putObject("$where")
                .putObject("discount")
                .put("$lt", "$dueAmount");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Invoice/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].discount").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].discount").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].dueAmount").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].dueAmount").isNotEmpty())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        JsonNode root = CommonUtil.json().readTree(content);
        JsonNode payload = root.get("payload");

        // Assert: discount < dueAmount
        for (JsonNode item : payload) {
            double discount = item.get("discount").asDouble();
            double dueAmount = item.get("dueAmount").asDouble();
            assertTrue(discount < dueAmount,
                    String.format("Expected discount < dueAmount but got discount=%s, dueAmount=%s", discount, dueAmount));
        }

    }

    @Test
    void query_with_field_reference_with_missing_field() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Invoice");
        body.putObject("$where")
                .putObject("invoiceDate")
                .put("$eq", "$created_at");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Invoice/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(
                        Matchers.allOf(
                                Matchers.containsString("created_at"),
                                Matchers.containsString("not found")
                        )
                ))
                .andReturn();

    }

    @Test
    void query_with_field_reference_with_date() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Invoice");
        body.putObject("$where")
                .putObject("invoiceDate")
                .put("$eq", "$createdAt");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Invoice/$query")
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].invoiceDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].invoiceDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].createdAt").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].createdAt").isNotEmpty())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        JsonNode root = CommonUtil.json().readTree(content);
        JsonNode payload = root.get("payload");

        // Assert: discount < dueAmount
        for (JsonNode item : payload) {
            double invoiceDate = item.get("invoiceDate").asDouble();
            double createdAt = item.get("createdAt").asDouble();
            assertEquals(invoiceDate, createdAt, String.format("Expected discount < dueAmount but got invoiceDate=%s, createdAt=%s", invoiceDate, createdAt));
        }

    }


}
