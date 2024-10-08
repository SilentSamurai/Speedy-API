package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.persistence.EntityManagerFactory;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyDeleteTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyDeleteTest.class);

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
//        category.setId(UUID.randomUUID().toString());
        categoryRepository.save(category);

        Assertions.assertNotNull(category.getId());

        long count = categoryRepository.count();


        MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders.delete(SpeedyConstant.URI + "/Category/$delete")
                .content("[{'id':'" + category.getId() + "'}]")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(deleteRequest)
                .andExpect(status().isOk());

        Assertions.assertEquals(count - 1, categoryRepository.count());

    }

    @Test
    void incompleteKey() throws Exception {
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.delete(SpeedyConstant.URI + "/Category/$update")
                .content("[{'name':'1'}]")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyContent() throws Exception {
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.delete(SpeedyConstant.URI + "/Category/$delete")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isBadRequest());

        updateRequest = MockMvcRequestBuilders.delete(SpeedyConstant.URI + "/Category/")
                .content("")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isBadRequest());
    }

}
