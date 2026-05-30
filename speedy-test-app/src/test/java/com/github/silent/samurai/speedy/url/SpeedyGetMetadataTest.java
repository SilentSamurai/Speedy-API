package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.hamcrest.Matchers;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyGetMetadataTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyGetMetadataTest.class);

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
    void getMetadata() throws Exception {

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].keyFields").exists())
//                .andExpect(MockMvcResultMatchers.jsonPath("$[*].entityType").exists())
//                .andExpect(MockMvcResultMatchers.jsonPath("$[*].keyType").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].hasCompositeKey").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].keyFields").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields").isArray())
//                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].className").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].outputProperty").exists())
//                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].dbColumn").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].fieldType").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].isNullable").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].isAssociation").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].isCollection").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].isSerializable").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].isDeserializable").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[*].fields[*].isUnique").exists())
                .andReturn();
    }

    @Test
    void getApiDocs() throws Exception {

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get("/v3/api-docs")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.schemas.Category").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.schemas.CategoryKey.properties.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.schemas.CategoryKey.properties.name").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.schemas.UpdateCategoryRequest.properties.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.components.schemas.CreateCategoryRequest.properties.id").doesNotExist())
                .andReturn();
    }

    @Test
    void generatedKeyIsDeserializable() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(
                        "$[?(@.name == 'Category')].fields[?(@.outputProperty == 'id')].isDeserializable",
                        Matchers.everyItem(Matchers.is(true))))
                .andReturn();
    }


}