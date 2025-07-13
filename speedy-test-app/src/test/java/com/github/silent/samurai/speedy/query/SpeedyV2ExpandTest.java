package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.persistence.EntityManagerFactory;
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

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyV2ExpandTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyV2ExpandTest.class);

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
           "from": "Product",
           "expand": ["Category"],
       }
       * */
    @Test
    void single_level_expand() throws Exception {


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Product")
                                .expand("Category")
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
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();

    }


    /*
       {
           "from": "Category",
           "page": {
            "index": 0,
            "size": 100
          }
       }
       * */
    @Test
    void pagging() throws Exception {

        List<Category> allSorted = categoryRepository.findAllSorted();

        JsonNode query = SpeedyQuery.from("Category")
                .orderByAsc("name")
                .pageNo(1)
                .pageSize(2)
                .prettyPrint()
                .build();

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$query")
                .content(CommonUtil.json().writeValueAsString(query))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.equalTo(2))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(
                                Matchers.anyOf(
                                        Matchers.equalTo(allSorted.get(2).getName()),
                                        Matchers.equalTo(allSorted.get(3).getName())
                                )
                        )))
                .andReturn();

    }


    @Test
    void multi_level_expand() throws Exception {


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/Procurement/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Procurement")
                                .expand("Product")
                                .expand("Category")
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
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category.id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category.name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category.name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category.name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();

    }


}
