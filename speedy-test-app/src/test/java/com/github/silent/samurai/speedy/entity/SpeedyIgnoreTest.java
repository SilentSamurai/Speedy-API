package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyIgnoreTest {

    private final ObjectMapper mapper = CommonUtil.json();

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void entityLevel_ignoredEntity_absentFromGlobalMetadata() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='IgnoredEntity')]").doesNotExist())
                .andReturn();
    }

    @Test
    void entityLevel_ignoredEntity_getReturns400() {
        speedyClient.get("IgnoredEntity")
                .execute()
                .expectBadRequest();
    }

    @Test
    void entityLevel_ignoredEntity_postQueryReturns400() {
        speedyClient.query("IgnoredEntity")
                .execute()
                .expectBadRequest();
    }

    @Test
    void entityLevel_ignoredEntity_postCreateReturns400() {
        speedyClient.create("IgnoredEntity")
                .field("name", "test")
                .execute()
                .expectBadRequest();
    }

    @Test
    void entityLevel_ignoredEntity_patchUpdateReturns400() {
        speedyClient.update("IgnoredEntity")
                .key("id", "dummy-id")
                .field("name", "test")
                .execute()
                .expectBadRequest();
    }

    @Test
    void entityLevel_ignoredEntity_deleteReturns400() {
        speedyClient.delete("IgnoredEntity")
                .key("id", "dummy-id")
                .execute()
                .expectBadRequest();
    }

    @Test
    void entityLevel_nonIgnoredEntity_presentInMetadata() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Category')]").exists())
                .andReturn();
    }

    @Test
    void fieldLevel_hiddenFieldAbsentFromMetadata() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='EntityWithIgnoredField')].fields[?(@.outputProperty=='hiddenField')]")
                        .doesNotExist())
                .andReturn();
    }

    @Test
    void fieldLevel_intFieldAbsentFromMetadata() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='EntityWithIgnoredField')].fields[?(@.outputProperty=='intField')]")
                        .doesNotExist())
                .andReturn();
    }

    @Test
    void fieldLevel_visibleFieldPresentInMetadata() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='EntityWithIgnoredField')].fields[?(@.outputProperty=='visibleField')]")
                        .exists())
                .andReturn();
    }

    @Test
    void fieldLevel_getWithHiddenFieldFilterReturns400() throws Exception {
        mvc.perform(get(SpeedyConstant.URI + "/EntityWithIgnoredField?hiddenField='test'")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void fieldLevel_querySelectVisibleFieldReturns200() throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("$from", "EntityWithIgnoredField");
        ArrayNode select = body.putArray("$select");
        select.add("visibleField");

        mvc.perform(post(SpeedyConstant.URI + "/EntityWithIgnoredField/" + SpeedyEndpoint.QUERY.suffix())
                        .content(mapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void fieldLevel_getVisibleFieldFilterReturns200() throws Exception {
        mvc.perform(get(SpeedyConstant.URI + "/EntityWithIgnoredField?visibleField='test'")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void fieldLevel_associationToIgnoredEntity_absentFromMetadata() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='EntityWithIgnoredField')].fields[?(@.outputProperty=='associationToIgnored')]")
                        .doesNotExist())
                .andReturn();
    }

    @Test
    void oneToMany_collectionAbsentFromMetadata() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Category')].fields[?(@.outputProperty=='products')]")
                        .doesNotExist())
                .andReturn();
    }

    @Test
    void manyToOne_associationPresentInMetadata() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Product')].fields[?(@.outputProperty=='category')]")
                        .exists())
                .andReturn();
    }
}
