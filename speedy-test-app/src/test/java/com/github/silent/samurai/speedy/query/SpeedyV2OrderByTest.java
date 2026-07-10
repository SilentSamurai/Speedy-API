package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import net.bytebuddy.utility.RandomString;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.matches;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyV2OrderByTest {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    private MockMvc mvc;

    /*
       {
           "from": "Category",
           "orderBy": {
               "name": "desc"
           }
       }
       * */
    @Test
    void testQuery1() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$orderBy")
                .put("name", "DESC");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();


    }

    @Test
    void check_order_by_with_actual_names() throws Exception {
        // Create a batch of categories scoped to this test run, so the assertions
        // below don't depend on how many Category rows other tests have left behind.
        String prefix = "ordby-" + RandomString.make(8) + "-";
        List<String> suffixes = List.of("c", "a", "e", "b", "d");

        ObjectMapper json = new ObjectMapper();
        List<ObjectNode> items = suffixes.stream()
                .map(suffix -> json.createObjectNode().put("name", prefix + suffix))
                .toList();
        SpeedyTest.mockMvc(mvc).createMany("Category", items);

        List<Category> categories = categoryRepository.findAllByNameStartingWithOrderByNameAsc(prefix);
        List<String> expectedNames = categories.stream()
                .map(Category::getName)
                .toList();
        List<String> reverseName = new ArrayList<>(expectedNames);
        Collections.reverse(reverseName);

        JsonNode body = SpeedyQuery.from("Category")
                .where(condition("name", matches(prefix + "*")))
                .orderByAsc("name")
                .pageSize(suffixes.size())
                .prettyPrint()
                .build();

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(contains(expectedNames.toArray())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.not(contains(reverseName.toArray()))));
    }


    /*
       {
           "from": "Category",
           "orderBy": {
               "name": "asc"
           }
       }
       * */
    @Test
    void testQuery2() throws Exception {
        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("$from", "Category");
        body.putObject("$orderBy")
                .put("name", "ASC");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstants.URI + "/Category/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].name")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();

    }


}
