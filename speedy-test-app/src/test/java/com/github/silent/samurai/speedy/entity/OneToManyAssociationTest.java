package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// One-to-many fields cannot be represented by Speedy's current flat FK model, so the JPA metadata
/// processor must omit them while retaining the usable many-to-one side for query expansion.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class OneToManyAssociationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void metadataOmitsUnsupportedOneToManyCollectionButKeepsTheEntityAvailable() throws Exception {
        mvc.perform(get(SpeedyConstants.URI + SpeedyEndpoint.METADATA.path())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Category')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'Category')].fields[?(@.outputProperty == 'products')]")
                        .doesNotExist());
    }
}
