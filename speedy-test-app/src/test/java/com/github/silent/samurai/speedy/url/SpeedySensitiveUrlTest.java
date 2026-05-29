package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedySensitiveUrlTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sensitiveFieldRef_publicFieldEqualsSecretField_shouldBeRejected() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + "/SensitiveTestEntity?publicField=$secretField")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void sensitiveFieldRef_publicFieldEqualsAmount_shouldBeRejected() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + "/SensitiveTestEntity?publicField=$amount")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void nonSensitiveFieldRef_publicFieldEqualsLiteralValue_shouldSucceed() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + "/SensitiveTestEntity?publicField=hello")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void literalOnSensitiveField_secretFieldEqualsLiteralValue_shouldSucceed() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + "/SensitiveTestEntity?secretField=actualValue")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andReturn();
    }

    // fieldA inherits @SpeedySensitive from the class; $fieldA on RHS must be rejected.
    @Test
    void sensitiveClassEntity_inheritedFieldInRHS_shouldBeRejected() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + "/SensitiveClassEntity?fieldB=$fieldA")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    // fieldB is marked @SpeedySensitive(false), exempt from entity-level sensitivity.
    @Test
    void sensitiveClassEntity_exemptedFieldInRHS_shouldSucceed() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + "/SensitiveClassEntity?fieldA=$fieldB")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void sensitiveOnLeft_secretFieldRefToPublicField_shouldSucceed() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + "/SensitiveTestEntity?secretField=$publicField")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andReturn();
    }
}
