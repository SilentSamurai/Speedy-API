package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomTypeTest {

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    private MockMvc mvc;

    @Test
    void createAndReadCustomTypeEntity() throws Exception {
        String emailValue = "user@example.com";

        MvcResult createResult = mvc.perform(
                        MockMvcRequestBuilders.post(SpeedyConstant.URI + "/CustomTypeEntity/" + SpeedyEndpoint.CREATE.suffix())
                                .content(CommonUtil.toJson(List.of(CommonUtil.json().createObjectNode()
                                        .put("email", emailValue))))
                                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andReturn();

        String entityId = CommonUtil.json()
                .readTree(createResult.getResponse().getContentAsString())
                .get("payload").get(0).get("id").asText();

        mvc.perform(MockMvcRequestBuilders.get(SpeedyConstant.URI + "/CustomTypeEntity?id='" + entityId + "'")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[0].email").value(emailValue));
    }

    @Test
    void queryCustomTypeByEmail() throws Exception {
        String emailValue = "query-test@example.com";

        MvcResult createResult = mvc.perform(
                        MockMvcRequestBuilders.post(SpeedyConstant.URI + "/CustomTypeEntity/" + SpeedyEndpoint.CREATE.suffix())
                                .content(CommonUtil.toJson(List.of(CommonUtil.json().createObjectNode()
                                        .put("email", emailValue))))
                                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        String entityId = CommonUtil.json()
                .readTree(createResult.getResponse().getContentAsString())
                .get("payload").get(0).get("id").asText();

        mvc.perform(MockMvcRequestBuilders.get(SpeedyConstant.URI + "/CustomTypeEntity?email='" + emailValue + "'")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[0].id").value(entityId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[0].email").value(emailValue));
    }
}
