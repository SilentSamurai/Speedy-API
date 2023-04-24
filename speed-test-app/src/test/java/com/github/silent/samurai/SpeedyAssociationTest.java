package com.github.silent.samurai;

import com.github.silent.samurai.service.CategoryRepository;
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
        Function<String, PostCategory> createPostCategory = (String name) -> new PostCategory().name(name);
        List<PostCategory> postCategories = Arrays.asList(
                createPostCategory.apply("new-cat-1"),
                createPostCategory.apply("new-cat-2")
        ); // List<PostCategory> | Fields needed for creation

        apiInstance.createMultipleCategory(postCategories);

    }

    @Test
    void createProduct() throws Exception {
        CategoryApi categoryApi = new CategoryApi(defaultClient);
        List<PostCategory> postCategories = Arrays.asList(
                new PostCategory().name("New Category")
        ); // List<PostCategory> | Fields needed for creation
        categoryApi.createMultipleCategory(postCategories);

        GetAllCategory200Response category = categoryApi.getCategory(String.format("name='%s'", "New Category"));
        List<GetCategory> payload = category.getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertTrue(payload.size() > 0);
        GetCategory getCategory = payload.get(0);
        Assertions.assertEquals("New Category", getCategory.getName());


        ProductApi productApi = new ProductApi(defaultClient);
        PostProduct postProduct = new PostProduct()
                .name("New Product")
                .category(new PostProcurementSupplier().id(getCategory.getId()))
                .description("dummy Product");
        List<PostProduct> postProducts = List.of(postProduct); // List<PostProduct> | Fields needed for creation
        productApi.createMultipleProduct(postProducts);


        GetAllProduct200Response allProduct = productApi.getAllProduct();
        Assertions.assertNotNull(allProduct.getPayload());
        Assertions.assertTrue(allProduct.getPayload().size() > 0);
        GetProduct getProduct = allProduct.getPayload().get(0);
        Assertions.assertEquals("New Product", getProduct.getName());
        Assertions.assertEquals("dummy Product", getProduct.getDescription());

        GetOneProduct200Response oneProduct = productApi.getOneProduct(String.format("id='%s'", getProduct.getId()));
        Assertions.assertNotNull(oneProduct.getPayload());
        GetSingleProduct product = oneProduct.getPayload();
        GetSingleProductCategory category1 = product.getCategory();
        Assertions.assertNotNull(category1);
        Assertions.assertEquals(getCategory.getId(), category1.getId());

    }

}


