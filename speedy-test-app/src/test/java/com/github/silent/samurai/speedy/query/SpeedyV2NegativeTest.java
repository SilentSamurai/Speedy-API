package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyV2NegativeTest {

    @Autowired
    private MockMvc mvc;

    /*
       {
           "from": "Product",
           "expand": ["Category"],
       }
       * */
    @Test
    void single_level_expand() throws Exception {

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders
                .post(SpeedyConstant.URI + "/NotPresent/$query")
                .content(CommonUtil.json().writeValueAsString(
                        SpeedyQuery.from("Product")
                                .prettyPrint()
                                .build()
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE);


        mvc.perform(mockHttpServletRequest)
                .andExpect(status().is4xxClientError())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

    }

}
