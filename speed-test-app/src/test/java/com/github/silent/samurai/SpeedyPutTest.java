package com.github.silent.samurai;

import com.github.silent.samurai.entity.Category;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
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
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyPutTest {

    Logger logger = LogManager.getLogger(SpeedyPutTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    MetaModelProcessor metaModelProcessor;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    private MockMvc mvc;

    @Test
    void updateCategory() throws Exception {

        Category category = new Category();
        category.setName("generated-category-update");
        categoryRepository.save(category);

        Assertions.assertNotNull(category.getId());

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.put("/speedy/v1.0/Category(id='" + category.getId() + "')")
                .content("{'name':'generated-category-update-modified'}")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isOk());

        Optional<Category> categoryOptional = categoryRepository.findById(category.getId());
        Assertions.assertTrue(categoryOptional.isPresent());
        Assertions.assertEquals("generated-category-update-modified", categoryOptional.get().getName());
    }

    @Test
    void incompleteKey() throws Exception {
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.put("/speedy/v1.0/Category(name='not-there')")
                .content("{'name':'generated-cat'}")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isBadRequest());
    }

}
