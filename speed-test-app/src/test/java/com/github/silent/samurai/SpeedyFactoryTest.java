package com.github.silent.samurai;

import com.github.silent.samurai.interfaces.MetaModelProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyFactoryTest {

    Logger logger = LogManager.getLogger(SpeedyFactoryTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    MetaModelProcessor metaModelProcessor;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        entityManager
                .createNativeQuery("INSERT INTO categories(ID,NAME) VALUES('1234', 'cat-1');")
                .executeUpdate();
        transaction.commit();
    }

    @Test
    void requestResource() throws Exception {
        Assertions.assertNotNull(metaModelProcessor.findEntityMetadata("Customer"));

        MvcResult mvcResult = mvc.perform(get("/speedy/v1.0/Category(id='1234')")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        logger.info(mvcResult.getResponse().getContentAsString());
    }
}