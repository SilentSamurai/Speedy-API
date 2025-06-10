package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyRequest;
import com.github.silent.samurai.speedy.entity.Procurement;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.ProcurementRepository;
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

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyV2ForgeinKeyWhereClause {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyV2ForgeinKeyWhereClause.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    ProcurementRepository procurementRepository;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
    }

    @Test
    void query() throws Exception {

        Procurement procurement = procurementRepository.findById("1").get();

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyRequest
                                .query("Procurement")
                                .where(
                                        and(
                                                condition("product.id", eq("1")),
                                                condition("supplier.id", eq("2"))
                                        )
                                )
                                .prettyPrint()
                                .build()
                ))
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.id")
                        .value(Matchers.hasItem("1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier.id").value(Matchers.hasItem("2")))
                .andReturn();
    }


    @Test
    void query_2() throws Exception {

        Procurement procurement = procurementRepository.findById("1").get();

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Procurement/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyRequest
                                .query("Procurement")
                                .where(
                                        and(
                                                condition("product.id", eq("1")),
                                                condition("supplier.id", eq("2")),
                                                condition("amount", gt(100))
                                        )
                                )
                                .prettyPrint()
                                .build()
                ))
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product.id")
                        .value(Matchers.hasItem("1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier.id").value(Matchers.hasItem("2")))
                .andReturn();
    }
}