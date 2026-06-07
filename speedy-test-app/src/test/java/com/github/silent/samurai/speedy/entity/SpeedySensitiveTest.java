package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedySensitiveTest {

    private final ObjectMapper mapper = CommonUtil.json();
    @Autowired
    private MockMvc mockMvc;

    @Test
    void classLevelSensitive_inheritedFieldRef_shouldBeRejected() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveClassEntity");
        ObjectNode where = body.putObject("$where");
        where.put("fieldB", "$fieldA");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveClassEntity/" + SpeedyEndpoint.QUERY.suffix())
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void classLevelSensitive_exemptedFieldRef_shouldSucceed() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveClassEntity");
        ObjectNode where = body.putObject("$where");
        where.put("fieldA", "$fieldB");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveClassEntity/" + SpeedyEndpoint.QUERY.suffix())
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void classLevelSensitive_literalOnRHS_shouldSucceed() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "SensitiveClassEntity");
        ObjectNode where = body.putObject("$where");
        where.put("fieldA", "some-value");

        mockMvc.perform(post(SpeedyConstant.URI + "/SensitiveClassEntity/" + SpeedyEndpoint.QUERY.suffix())
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void classLevelSensitive_metadataShowsBothFields() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='SensitiveClassEntity')].fields[?(@.outputProperty=='fieldA')].sensitive").value(true))
                .andExpect(jsonPath("$[?(@.name=='SensitiveClassEntity')].fields[?(@.outputProperty=='fieldB')].sensitive").value(false))
                .andReturn();
    }

    @Test
    void classLevelSensitive_GET_inheritedFieldRef_shouldBeRejected() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + "/SensitiveClassEntity?fieldB=$fieldA")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mockMvc.perform(getRequest)
                .andExpect(status().isBadRequest())
                .andReturn();
    }
}
