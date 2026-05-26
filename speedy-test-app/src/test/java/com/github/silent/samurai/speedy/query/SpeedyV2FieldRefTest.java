package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyV2FieldRefTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = CommonUtil.json();

    @Test
    void fieldReference_costLessThanSoldPrice() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "Inventory");
        ObjectNode where = body.putObject("$where");
        ObjectNode ltCondition = where.putObject("cost");
        ltCondition.put("$lt", "$soldPrice");

        mockMvc.perform(post(SpeedyConstant.URI + "/Inventory/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload[*]").isNotEmpty())
                .andReturn();
    }

    @Test
    void fieldReference_costGreaterThanDiscount() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "Inventory");
        ObjectNode where = body.putObject("$where");
        ObjectNode gtCondition = where.putObject("cost");
        gtCondition.put("$gt", "$discount");

        mockMvc.perform(post(SpeedyConstant.URI + "/Inventory/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andReturn();
    }

    @Test
    void dateGreaterThan_filtersCorrectly() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "ValueTestEntity");
        ObjectNode where = body.putObject("$where");
        ObjectNode gtCondition = where.putObject("localDateTime");
        gtCondition.put("$gt", "2022-01-01T00:00:00");

        mockMvc.perform(post(SpeedyConstant.URI + "/ValueTestEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload[*]").isNotEmpty())
                .andReturn();
    }

    @Test
    void dateLessThan_filtersCorrectly() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "ValueTestEntity");
        ObjectNode where = body.putObject("$where");
        ObjectNode ltCondition = where.putObject("localDateTime");
        ltCondition.put("$lt", "2023-01-01T00:00:00");

        mockMvc.perform(post(SpeedyConstant.URI + "/ValueTestEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload[*]").isNotEmpty())
                .andReturn();
    }

    @Test
    void dateGreaterThanOrEqual_filtersCorrectly() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "ValueTestEntity");
        ObjectNode where = body.putObject("$where");
        ObjectNode gteCondition = where.putObject("localDateTime");
        gteCondition.put("$gte", "2022-04-30T10:00:00");

        mockMvc.perform(post(SpeedyConstant.URI + "/ValueTestEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload[*]").isNotEmpty())
                .andReturn();
    }

    @Test
    void dateLessThanOrEqual_filtersCorrectly() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "ValueTestEntity");
        ObjectNode where = body.putObject("$where");
        ObjectNode lteCondition = where.putObject("localDateTime");
        lteCondition.put("$lte", "2022-04-30T10:00:00");

        mockMvc.perform(post(SpeedyConstant.URI + "/ValueTestEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload[*]").isNotEmpty())
                .andReturn();
    }

    @Test
    void sensitiveFieldRef_publicEqualsSecret_shouldBeRejected() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveTestEntity");
        ObjectNode where = body.putObject("$where");
        where.put("publicField", "$secretField");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveTestEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void sensitiveFieldRef_publicEqualsAmount_shouldBeRejected() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveTestEntity");
        ObjectNode where = body.putObject("$where");
        where.put("publicField", "$amount");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveTestEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void nonSensitiveFieldRef_publicFieldEqualsLiteral_shouldSucceed() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveTestEntity");
        ObjectNode where = body.putObject("$where");
        where.put("publicField", "test-value");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveTestEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void sensitiveFieldRef_fkToSecretField_shouldBeRejected() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveFkEntity");
        ObjectNode where = body.putObject("$where");
        where.put("name", "$sensitiveTestEntity.secretField");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveFkEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    // fieldA inherits sensitivity from @SpeedySensitive on the entity class;
    // referencing it via $fieldA on the RHS must be rejected.
    @Test
    void sensitiveClassEntity_inheritedFieldRef_shouldBeRejected() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveClassEntity");
        ObjectNode where = body.putObject("$where");
        where.put("fieldB", "$fieldA");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveClassEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    // fieldB is annotated @SpeedySensitive(false), overriding the entity-level
    // default. Referencing it via $fieldB on the RHS must be allowed.
    @Test
    void sensitiveClassEntity_exemptedFieldRef_shouldSucceed() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveClassEntity");
        ObjectNode where = body.putObject("$where");
        where.put("fieldA", "$fieldB");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveClassEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    // Literal values on the RHS are never blocked by sensitivity checks.
    @Test
    void sensitiveClassEntity_literalOnRHS_shouldSucceed() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveClassEntity");
        ObjectNode where = body.putObject("$where");
        where.put("fieldA", "some-value");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveClassEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void sensitiveFieldRef_fkToPublicField_shouldSucceed() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveFkEntity");
        ObjectNode where = body.putObject("$where");
        where.put("name", "$sensitiveTestEntity.publicField");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveFkEntity/$query")
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }
}
