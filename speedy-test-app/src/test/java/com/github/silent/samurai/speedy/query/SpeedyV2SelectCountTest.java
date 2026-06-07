package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.ProductRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
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

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyV2SelectCountTest {

    @Autowired
    ProductRepository productRepository;


    @Autowired
    private MockMvc mvc;

    private SpeedyTest speedyClient;
    private long totalProduct = 0;

    @BeforeEach
    void setUp() {
        totalProduct = productRepository.count();
        speedyClient = SpeedyTest.mockMvc(mvc);
    }

    /*
       {
           "from": "Product",
           "$where": {},
           "$select" : ['$count']
       }
       * */
    @Test
    void testCountQuery() throws Exception {


        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery.from("Product")
                                .select("$count")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(totalProduct))
                .andReturn();

    }

    /*
       {
           "from": "Product",
           "$where": {},
           "$select" : ['$count']
       }
       * */
    @Test
    void testCountQuery1() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery.from("Product")
                                .select("$count")
                                .where(
                                        condition("id", eq("1"))
                                )
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(1))
                .andReturn();

    }

    @Test
    void testCountQuery2() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery.from("Product")
                                .select("$count")
                                .where(
                                        condition("category.id", eq("2"))
                                )
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(2))
                .andReturn();

    }

    @Test
    void testCountQuery3() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery.from("Product")
                                .select("$count")
                                .expand("category")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(totalProduct))
                .andReturn();

    }

    @Test
    void testCountQuery4() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery.from("Product")
                                .select("$count")
                                .pageNo(2)
                                .pageSize(2)
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(totalProduct))
                .andReturn();

    }

    @Test
    void testCountQuery5() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery.from("Product")
                                .select("$count")
                                .orderByAsc("id")
                                .orderByDesc("name")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(totalProduct))
                .andReturn();

    }

    @Test
    void testMixedCountAndFieldsShouldReturnBadRequest() throws Exception {
        speedyClient.query("Product")
                .select("$count", "id")
                .execute()
                .expectBadRequest();
    }

    @Test
    void testCountQueryWithWhereAndExpand() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Product/" + SpeedyEndpoint.QUERY.suffix())
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery.from("Product")
                                .select("$count")
                                .where(
                                        condition("category.name", eq("cat-1-1"))
                                )
                                .expand("category")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(Matchers.greaterThan(0)))
                .andReturn();

    }

}
