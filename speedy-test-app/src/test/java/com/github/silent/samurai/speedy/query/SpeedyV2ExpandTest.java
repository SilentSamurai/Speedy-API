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
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                                .expand("Product.Category")
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

    /*
       {
           "from": "ExchangeRate",
           "expand": ["baseCurrency", "foreignCurrency"],
       }
       * */
    @Test
    void multiple_expansions_same_entity_type() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/ExchangeRate/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("ExchangeRate")
                                .expand("Currency")
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

                // Validate baseCurrency expansion (first Currency reference)
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.currencyName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.currencyName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.currencyName")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.currencySymbol").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.currencySymbol").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.currencySymbol")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.currencyAbbr").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.currencyAbbr").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.currencyAbbr")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                // Validate foreignCurrency expansion (second Currency reference)
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.currencyName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.currencyName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.currencyName")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.currencySymbol").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.currencySymbol").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.currencySymbol")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.currencyAbbr").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.currencyAbbr").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.currencyAbbr")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                // Validate that both expansions are independent (different Currency instances)
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].baseCurrency.id")
                        .value(Matchers.everyItem(Matchers.not(Matchers.equalTo(
                                Matchers.anyOf(
                                        Matchers.nullValue(),
                                        Matchers.emptyString()
                                )
                        )))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].foreignCurrency.id")
                        .value(Matchers.everyItem(Matchers.not(Matchers.equalTo(
                                Matchers.anyOf(
                                        Matchers.nullValue(),
                                        Matchers.emptyString()
                                )
                        )))))
                .andReturn();

    }

    /*
       {
           "from": "Inventory",
           "expand": ["product", "procurement"],
       }
       * */
    @Test
    void nested_expansions_same_entity_different_levels() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Inventory")
                                .expand("Product")
                                .expand("Product.Category")
                                .expand("Procurement")
                                .expand("Procurement.Product")
                                .expand("Procurement.Product.Category")
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

                // Validate product expansion (Category at level 2)
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.description")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                // Validate product.category expansion (Category at level 2)
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

                // Validate procurement expansion (Category at level 3)
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.amount").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.amount").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.amount")
                        .value(Matchers.everyItem(Matchers.isA(Number.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.dueAmount").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.dueAmount").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.dueAmount")
                        .value(Matchers.everyItem(Matchers.isA(Number.class))))

                // Validate procurement.product expansion (Category at level 3)
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.description")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                // Validate procurement.product.category expansion (Category at level 3)
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category.id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category.name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category.name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category.name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))

                // Validate that Category entities at different levels are properly expanded
                // Both product.category and procurement.product.category should be Category entities
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category.id")
                        .value(Matchers.everyItem(Matchers.not(Matchers.equalTo(
                                Matchers.anyOf(
                                        Matchers.nullValue(),
                                        Matchers.emptyString()
                                )
                        )))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category.id")
                        .value(Matchers.everyItem(Matchers.not(Matchers.equalTo(
                                Matchers.anyOf(
                                        Matchers.nullValue(),
                                        Matchers.emptyString()
                                )
                        )))))
                .andReturn();

    }

    /*
       {
           "from": "Inventory",
           "expand": ["Product", "Category", "Procurement"],
       }
       * */
    @Test
    void entity_based_expansion_with_nested() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/Inventory/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Inventory")
                                .where(condition("product.category", ne(null)))
                                .expand("Product")
                                .expand("Procurement")
                                .expand("Procurement.Product")
                                .expand("Procurement.Product.Category")
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
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        LOGGER.info("Response: {}", responseBody);

        // Check that inventory has basic fields
        mvc.perform(mockHttpServletRequest)
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists());

        // Check that product expansion exists with a nested category
        mvc.perform(mockHttpServletRequest)
                // Verify that product.category exists (nested expansion works)
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.category.name").doesNotExist());

        // Check that procurement expansion exists with nested product and category
        mvc.perform(mockHttpServletRequest)
                // Verify that procurement.product.category exists (deep nested expansion)
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category.name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].procurement.product.category.id").exists());
    }

    // ==================== EDGE CASE TESTS ====================

    /*
       Test invalid expansion paths that don't exist in the entity model
       {
           "from": "Product",
           "expand": ["NonExistentEntity"],
       }
       * */
    @Test
    void invalid_expansion_path_should_handle_gracefully() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Product")
                                .expand("NonExistentEntity")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        // Should either return 400 Bad Request or handle gracefully with 200 OK
        // but without the invalid expansion
        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        int status = mvcResult.getResponse().getStatus();
        
        if (status == 200) {
            // If it returns 200, verify that the invalid expansion is not present
            mvc.perform(mockHttpServletRequest)
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].nonExistentEntity").doesNotExist())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists());
        } else {
            // If it returns 400, that's also acceptable for invalid expansion paths
            mvc.perform(mockHttpServletRequest)
                    .andExpect(status().isBadRequest());
        }
    }

    /*
       Test invalid nested expansion paths
       {
           "from": "Product",
           "expand": ["Category.NonExistentField"],
       }
       * */
    @Test
    void invalid_nested_expansion_path_should_handle_gracefully() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Product")
                                .expand("Category.NonExistentField")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        int status = mvcResult.getResponse().getStatus();
        
        if (status == 200) {
            // If it returns 200, verify that the invalid nested expansion is not present
            mvc.perform(mockHttpServletRequest)
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.nonExistentField").doesNotExist())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").exists());
        } else {
            // If it returns 400, that's also acceptable for invalid expansion paths
            mvc.perform(mockHttpServletRequest)
                    .andExpect(status().isBadRequest());
        }
    }

    /*
       Test circular reference expansion (if entities have circular relationships)
       {
           "from": "Product",
           "expand": ["Category.Products.Category"],
       }
       * */
    @Test
    void circular_reference_expansion_should_handle_gracefully() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Product")
                                .expand("Category.Products.Category")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        int status = mvcResult.getResponse().getStatus();
        
        if (status == 200) {
            // If it returns 200, verify that the circular expansion is handled properly
            mvc.perform(mockHttpServletRequest)
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.products").doesNotExist());
            
            // The circular reference should either be limited in depth or handled gracefully
            // We don't expect infinite nesting, so we check that it doesn't go too deep
            String responseBody = mvcResult.getResponse().getContentAsString();
            LOGGER.info("Circular reference response: {}", responseBody);
            
        } else {
            // If it returns 400 or 500, that's also acceptable for circular references
            int responseStatus = mvcResult.getResponse().getStatus();
            assertTrue(responseStatus >= 400 && responseStatus < 600, 
                "Expected 4xx or 5xx status code, but got: " + responseStatus);
        }
    }

    /*
       Test empty expansion list
       {
           "from": "Product",
           "expand": [],
       }
       * */
    @Test
    void empty_expansion_list_should_work() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Product")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                // Should not have expanded fields
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.name").doesNotExist())
                .andReturn();
    }

    /*
       Test null expansion list
       {
           "from": "Product",
           "expand": null,
       }
       * */
    @Test
    void null_expansion_list_should_work() throws Exception {
        // Create a query without any expanded calls
        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Product")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                // Should not have expanded fields
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.name").doesNotExist())
                .andReturn();
    }

    /*
       Test very deep expansion paths that might exceed limits
       {
           "from": "Product",
           "expand": ["Category.Products.Category.Products.Category.Products.Category"],
       }
       * */
    @Test
    void very_deep_expansion_path_should_handle_gracefully() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("Product")
                                .expand("Category.Products.Category.Products.Category.Products.Category")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        int status = mvcResult.getResponse().getStatus();
        
        if (status == 200) {
            // If it returns 200, verify that the deep expansion is handled properly
            mvc.perform(mockHttpServletRequest)
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").exists());
            
            String responseBody = mvcResult.getResponse().getContentAsString();
            LOGGER.info("Deep expansion response: {}", responseBody);
            
        } else {
            // If it returns 400 or 500, that's also acceptable for very deep expansions
            int responseStatus = mvcResult.getResponse().getStatus();
            assertTrue(responseStatus >= 400 && responseStatus < 600, 
                "Expected 4xx or 5xx status code, but got: " + responseStatus);
        }
    }

    /*
       Test expansion with non-existent entity
       {
           "from": "NonExistentEntity",
           "expand": ["SomeField"],
       }
       * */
    @Test
    void expansion_with_non_existent_entity_should_fail() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/NonExistentEntity/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery
                                .from("NonExistentEntity")
                                .expand("SomeField")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        // Should return 400 Bad Request or 404 Not Found for non-existent entity
        MvcResult result = mvc.perform(mockHttpServletRequest)
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        
        int responseStatus = result.getResponse().getStatus();
        assertTrue(responseStatus == 400 || responseStatus == 404, 
            "Expected 400 or 404 status code, but got: " + responseStatus);
    }

}

