package com.github.silent.samurai;

import com.github.silent.samurai.entity.Category;
import com.github.silent.samurai.service.CategoryRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.persistence.EntityManagerFactory;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyDeleteTest {

    Logger logger = LogManager.getLogger(SpeedyDeleteTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    private MockMvc mvc;

    @Test
    void deleteCategory() throws Exception {

        Category category = new Category();
        category.setName("generated-category-delete");
        categoryRepository.save(category);

        Assertions.assertNotNull(category.getId());

        long count = categoryRepository.count();


        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.delete("/speedy/v1.0/Category/")
                .content("[{'id':'" + category.getId() + "'}]")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isOk());

        Assertions.assertEquals(count - 1, categoryRepository.count());

    }

    @Test
    void incompleteKey() throws Exception {
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.delete("/speedy/v1.0/Category/")
                .content("[{'name':'1'}]")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyContent() throws Exception {
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.delete("/speedy/v1.0/Category/")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isBadRequest());

        updateRequest = MockMvcRequestBuilders.delete("/speedy/v1.0/Category/")
                .content("")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isBadRequest());
    }

}
