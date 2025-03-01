package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.api.client.SpeedyRequest;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.repositories.ProductRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.persistence.EntityManagerFactory;
import org.hamcrest.Matchers;
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

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.$condition;
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.$eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyV2SelectCountTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyV2SelectCountTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;


    @Autowired
    private MockMvc mvc;

    private long totalProduct = 0;

    @BeforeEach
    void setUp() throws Exception {
        totalProduct = productRepository.count();
    }

    /*
       {
           "from": "Product",
           "$where": {},
           "$select" : ['count']
       }
       * */
    @Test
    void testCountQuery() throws Exception {


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyRequest.query("Product")
                                .$select("count")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(totalProduct))
                .andReturn();

    }

    /*
       {
           "from": "Product",
           "$where": {},
           "$select" : ['count']
       }
       * */
    @Test
    void testCountQuery1() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyRequest.query("Product")
                                .$select("count")
                                .$where(
                                        $condition("id", $eq("1"))
                                )
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(1))
                .andReturn();

    }

    @Test
    void testCountQuery2() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyRequest.query("Product")
                                .$select("count")
                                .$where(
                                        $condition("category.id", $eq("2"))
                                )
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(2))
                .andReturn();

    }

    @Test
    void testCountQuery3() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyRequest.query("Product")
                                .$select("count")
                                .$expand("category")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(totalProduct))
                .andReturn();

    }

    @Test
    void testCountQuery4() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyRequest.query("Product")
                                .$select("count")
                                .$pageNo(2)
                                .$pageSize(2)
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(totalProduct))
                .andReturn();

    }

    @Test
    void testCountQuery5() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyRequest.query("Product")
                                .$select("count")
                                .$orderByAsc("id")
                                .$orderByDesc("name")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);


        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(totalProduct))
                .andReturn();

    }

}
