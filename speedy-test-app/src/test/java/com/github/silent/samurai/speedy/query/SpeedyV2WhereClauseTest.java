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
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;

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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Currency/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
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
                .put("$matches", "*-10-10");


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Invoice/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Invoice/" + SpeedyEndpoint.QUERY.suffix())
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


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Invoice/" + SpeedyEndpoint.QUERY.suffix())
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

        for (JsonNode item : payload) {
            double invoiceDate = item.get("invoiceDate").asDouble();
            double createdAt = item.get("createdAt").asDouble();
            assertEquals(invoiceDate, createdAt, String.format("Expected discount < dueAmount but got invoiceDate=%s, createdAt=%s", invoiceDate, createdAt));
        }

    }

    // T007 - User Story 1: $between on numeric field (cost)
    // Inventory costs: 10, 15, 25, 30, 45, 50, 60, 75, 80, 100
    // $between [10, 50] should return 6 records (10, 15, 25, 30, 45, 50)
    @Test
    void testQuery20_between_numeric() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        ArrayNode betweenArray = body.putObject("$where")
                .putObject("cost")
                .putArray("$between");
        betweenArray.add(10);
        betweenArray.add(50);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(6)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(
                                Matchers.allOf(
                                        Matchers.greaterThanOrEqualTo(10.0),
                                        Matchers.lessThanOrEqualTo(50.0)
                                )
                        )))
                .andReturn();
    }

    // T008 - User Story 1: $between on date field (purchaseDate)
    // Procurement purchaseDate values: 2022-01-01, 2022-01-02, 2022-01-03, 2022-01-04, 2022-01-05, 2022-01-05
    // $between ["2022-01-01T00:00:00Z", "2022-01-03T23:59:59Z"] should return 3 records (IDs 1, 2, 3)
    @Test
    void testQuery21_between_date() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        ArrayNode betweenArray = body.putObject("$where")
                .putObject("purchaseDate")
                .putArray("$between");
        betweenArray.add("2022-01-01T00:00:00Z");
        betweenArray.add("2022-01-03T23:59:59Z");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();
    }

    // T009 - User Story 1: $between with empty result set
    // Inventory costs: 10, 15, 25, 30, 45, 50, 60, 75, 80, 100
    // $between [200, 300] should return 0 records
    @Test
    void testQuery22_between_empty_result() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        ArrayNode betweenArray = body.putObject("$where")
                .putObject("cost")
                .putArray("$between");
        betweenArray.add(200);
        betweenArray.add(300);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(0)))
                .andReturn();
    }

    // T010 - User Story 1: $between on datetime field (createdAt)
    // All procurements have createdAt = '2022-04-30 10:00:00'
    @Test
    void testQuery23_between_datetime() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        ArrayNode betweenArray = body.putObject("$where")
                .putObject("createdAt")
                .putArray("$between");
        betweenArray.add("2022-04-30T00:00:00");
        betweenArray.add("2022-04-30T23:59:59");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(6)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();
    }

    // T011 - User Story 1: $between on string field (name)
    // Category names: cat-1-1 through cat-13-13
    // $between ["cat-10-10", "cat-13-13"] should return 4 records (10, 11, 12, 13)
    @Test
    void testQuery24_between_string() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        ArrayNode betweenArray = body.putObject("$where")
                .putObject("name")
                .putArray("$between");
        betweenArray.add("cat-10-10");
        betweenArray.add("cat-13-13");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(4)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();
    }

    // T016 - User Story 2: $isnull shorthand on modifiedAt
    // All procurements have modifiedAt = NULL
    @Test
    void testQuery25_isnull_shorthand() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where")
                .put("modifiedAt", "$isnull");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(6)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].modifiedAt")
                        .value(Matchers.everyItem(Matchers.is(IsNull.nullValue()))))
                .andReturn();
    }

    // T017 - User Story 2: $isnull explicit form
    @Test
    void testQuery26_isnull_explicit() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where")
                .putObject("modifiedAt")
                .put("$isnull", true);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(6)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].modifiedAt")
                        .value(Matchers.everyItem(Matchers.is(IsNull.nullValue()))))
                .andReturn();
    }

    // T018 - User Story 2: $isnotnull shorthand on modifiedAt (all NULL, expect empty)
    @Test
    void testQuery27_isnotnull_shorthand() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where")
                .put("modifiedAt", "$isnotnull");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(0)))
                .andReturn();
    }

    // T019 - User Story 2: $isnotnull explicit on createdAt (all non-null, expect all 6)
    @Test
    void testQuery28_isnotnull_explicit() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where")
                .putObject("createdAt")
                .put("$isnotnull", true);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(6)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].createdAt")
                        .value(Matchers.everyItem(Matchers.is(IsNull.notNullValue()))))
                .andReturn();
    }

    // T020 - User Story 2: $isnull with false value → 400 error
    @Test
    void testQuery29_isnull_false_error() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where")
                .putObject("modifiedAt")
                .put("$isnull", false);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value(Matchers.containsString("$isnull requires true")))
                .andReturn();
    }

    // T027 - $isnull with non-boolean value (integer) → 400 error
    @Test
    void testQuery29b_isnull_nonboolean_error() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where")
                .putObject("modifiedAt")
                .put("$isnull", 123);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value(Matchers.containsString("$isnull only accepts a boolean value")));
    }

    // T028 - $isnotnull with non-boolean value (string) → 400 error
    @Test
    void testQuery29c_isnotnull_nonboolean_error() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        body.putObject("$where")
                .putObject("modifiedAt")
                .put("$isnotnull", "yes");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value(Matchers.containsString("$isnotnull only accepts a boolean value")));
    }

    // T027 - User Story 3: $between inside $or with $isnull
    @Test
    void testQuery30_between_in_or_with_isnull() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Procurement");
        ArrayNode orArray = body.putObject("$where")
                .putArray("$or");

        // Branch 1: purchaseDate between 2022-01-01 and 2022-01-02
        ObjectNode betweenCondition = orArray.addObject();
        betweenCondition.putObject("purchaseDate")
                .putArray("$between")
                .add("2022-01-01T00:00:00Z")
                .add("2022-01-02T23:59:59Z");

        // Branch 2: modifiedAt $isnull
        ObjectNode isnullCondition = orArray.addObject();
        isnullCondition.putObject("modifiedAt")
                .put("$isnull", true);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/" + SpeedyEndpoint.QUERY.suffix())
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

        String content = mvcResult.getResponse().getContentAsString();
        JsonNode root = CommonUtil.json().readTree(content);
        JsonNode payload = root.get("payload");

        // All 6 procurements have modifiedAt=NULL, so this $or should return all 6
        assertEquals(6, payload.size());
    }

    // T028 - User Story 3: $between inside $and with another field condition
    @Test
    void testQuery31_between_in_and_with_field() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Inventory");
        ArrayNode andArray = body.putObject("$where")
                .putArray("$and");

        // Condition 1: cost between 30 and 80
        ObjectNode betweenCondition = andArray.addObject();
        betweenCondition.putObject("cost")
                .putArray("$between")
                .add(30)
                .add(80);

        // Condition 2: cost != 75 (or some other field condition)
        ObjectNode neCondition = andArray.addObject();
        neCondition.putObject("cost")
                .put("$ne", 75);

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].cost")
                        .value(Matchers.everyItem(
                                Matchers.allOf(
                                        Matchers.greaterThanOrEqualTo(30.0),
                                        Matchers.lessThanOrEqualTo(80.0),
                                        Matchers.not(Matchers.equalTo(75.0))
                                )
                        )))
                .andReturn();
    }

    // T029 - User Story 3: $between produces same results as $and + $gte + $lte
    @Test
    void testQuery32_between_equals_gte_lte() throws Exception {
        // Query 1: cost $between [15, 50]
        ObjectNode betweenBody = CommonUtil.json().createObjectNode();
        betweenBody.put("$from", "Inventory");
        betweenBody.putObject("$where")
                .putObject("cost")
                .putArray("$between")
                .add(15)
                .add(50);

        MockHttpServletRequestBuilder betweenReq = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(betweenBody))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult betweenResult = mvc.perform(betweenReq)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andReturn();

        JsonNode betweenPayload = CommonUtil.json().readTree(betweenResult.getResponse().getContentAsString()).get("payload");

        // Query 2: cost $gte 15 AND cost $lte 50
        ObjectNode andBody = CommonUtil.json().createObjectNode();
        andBody.put("$from", "Inventory");
        ArrayNode andArray = andBody.putObject("$where")
                .putArray("$and");
        andArray.addObject()
                .putObject("cost")
                .put("$gte", 15);
        andArray.addObject()
                .putObject("cost")
                .put("$lte", 50);

        MockHttpServletRequestBuilder andReq = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Inventory/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(andBody))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult andResult = mvc.perform(andReq)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]",
                        Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andReturn();

        JsonNode andPayload = CommonUtil.json().readTree(andResult.getResponse().getContentAsString()).get("payload");

        // Both must return the same number of records
        assertEquals(betweenPayload.size(), andPayload.size(),
                "$between and $and+$gte+$lte should return the same number of records");

        // Both must return the same IDs
        for (int i = 0; i < betweenPayload.size(); i++) {
            String betweenId = betweenPayload.get(i).get("id").asText();
            String andId = andPayload.get(i).get("id").asText();
            assertEquals(betweenId, andId,
                    "Record IDs must match between $between and $and+$gte+$lte at index " + i);
        }
    }

}
