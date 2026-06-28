package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyTypeTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SpeedyFactory speedyFactory;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void metamodelOverride_textField_columnTypeIsTEXT() throws NotFoundException {
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata field = metaModel.findFieldMetadata("TypeOverrideEntity", "textField");

        assertThat(field.getColumnType(), is(ColumnType.TEXT));
    }

    @Test
    void metamodelOverride_bigIntField_columnTypeIsBIGINT() throws NotFoundException {
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata field = metaModel.findFieldMetadata("TypeOverrideEntity", "bigIntField");

        assertThat(field.getColumnType(), is(ColumnType.BIGINT));
    }

    @Test
    void metamodelOverride_floatField_columnTypeIsFLOAT() throws NotFoundException {
        MetaModel metaModel = speedyFactory.getMetaModel();
        FieldMetadata field = metaModel.findFieldMetadata("TypeOverrideEntity", "floatField");

        assertThat(field.getColumnType(), is(ColumnType.FLOAT));
    }

    @Test
    void metamodelOverride_entityExists() throws NotFoundException {
        MetaModel metaModel = speedyFactory.getMetaModel();

        assertThat(metaModel.hasEntityMetadata("TypeOverrideEntity"), is(true));
        EntityMetadata entity = metaModel.findEntityMetadata("TypeOverrideEntity");
        assertThat(entity.getAllFields(), hasSize(4));
    }

    @Test
    void metadataEndpoint_entityPresent() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstants.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='TypeOverrideEntity')]").exists())
                .andReturn();
    }

    @Test
    void metadataEndpoint_fieldsPresent() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstants.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='TypeOverrideEntity')].fields[?(@.outputProperty=='textField')]").exists())
                .andExpect(jsonPath("$[?(@.name=='TypeOverrideEntity')].fields[?(@.outputProperty=='bigIntField')]").exists())
                .andExpect(jsonPath("$[?(@.name=='TypeOverrideEntity')].fields[?(@.outputProperty=='floatField')]").exists())
                .andReturn();
    }

    @Test
    void metadataEndpoint_textField_hasCorrectValueType() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstants.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='TypeOverrideEntity')].fields[?(@.outputProperty=='textField')].fieldType").value("TEXT"))
                .andReturn();
    }

    @Test
    void metadataEndpoint_bigIntField_hasCorrectValueType() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstants.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='TypeOverrideEntity')].fields[?(@.outputProperty=='bigIntField')].fieldType").value("INT"))
                .andReturn();
    }

    @Test
    void metadataEndpoint_floatField_hasCorrectValueType() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(SpeedyConstants.URI + SpeedyEndpoint.METADATA.path())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='TypeOverrideEntity')].fields[?(@.outputProperty=='floatField')].fieldType").value("FLOAT"))
                .andReturn();
    }

    @Test
    void crud_createAndGet_roundTripsCorrectly() {
        speedyClient.create("TypeOverrideEntity")
                .field("textField", "hello world")
                .field("bigIntField", 42)
                .field("floatField", 3.14)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].id", notNullValue());
    }

    @Test
    void get_allFieldsReturnCorrectly() {
        speedyClient.create("TypeOverrideEntity")
                .field("textField", "test-value")
                .field("bigIntField", 99)
                .field("floatField", 2.718)
                .execute()
                .expectOk();

        speedyClient.get("TypeOverrideEntity")
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].textField", notNullValue())
                .expectJsonPath("$.payload[0].bigIntField", notNullValue())
                .expectJsonPath("$.payload[0].floatField", notNullValue());
    }
}
