package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyClient;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class NullAssociationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NullAssociationTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    private MockMvc mvc;

    private SpeedyClient<ResultActions> speedyClient;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyClient.mockMvc(mockMvc);
    }

    @Test
    void null_fk_with_entity() throws Exception {

        speedyClient.create("FkNullEntity")
                .addField("name", "TEST")
                .addField("category", null)
                .execute()
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();


        MvcResult mvcResult = speedyClient.query(
                        SpeedyQuery.from("FkNullEntity")
                                .where(
                                        condition("category", eq(null))
                                )
                ).execute()
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
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.equalTo("TEST"))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").value(
                        Matchers.everyItem(Matchers.nullValue())
                ))
                .andReturn();

    }

    // InsERT INTO FK_NULL_ENTITY ( CATEGORY_ID , ID , NAME ) VALUES (NULL, 'f3886975-51e2-4320-8280-76f6661fc964', 'TEST2');
    // SELECT * FROM FK_NULL_ENTITY JOIN CATEGORIES ON FK_NULL_ENTITY.CATEGORY_ID = CATEGORIES.ID
    // SELECT * FROM FK_NULL_ENTITY LEFT OUTER JOIN CATEGORIES ON FK_NULL_ENTITY.CATEGORY_ID = CATEGORIES.ID WHERE CATEGORIES.ID IS NULL
    @Test
    void filtering_with_null_fk() throws Exception {
        speedyClient.create("FkNullEntity")
                .addField("name", "TEST")
                .addField("category", null)
                .execute()
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();


        MvcResult mvcResult = speedyClient.query(
                        SpeedyQuery.from("FkNullEntity")
                                .where(
                                        condition("name", eq("TEST")),
                                        condition("category", eq(null))
                                )
                ).execute()
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
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.equalTo("TEST"))))

                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();

    }

}
