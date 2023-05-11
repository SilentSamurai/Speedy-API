package com.github.silent.samurai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.service.CategoryRepository;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.*;
import org.openapitools.client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyEntityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedyEntityTest.class);

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    CategoryRepository categoryRepository;

    ApiClient defaultClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mvc);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        defaultClient = new ApiClient(restTemplate);
    }

    Company createCompany() {
        Instant datetime = Instant.now();
        CreateCompanyRequest createCompanyRequest = new CreateCompanyRequest();
        createCompanyRequest.name("New Company")
                .address("Address")
                .defaultGenerator(12)
                .extra("extra asp")
                .createdAt(datetime)
                .deletedAt(datetime)
                .invoiceNo(12)
                .currency("INR")
                .phone("0987383762")
                .detailsTop("asd")
                .email("poasdnfi@asd.com");

        CompanyApi companyApi = new CompanyApi(defaultClient);
        BulkCreateCompanyResponse bulkCreateCompany = companyApi.bulkCreateCompany(Lists.newArrayList(createCompanyRequest));

        CompanyKey companyKey = bulkCreateCompany.getPayload().get(0);


        CompanyResponse company200Response = companyApi.getCompany(companyKey.getId());
        Company company = company200Response.getPayload();

        LOGGER.info("company {}", company);
        return company;
    }

    Product createProduct() throws Exception {
        CategoryApi categoryApi = new CategoryApi(defaultClient);
        List<CreateCategoryRequest> postCategories = Arrays.asList(
                new CreateCategoryRequest().name("New Category ALL")
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
                .name("New Product All")
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

        LOGGER.info(" {} ", product);

        return product;
    }

    Supplier createSupplier() throws Exception {

        Instant dateTimeInstant = Instant.now();

        CreateSupplierRequest createSupplierRequest = new CreateSupplierRequest();
        createSupplierRequest.name("new Supplier")
                .address("ABCD aiohwef")
                .createdAt(dateTimeInstant)
                .createdBy("Happy Singh")
                .email("abcd@smainsda.cs")
                .altPhoneNo("8594093448")
                .phoneNo("8594094438");

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Supplier")
                .content(objectMapper.writeValueAsString(Lists.newArrayList(createSupplierRequest)))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mvc.perform(getRequest)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.payload[*].id").exists())
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        BulkCreateSupplierResponse apiResponse = objectMapper.readValue(contentAsString, BulkCreateSupplierResponse.class);

        Assertions.assertNotNull(apiResponse);
        Assertions.assertNotNull(apiResponse.getPayload());
        Assertions.assertTrue(apiResponse.getPayload().size() > 0);
        Assertions.assertNotNull(apiResponse.getPayload().get(0));
        SupplierKey supplierKey = apiResponse.getPayload().get(0);

        Assertions.assertNotNull(supplierKey.getId());
        Assertions.assertFalse(supplierKey.getId().isBlank());

        SupplierApi supplierApi = new SupplierApi(defaultClient);

        SupplierResponse supplier200Response = supplierApi.getSupplier(supplierKey.getId());
        Supplier supplier = supplier200Response.getPayload();

        LOGGER.info("Supplier {}", supplier);
        assert supplier != null;
        Assertions.assertNotNull(supplier.getCreatedAt());
        Assertions.assertTrue(supplier.getCreatedAt().toEpochMilli() - dateTimeInstant.toEpochMilli() <= 1000);
        return supplier;
    }

    Procurement createProcurement(Product product, Supplier supplier) throws Exception {
        CreateProcurementRequest createProcurementRequest = new CreateProcurementRequest();
        createProcurementRequest
                .purchaseDate(Instant.now())
                .supplier(new SupplierKey().id(supplier.getId()))
                .dueAmount(3.8)
                .modifiedAt(Instant.now())
                .createdAt(Instant.now())
                .modifiedBy("asdads")
                .createdBy("asfasf")
                .product(new ProductKey().id(product.getId()))
                .amount(2.0);

        ProcurementApi procurementApi = new ProcurementApi(defaultClient);
        BulkCreateProcurementResponse bulkCreateProcurement200Response = procurementApi.bulkCreateProcurement(Lists.newArrayList(createProcurementRequest));
        ProcurementKey procurementKey = bulkCreateProcurement200Response.getPayload().get(0);

        ProcurementResponse procurement = procurementApi.getProcurement(procurementKey.getId());

        LOGGER.info(" {} ", procurement);

        return procurement.getPayload();

    }

    Customer createCustomer() {
        CreateCustomerRequest createCustomerRequest = new CreateCustomerRequest();
        createCustomerRequest.createdAt(Instant.now())
                .altPhoneNo("0984738260")
                .createdBy("asfasf")
                .phoneNo("0984738269")
                .email("aksmfaksmf@sad.cc")
                .name("New Customer")
                .address("gg4g");

        CustomerApi customerApi = new CustomerApi(defaultClient);

        BulkCreateCustomerResponse bulkCreateCustomer200Response = customerApi.bulkCreateCustomer(Lists.newArrayList(createCustomerRequest));

        CustomerKey customerKey = bulkCreateCustomer200Response.getPayload().get(0);

        CustomerResponse getCustomer200Response = customerApi.getCustomer(customerKey.getId());

        Customer payload = getCustomer200Response.getPayload();

        LOGGER.info(" {} ", payload);
        return payload;
    }

    Invoice createInvoice(Customer customer) {
        CreateInvoiceRequest createInvoiceRequest = new CreateInvoiceRequest();
        createInvoiceRequest.createdAt(Instant.now())
                .dueAmount(23.7)
                .paid(34.0)
                .notes("asf")
                .discount(24354.9)
                .modifiedBy("josng")
                .customer(new CustomerKey().id(customer.getId()))
                .modifiedAt(Instant.now())
                .invoiceDate(Instant.now())
                .adjustment(4545.5)
                .createdBy("ABCD");


        InvoiceApi invoiceApi = new InvoiceApi(defaultClient);
        BulkCreateInvoiceResponse bulkCreateInvoice200Response = invoiceApi.bulkCreateInvoice(Lists.newArrayList(createInvoiceRequest));

        InvoiceKey invoiceKey = bulkCreateInvoice200Response.getPayload().get(0);

        InvoiceResponse invoice200Response = invoiceApi.getInvoice(invoiceKey.getId());

        Invoice payload = invoice200Response.getPayload();
        LOGGER.info(" {} ", payload);
        return payload;
    }

    User createUser(Company company) {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.createdAt(Instant.now())
                .createdAt(Instant.now())
                .deletedAt(Instant.now())
                .updatedAt(Instant.now())
                .phoneNo("0984738269")
                .email("aksmfaksmf@sad.cc")
                .name("New Customer")
                .company(new CompanyKey().id(company.getId()))
                .profilePic("gg4g");

        UserApi userApi = new UserApi(defaultClient);

        BulkCreateUserResponse bulkCreateUser200Response = userApi.bulkCreateUser(Lists.newArrayList(createUserRequest));

        UserKey userKey = bulkCreateUser200Response.getPayload().get(0);

        UserResponse getUser200Response = userApi.getUser(userKey.getId());

        User payload = getUser200Response.getPayload();
        LOGGER.info(" {} ", payload);
        return payload;
    }

    Inventory createInventory(Procurement procurement, Product product, Invoice invoice) {
        CreateInventoryRequest createInventoryRequest = new CreateInventoryRequest();
        createInventoryRequest.cost(230.0)
                .soldPrice(230.0)
                .discount(230.0)
                .listingPrice(230.0)
                .invoice(new InvoiceKey().id(invoice.getId()))
                .product(new ProductKey().id(product.getId()))
                .procurement(new ProcurementKey().id(procurement.getId()));

        InventoryApi inventoryApi = new InventoryApi(defaultClient);

        BulkCreateInventoryResponse bulkCreateInventory200Response = inventoryApi.bulkCreateInventory(Lists.newArrayList(createInventoryRequest));

        InventoryKey userKey = bulkCreateInventory200Response.getPayload().get(0);

        InventoryResponse getInventory200Response = inventoryApi.getInventory(userKey.getId());

        Inventory payload = getInventory200Response.getPayload();
        LOGGER.info(" {} ", payload);
        return payload;
    }

    @Test
    void normal() throws Exception {


        Company company = createCompany();
        Product product = createProduct();
        Supplier supplier = createSupplier();

        Procurement procurement = createProcurement(product, supplier);

        Customer customer = createCustomer();

        Invoice invoice = createInvoice(customer);

        User user = createUser(company);

        Inventory inventory = createInventory(procurement, product, invoice);

        Assertions.assertNotNull(inventory);
        Assertions.assertFalse(inventory.getId().isBlank());


    }


}


