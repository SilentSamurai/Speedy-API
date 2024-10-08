package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.Customer;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import net.bytebuddy.utility.RandomString;
import org.hamcrest.Matchers;
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

import javax.persistence.EntityManagerFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyPostTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyPostTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    private MockMvc mvc;

    @Test
    void createCategory() throws Exception {

        Category category = new Category();
        category.setName("generated-cat");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$create")
                .content(CommonUtil.toJson(List.of(category)))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();


        Optional<Category> categoryOptional = categoryRepository.findByName("generated-cat");
        assertTrue(categoryOptional.isPresent());
    }

    @Test
    void createBadException() throws Exception {

        Category category = new Category();
        category.setName(RandomString.make(251));

        MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$create")
                .content(CommonUtil.toJson(List.of(category)))
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(createRequest)
                .andExpect(status().isBadRequest());

    }

    @Test
    void createBadException2() throws Exception {

        Category category = new Category();
        category.setName("");

        MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$create")
                .content(CommonUtil.toJson(List.of(category)))
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(createRequest)
                .andExpect(status().isBadRequest());

    }

    @Test
    void createBadRequestException() throws Exception {

        Customer customer = new Customer();
        customer.setPhoneNo("+91-378-433-1234");
        customer.setEmail("thisisatestemail");
        customer.setAddress("this is a address");

        MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Customer/$create")
                .content(CommonUtil.toJson(List.of(customer)))
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(createRequest)
                .andExpect(status().isBadRequest());

    }

    @Test
    void createConstrainException() throws Exception {

        Category category = new Category();
        category.setName("ex-gen-cat");

        MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$create")
                .content(CommonUtil.toJson(List.of(category)))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mvc.perform(createRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();


        Optional<Category> categoryOptional = categoryRepository.findByName("generated-cat");
        assertTrue(categoryOptional.isPresent());

        mvc.perform(createRequest)
                .andExpect(status().isBadRequest());

    }


    @Test
    void bulkCreate() throws Exception {

        int creationCount = 10;
        List<Category> categories = new LinkedList<>();
        for (int i = 0; i < creationCount; i++) {
            Category category = new Category();
            category.setName("bulk-gen-cat-" + i);
            categories.add(category);
        }

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Category/$create")
                .content(CommonUtil.toJson(categories))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*]", Matchers.hasSize(creationCount)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id")
                        .value(Matchers.everyItem(Matchers.isA(String.class))))
                .andReturn();

        for (Category category : categories) {
            Optional<Category> categoryOptional = categoryRepository.findByName(category.getName());
            assertTrue(categoryOptional.isPresent());
        }

    }

}
