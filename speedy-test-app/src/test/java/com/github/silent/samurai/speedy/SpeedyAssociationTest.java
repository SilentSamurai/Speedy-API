package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.repositories.CategoryRepository;
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
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyAssociationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyAssociationTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;
    ApiClient defaultClient;
    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
    }

    @Test
    void createCategory() throws Exception {
        CategoryApi apiInstance = new CategoryApi(defaultClient);
        Function<String, CreateCategoryRequest> createPostCategory = (String name) -> new CreateCategoryRequest().name(name);
        List<CreateCategoryRequest> postCategories = Arrays.asList(
                createPostCategory.apply("new-cat-1"),
                createPostCategory.apply("new-cat-2")
        ); // List<PostCategory> | Fields needed for creation
        apiInstance.bulkCreateCategory(postCategories);
    }

    @Test
    void createProduct() throws Exception {
        CategoryApi categoryApi = new CategoryApi(defaultClient);
        List<CreateCategoryRequest> postCategories = Arrays.asList(
                new CreateCategoryRequest().name("New Category")
        ); // List<PostCategory> | Fields needed for creation
        BulkCreateCategoryResponse categoryResponse = categoryApi.bulkCreateCategory(postCategories);

        Assertions.assertNotNull(categoryResponse);
        Assertions.assertNotNull(categoryResponse.getPayload());
        Assertions.assertTrue(categoryResponse.getPayload().size() > 0);
        CategoryKey getCategory = categoryResponse.getPayload().get(0);
        Assertions.assertNotNull(getCategory.getId());
        Assertions.assertNotEquals("", getCategory.getId());


        ProductApi productApi = new ProductApi(defaultClient);
        CreateProductRequest postProduct = new CreateProductRequest()
                .name("New Product")
                .category(new CategoryKey().id(getCategory.getId()))
                .description("dummy Product");
        BulkCreateProductResponse productsResponse = productApi.bulkCreateProduct(List.of(postProduct));

        Assertions.assertNotNull(productsResponse);
        Assertions.assertNotNull(productsResponse.getPayload());
        Assertions.assertTrue(productsResponse.getPayload().size() > 0);
        ProductKey productKey = productsResponse.getPayload().get(0);
        Assertions.assertNotNull(productKey.getId());
        Assertions.assertNotEquals("", productKey.getId());

        ProductResponse productResponse = productApi.getProduct(productKey.getId());
        Assertions.assertNotNull(productResponse);
        Product product = productResponse.getPayload();
        Assertions.assertNotNull(product);
        Assertions.assertNotNull(product.getCategory());
        Assertions.assertEquals(getCategory.getId(), product.getCategory().getId());


    }

}


