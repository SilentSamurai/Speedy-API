package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
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

import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyPutTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyPutTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

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

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.patch(SpeedyConstant.URI + "/Category/$update")
                .content(CommonUtil.json().createObjectNode()
                        .put("id", category.getId())
                        .put("name", "generated-category-update-modified")
                        .toPrettyString())
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isOk());

        Optional<Category> categoryOptional = categoryRepository.findById(category.getId());
        Assertions.assertTrue(categoryOptional.isPresent());
        Assertions.assertEquals("generated-category-update-modified", categoryOptional.get().getName());
    }

    @Test
    void incompleteKey() throws Exception {
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.put(SpeedyConstant.URI + "/Category(name='not-there')")
                .content("{'name':'generated-cat'}")
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(updateRequest)
                .andExpect(status().isBadRequest());
    }

}
