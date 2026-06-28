package com.github.silent.samurai.speedy.docs;

import com.github.silent.samurai.speedy.TestApplication;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates the runtime-generated OpenAPI document (served at {@code /v3/api-docs}) against the
 * OpenAPI 3.0 specification using swagger-parser — the same engine code generators rely on. A
 * clean parse with no messages means the document is well-formed enough to generate a client from
 * (valid types, resolvable {@code $ref}s, well-formed paths/operations/responses).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenApiSpecValidityTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void generatedSpecIsValidOpenApi() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(json);

        assertThat(result.getMessages())
                .as("OpenAPI 3.0 validation problems in the generated spec")
                .isEmpty();
        assertThat(result.getOpenAPI())
                .as("swagger-parser should produce a parsed OpenAPI model")
                .isNotNull();
    }
}
