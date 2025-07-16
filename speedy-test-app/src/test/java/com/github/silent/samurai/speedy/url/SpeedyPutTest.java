package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.CategoryApi;
import org.openapitools.client.api.ProductApi;
import org.openapitools.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    private ApiClient defaultClient;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
    }

    @Test
    void updateCategory() throws Exception {

        Category category = new Category();
        category.setName("generated-category-update");
        categoryRepository.save(category);

        assertNotNull(category.getId());

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.patch(SpeedyConstant.URI + "/Category/$update")
                .content(CommonUtil.json().createObjectNode()
                        .put("id", String.valueOf(category.getId()))
                        .put("name", "generated-category-update-modified")
                        .toPrettyString())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(updateRequest)
                .andExpect(status().isOk());

        Optional<Category> categoryOptional = categoryRepository.findById(category.getId());
        Assertions.assertTrue(categoryOptional.isPresent());
        Assertions.assertEquals("generated-category-update-modified", categoryOptional.get().getName());
    }

    @Test
    void updateAssosiation() throws Exception {
        ProductApi productApi = new ProductApi(defaultClient);

        FilteredProductResponse getResp = productApi.getProduct("7");
        assertNotNull(getResp);
        assertNotNull(getResp.getPayload());
        assertFalse(getResp.getPayload().isEmpty());
        Product getProduct = getResp.getPayload().get(0);
        assertNotNull(getProduct.getId());
        assertNotNull(getProduct.getName());
        assertNotNull(getProduct.getDescription());
        assertNotNull(getProduct.getCategory());
        assertEquals("3", getProduct.getCategory().getId());

        UpdateProductRequest productRequest = new UpdateProductRequest();
        productRequest.setId("7");
        productRequest.setCategory(new CategoryKey().id("1"));
        UpdateProductResponse response = productApi.updateProduct(productRequest);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertFalse(response.getPayload().isEmpty());
        Product product = response.getPayload().get(0);
        assertNotNull(product.getId());
        assertNotNull(product.getName());
        assertNotNull(product.getDescription());
        assertNotNull(product.getCategory());
        assertEquals("1", product.getCategory().getId());
    }

    @Test
    void incompleteKey() throws Exception {
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.put(SpeedyConstant.URI + "/Category(name='not-there')")
                .content("{'name':'generated-cat'}")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        mvc.perform(updateRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingKey() throws Exception {
        assertThrows(Exception.class, () -> {
            CategoryApi categoryApi = new CategoryApi(defaultClient);
            UpdateCategoryRequest categoryRequest = new UpdateCategoryRequest();
            String value = CommonUtil.generateString(300);
            categoryRequest.setName(value);
            UpdateCategoryResponse response = categoryApi.updateCategory(categoryRequest);
            assertNotNull(response);
            assertNotNull(response.getPayload());
            List<org.openapitools.client.model.Category> payload = response.getPayload();
        });
    }

    @Test
    void failValidation() throws Exception {
        assertThrows(Exception.class, () -> {
            CategoryApi categoryApi = new CategoryApi(defaultClient);
            UpdateCategoryRequest categoryRequest = new UpdateCategoryRequest();
            String value = CommonUtil.generateString(300);
            categoryRequest.setId("1");
            categoryRequest.setName(value);
            UpdateCategoryResponse response = categoryApi.updateCategory(categoryRequest);
            assertNotNull(response);
            assertNotNull(response.getPayload());
            List<org.openapitools.client.model.Category> payload = response.getPayload();
        });
    }

}
