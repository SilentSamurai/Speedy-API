package com.github.silent.samurai.speedy;

import com.github.silent.samurai.SpeedyFactory;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
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

import javax.persistence.EntityManagerFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyGetTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyGetTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    private MockMvc mvc;

//    @BeforeEach
//    void setUp() {
//        EntityManager entityManager = entityManagerFactory.createEntityManager();
//        EntityTransaction transaction = entityManager.getTransaction();
//        transaction.begin();
//
//        Customer customer = new Customer();
//        customer.setName("cat-1");
//        customer.setEmail("testemail@test.cas");
//        customer.setPhoneNo("+91-189-298-7633");
//
//        entityManager.merge(customer);
//        entityManager.flush();
//        transaction.commit();
//    }


    @Test
    void getViaPrimaryKey() throws Exception {

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(SpeedyConstant.URI + "/Category(id='1')")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload.name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload.name").value("cat-1-1"))
                .andReturn();
    }

    @Test
    void getVia() throws Exception {

        MvcResult mvcResult = mvc.perform(get(SpeedyConstant.URI + "/Category(id='not-there')")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    void getAll() throws Exception {

        mvc.perform(get(SpeedyConstant.URI + "/Category/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andReturn();
    }

    @Test
    void getViaFilter() throws Exception {

        mvc.perform(get(SpeedyConstant.URI + "/Category(name='cat-1-1')")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.payload[*].name",
                                Matchers.contains("cat-1-1")
                        )
                )
                .andReturn();
    }


    @Test
    void getViaFilterArg() throws Exception {

        mvc.perform(get(SpeedyConstant.URI + "/Category('1')")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isMap())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload.name").value("cat-1-1"))
                .andReturn();
    }

    @Test
    void getAssociation() throws Exception {

        mvc.perform(get(SpeedyConstant.URI + "/Product( category.id = '1')")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(2)))
                .andReturn();
    }

    @Test
    void getviadoublequotes() throws Exception {

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(SpeedyConstant.URI + "/Category(id=\"1\")")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload.name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload.name").value("cat-1-1"))
                .andReturn();
    }


}