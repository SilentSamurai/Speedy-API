package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.entity.FkNullEntity;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyV2EdgeCaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SpeedyTest client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
        mapper = CommonUtil.json();
    }

    @Test
    void missingFromField_defaultsToEntityFromUri() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.putObject("$where").put("name", "test");

        mockMvc.perform(post(SpeedyConstant.URI + "/Product/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray());
    }

    @Test
    void malformedJson_shouldReturn400() throws Exception {
        mockMvc.perform(post(SpeedyConstant.URI + "/Product/$query")
                        .content("{\"$from\":")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void objectBodyForCreate_shouldReturn400() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("name", "test");

        mockMvc.perform(post(SpeedyConstant.URI + "/Product/$create")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postToMetadataEndpoint_shouldReturn4xx() throws Exception {
        mockMvc.perform(post(SpeedyConstant.URI + "/Product/$metadata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void xssInjection_isStoredAndReturnedAsIs() {
        String xssName = "<script>alert('xss')</script>" + System.nanoTime();

        SpeedyTestResult result = client.create("Category")
                .field("name", xssName)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = result.jsonPath("$.payload[0].id");

        client.get("Category")
                .key("id", id)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].name", equalTo(xssName));
    }

    @Test
    void nullFkHandling_createsEntityWithNullAssociation() {
        String entityName = "NullFK " + System.nanoTime();

        SpeedyTestResult result = client.create("FkNullEntity")
                .field("name", entityName)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", not(emptyString()));

        String id = result.jsonPath("$.payload[0].id");

        EntityManager em = entityManagerFactory.createEntityManager();
        FkNullEntity persisted = em.createQuery(
                        "SELECT f FROM FkNullEntity f WHERE f.name = :name", FkNullEntity.class)
                .setParameter("name", entityName)
                .getSingleResult();

        assertNotNull(persisted, "FkNullEntity should be persisted");
        assertEquals(entityName, persisted.getName());
        assertNull(persisted.getCategory(), "FK should be null when not supplied");
        em.close();
    }

    @Test
    void matchesOnNonExistentField_shouldReturn404() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "Product");
        ObjectNode where = body.putObject("$where");
        where.put("nonexistent", "Karan");

        mockMvc.perform(post(SpeedyConstant.URI + "/Product/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void nestedOrWithFkFields_returnsResults() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "Product");
        ArrayNode orNode = body.putObject("$where")
                .putArray("$or");
        orNode.addObject().put("category.id", "1");
        orNode.addObject().put("id", "2");

        mockMvc.perform(post(SpeedyConstant.URI + "/Product/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload[*]").isNotEmpty())
                .andReturn();
    }
}
