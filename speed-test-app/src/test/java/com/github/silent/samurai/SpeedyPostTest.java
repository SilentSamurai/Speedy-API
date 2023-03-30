package com.github.silent.samurai;

import com.github.silent.samurai.entity.Category;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.service.CategoryRepository;
import com.google.gson.Gson;
import net.bytebuddy.utility.RandomString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.persistence.EntityManagerFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyPostTest {

    Logger logger = LogManager.getLogger(SpeedyPostTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    MetaModelProcessor metaModelProcessor;

    @Autowired
    private MockMvc mvc;

    @Test
    void createCategory() throws Exception {

        Category category = new Category();
        category.setName("generated-cat");

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post("/speedy/v1.0/Category")
                .content(new Gson().toJson(List.of(category)))
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk());


        Optional<Category> categoryOptional = categoryRepository.findByName("generated-cat");
        assertTrue(categoryOptional.isPresent());
    }

    @Test
    void createBadException() throws Exception {

        Category category = new Category();
        category.setName(RandomString.make(251));

        MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders.post("/speedy/v1.0/Category")
                .content(new Gson().toJson(List.of(category)))
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(createRequest)
                .andExpect(status().isBadRequest());

    }

    @Test
    void createConstrainException() throws Exception {

        Category category = new Category();
        category.setName("ex-gen-cat");

        MockHttpServletRequestBuilder createRequest = MockMvcRequestBuilders.post("/speedy/v1.0/Category")
                .content(new Gson().toJson(List.of(category)))
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(createRequest)
                .andExpect(status().isOk());


        Optional<Category> categoryOptional = categoryRepository.findByName("generated-cat");
        assertTrue(categoryOptional.isPresent());

        mvc.perform(createRequest)
                .andExpect(status().isBadRequest());

    }


    @Test
    void bulkCreate() throws Exception {

        List<Category> categories = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            Category category = new Category();
            category.setName("bulk-gen-cat-" + i);
            categories.add(category);
        }

        MockHttpServletRequestBuilder mockHttpServletRequest = MockMvcRequestBuilders.post("/speedy/v1.0/Category")
                .content(new Gson().toJson(categories))
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(mockHttpServletRequest)
                .andExpect(status().isOk());

        for (Category category : categories) {
            Optional<Category> categoryOptional = categoryRepository.findByName(category.getName());
            assertTrue(categoryOptional.isPresent());
        }

    }

}