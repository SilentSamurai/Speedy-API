package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.api.client.SpeedyRequest;
import com.github.silent.samurai.speedy.entity.PkUuidTest;
import com.github.silent.samurai.speedy.entity.Procurement;
import com.github.silent.samurai.speedy.entity.Product;
import com.github.silent.samurai.speedy.entity.Supplier;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.PkUuidTestRepository;
import com.github.silent.samurai.speedy.repositories.ProcurementRepository;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
                                .$where(
                                        SpeedyQuery.$and(
                                                SpeedyQuery.$condition("product.id", SpeedyQuery.$eq("1")),
                                                SpeedyQuery.$condition("supplier.id", SpeedyQuery.$eq("2"))
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].product")
                        .value(Matchers.hasItem("1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].supplier").value(Matchers.hasItem("2")))
                .andReturn();
    }
}