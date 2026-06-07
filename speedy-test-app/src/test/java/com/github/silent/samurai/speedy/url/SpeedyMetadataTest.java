package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyMetadataTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void getCategoryMetadata_returnsEntitySpecificData() throws Exception {
        mvc.perform(get(SpeedyConstant.URI + "/Category/" + SpeedyEndpoint.METADATA.suffix())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$").isMap())
                .andReturn();
    }

    @Test
    void getProductMetadata_returnsMetadata() throws Exception {
        mvc.perform(get(SpeedyConstant.URI + "/Product/" + SpeedyEndpoint.METADATA.suffix())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andReturn();
    }

    @Test
    void getGlobalMetadata_showsSensitiveFields() throws Exception {
        mvc.perform(get(SpeedyConstant.URI + SpeedyEndpoint.METADATA.path())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='SensitiveClassEntity')].sensitive").value(true))
                .andExpect(jsonPath("$[?(@.name=='SensitiveTestEntity')].fields[?(@.outputProperty=='secretField')].sensitive").value(true))
                .andExpect(jsonPath("$[?(@.name=='SensitiveTestEntity')].fields[?(@.outputProperty=='publicField')].sensitive").value(false))
                .andReturn();
    }
}
