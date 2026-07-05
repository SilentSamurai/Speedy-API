package com.github.silent.samurai.speedy.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// End-to-end coverage of the {@code application/yaml} I/O provider: the write path
/// (Except → YAML response body) and the read path (YAML request body → create), both
/// driven through the same pipeline as JSON with only the negotiated content type differing.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyYamlIoTest {

    private static final String YAML = "application/yaml";
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void getRendersYaml() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(SpeedyConstants.URI + "/Category/")
                        .accept(YAML))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.contains(YAML), "expected YAML content type but was " + contentType);

        JsonNode doc = YAML_MAPPER.readTree(result.getResponse().getContentAsString());
        assertTrue(doc.get("payload").isArray(), "payload should be a YAML sequence");
        assertFalse(doc.get("payload").isEmpty());
    }

    @Test
    void createFromYamlBody() throws Exception {
        String name = "yaml-cat-" + RandomString.make(8);
        // A YAML sequence with a single mapping — the creation body is a top-level array.
        String body = "- name: " + name + "\n";

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.CREATE.suffix())
                        .content(body)
                        .contentType(YAML)
                        .accept(YAML))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.contains(YAML), "expected YAML content type but was " + contentType);

        JsonNode doc = YAML_MAPPER.readTree(result.getResponse().getContentAsString());
        assertTrue(doc.get("payload").isArray());
        assertEquals(1, doc.get("payload").size());
        assertFalse(doc.get("payload").get(0).get("id").asText().isEmpty());

        assertTrue(categoryRepository.findByName(name).isPresent());
    }
}
