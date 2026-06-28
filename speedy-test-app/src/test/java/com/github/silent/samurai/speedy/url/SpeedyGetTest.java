package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import jakarta.persistence.EntityManagerFactory;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyGetTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyGetTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;

    @BeforeEach
    void setUp() {
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    @Test
    void getViaPrimaryKey() throws Exception {

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(SpeedyConstants.URI + "/Category?id='1'")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").value("cat-1-1"))
                .andReturn();
    }

    @Test
    void getVia() throws Exception {

        MvcResult mvcResult = mvc.perform(get(SpeedyConstants.URI + "/Category?id='not-there'")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(0)))
                .andReturn();
    }

    @Test
    void getAll() throws Exception {

        mvc.perform(get(SpeedyConstants.URI + "/Category/")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andReturn();
    }

    @Test
    void getViaFilter() throws Exception {

        mvc.perform(get(SpeedyConstants.URI + "/Category?name='cat-1-1'")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.payload[*].name",
                                Matchers.contains("cat-1-1")
                        )
                )
                .andReturn();
    }


    @Test
    void getViaFilterArg() throws Exception {

        mvc.perform(get(SpeedyConstants.URI + "/Category?id='1'")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").value("cat-1-1"))
                .andReturn();
    }

    @Test
    void getAssociation() throws Exception {

        mvc.perform(get(SpeedyConstants.URI + "/Product?category.id = '1'")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(2))))
                .andReturn();
    }

    @Test
    void getviadoublequotes() throws Exception {

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(SpeedyConstants.URI + "/Category?id=\"1\"")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").value("cat-1-1"))
                .andReturn();
    }

    @Test
    void getWithExpand() throws Exception {
        mvc.perform(get(SpeedyConstants.URI + "/Product?$expand=Category")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThan(0))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].category.name").exists())
                .andReturn();
    }

    @Test
    void getWithPagination() throws Exception {
        mvc.perform(get(SpeedyConstants.URI + "/Product?$page=0&$pageSize=3")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.pageIndex").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.pageSize").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(3)))
                .andReturn();
    }

    @Test
    void getWithMultiConditionAnd() throws Exception {
        mvc.perform(get(SpeedyConstants.URI + "/Product?name='prod-1-1'&category.id=1")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andReturn();
    }

    @Test
    void getWithSelectFields() throws Exception {
        speedyClient.get("Product")
                .select("id", "name", "description")
                .execute()
                .expectOk()
                .expectJsonPathExists("$.payload")
                .expectJsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThan(0)))
                .expectJsonPathExists("$.payload[0].id")
                .expectJsonPathExists("$.payload[0].name")
                .expectJsonPathExists("$.payload[0].description")
                .expectJsonPathDoesNotExist("$.payload[0].category");
    }

    @Test
    void getWithSelectCount() throws Exception {
        speedyClient.get("Product")
                .select("$count")
                .execute()
                .expectOk()
                .expectJsonPathExists("$.count")
                .expectJsonPath("$.count", Matchers.isA(Number.class));
    }

    @Test
    void getSelectMixedCountAndFieldsShouldBadRequest() {
        speedyClient.get("Product")
                .select("$count", "id")
                .execute()
                .expectBadRequest();
    }

    @Test
    void getWithPageSizeExceedsMax() throws Exception {
        speedyClient.get("Product")
                .pageSize(5000)
                .pageNo(0)
                .execute()
                .expectBadRequest();
    }
}
