package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.CategoryApi;
import org.openapitools.client.api.ProductApi;
import org.openapitools.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.github.silent.samurai.speedy.client.SpeedyQuery.condition;
import static com.github.silent.samurai.speedy.client.SpeedyQuery.eq;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyAssociationTest {

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
    void createCategory() {
        CategoryApi apiInstance = new CategoryApi(defaultClient);
        List<CreateCategoryRequest> postCategories = List.of(
                new CreateCategoryRequest().name("new-cat-1"),
                new CreateCategoryRequest().name("new-cat-2")
        );
        apiInstance.bulkCreateCategory(postCategories);
    }


    @Test
    void createProduct() {
        CategoryApi categoryApi = new CategoryApi(defaultClient);
        List<CreateCategoryRequest> postCategories = List.of(
                new CreateCategoryRequest().name("New Category")
        );
        BulkCreateCategoryResponse categoryResponse = categoryApi.bulkCreateCategory(postCategories);

        Assertions.assertNotNull(categoryResponse);
        Assertions.assertNotNull(categoryResponse.getPayload());
        Assertions.assertFalse(categoryResponse.getPayload().isEmpty());
        CategoryKey categoryKey = categoryResponse.getPayload().get(0);
        Assertions.assertNotNull(categoryKey.getId());
        Assertions.assertNotEquals("", categoryKey.getId());


        ProductApi productApi = new ProductApi(defaultClient);
        CreateProductRequest postProduct = new CreateProductRequest()
                .name("New Product")
                .category(new CategoryKey().id(categoryKey.getId()))
                .description("dummy Product");
        BulkCreateProductResponse productsResponse = productApi.bulkCreateProduct(List.of(postProduct));

        Assertions.assertNotNull(productsResponse);
        Assertions.assertNotNull(productsResponse.getPayload());
        Assertions.assertFalse(productsResponse.getPayload().isEmpty());
        ProductKey productKey = productsResponse.getPayload().get(0);
        Assertions.assertNotNull(productKey.getId());
        Assertions.assertNotEquals("", productKey.getId());

//        String query = String.format("(id='%s')", productKey.getId());
        FilteredProductResponse productResponse = productApi.queryProduct(
                SpeedyQuery.from()
                        .where(
                                condition("id", eq(productKey.getId()))
                        ).build()
        );
        Assertions.assertNotNull(productResponse);
        List<Product> productList = productResponse.getPayload();
        Assertions.assertNotNull(productList);
        Assertions.assertEquals(1, productList.size());
        Product product = productList.get(0);
        Assertions.assertNotNull(product.getCategory());
        Assertions.assertEquals(categoryKey.getId(), product.getCategory().getId());


    }

}


