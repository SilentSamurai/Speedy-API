package com.github.silent.samurai.speedy.url;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
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

import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.$condition;
import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.$eq;
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

    Company crudCompany() {
        String datetime = Instant.now().toString();
        CreateCompanyRequest createCompanyRequest = new CreateCompanyRequest();
        createCompanyRequest.name("New Company")
                .address("Address")
                .defaultGenerator(12L)
                .extra("extra asp")
                .createdAt(datetime)
                .deletedAt(datetime)
                .invoiceNo(12L)
                .currency("INR")
                .phone("0987383762")
                .detailsTop("asd")
                .email("poasdnfi@asd.com");

        CompanyApi companyApi = new CompanyApi(defaultClient);
        BulkCreateCompanyResponse bulkCreateCompany = companyApi.bulkCreateCompany(Lists.newArrayList(createCompanyRequest));

        // assert payload has at least one element
        Assertions.assertNotNull(bulkCreateCompany.getPayload());
        Assertions.assertFalse(bulkCreateCompany.getPayload().isEmpty());

        CompanyKey companyKey = bulkCreateCompany.getPayload().get(0);


        FilteredCompanyResponse companyResponse = companyApi.getCompany(companyKey.getId());
        List<Company> company = companyResponse.getPayload();
        Company lightCompany = company.get(0);
        LOGGER.info("company {}", lightCompany);
        return lightCompany;
    }

    Product crudProduct() throws Exception {
        CategoryApi categoryApi = new CategoryApi(defaultClient);
        List<CreateCategoryRequest> postCategories = Arrays.asList(
                new CreateCategoryRequest().name("New Category ALL")
        ); // List<PostCategory> | Fields needed for creation
        BulkCreateCategoryResponse categoryResponse = categoryApi.bulkCreateCategory(postCategories);

        Assertions.assertNotNull(categoryResponse);
        Assertions.assertNotNull(categoryResponse.getPayload());
        Assertions.assertFalse(categoryResponse.getPayload().isEmpty());
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
        Assertions.assertFalse(productsResponse.getPayload().isEmpty());
        ProductKey productKey = productsResponse.getPayload().get(0);
        Assertions.assertNotNull(productKey.getId());
        Assertions.assertNotEquals("", productKey.getId());

        List<Product> payload = productApi.getProduct(productKey.getId()).getPayload();
        Assertions.assertNotNull(payload);
        Assertions.assertFalse(payload.isEmpty());
        Product product = payload.get(0);
        Assertions.assertNotNull(product);
        Assertions.assertNotNull(product.getCategory());
        Assertions.assertEquals(getCategory.getId(), product.getCategory().getId());

        LOGGER.info(" {} ", product);

        return product;
    }

    Supplier crudSupplier() throws Exception {

        String dateTimeInstant = Instant.now().toString();

        CreateSupplierRequest createSupplierRequest = new CreateSupplierRequest();
        createSupplierRequest.name("new Supplier")
                .address("ABCD aiohwef")
                .createdAt(dateTimeInstant)
                .createdBy("Happy Singh")
                .email("abcd@smainsda.cs")
                .altPhoneNo("8594093448")
                .phoneNo("8594094438");

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.post(SpeedyConstant.URI + "/Supplier/$create")
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
        Assertions.assertFalse(apiResponse.getPayload().isEmpty());
        Assertions.assertNotNull(apiResponse.getPayload().get(0));
        SupplierKey supplierKey = apiResponse.getPayload().get(0);

        Assertions.assertNotNull(supplierKey.getId());
        Assertions.assertFalse(supplierKey.getId().isBlank());

        SupplierApi supplierApi = new SupplierApi(defaultClient);

        List<Supplier> payload = supplierApi.getSupplier(supplierKey.getId()).getPayload();
        Supplier supplier = payload.get(0);

        LOGGER.info("Supplier {}", supplier);
        assert supplier != null;
        Assertions.assertNotNull(supplier.getCreatedAt());

        Instant createdAt = Instant.parse(supplier.getCreatedAt());

        Assertions.assertTrue(
                createdAt.toEpochMilli() -
                        Instant.parse(dateTimeInstant).toEpochMilli() <= 1000);
        return supplier;
    }

    Procurement crudProcurement(Product product, Supplier supplier) throws Exception {
        String dateTimeInstant = Instant.now().toString();
        CreateProcurementRequest createProcurementRequest = new CreateProcurementRequest();
        createProcurementRequest
                .purchaseDate(dateTimeInstant)
                .supplier(new SupplierKey().id(supplier.getId()))
                .dueAmount(3.8)
                .modifiedAt(dateTimeInstant)
                .createdAt(dateTimeInstant)
                .modifiedBy("asdads")
                .createdBy("asfasf")
                .product(new ProductKey().id(product.getId()))
                .amount(2.0);

        ProcurementApi procurementApi = new ProcurementApi(defaultClient);
        BulkCreateProcurementResponse bulkCreateProcurement200Response = procurementApi.bulkCreateProcurement(Lists.newArrayList(createProcurementRequest));
        ProcurementKey procurementKey = bulkCreateProcurement200Response.getPayload().get(0);

        List<Procurement> payload = procurementApi.getProcurement(procurementKey.getId()).getPayload();
        Procurement lightProcurement = payload.get(0);
        LOGGER.info(" {} ", lightProcurement);

        return lightProcurement;

    }

    Customer crudCustomer() {
        CreateCustomerRequest createCustomerRequest = new CreateCustomerRequest();
        String dateTimeInstant = Instant.now().toString();
        createCustomerRequest.createdAt(dateTimeInstant)
                .altPhoneNo("0984738260")
                .createdBy("asfasf")
                .phoneNo("0984738269")
                .email("aksmfaksmf@sad.cc")
                .name("New Customer")
                .address("gg4g");

        CustomerApi customerApi = new CustomerApi(defaultClient);

        BulkCreateCustomerResponse bulkCreateCustomer200Response = customerApi.bulkCreateCustomer(Lists.newArrayList(createCustomerRequest));

        CustomerKey customerKey = bulkCreateCustomer200Response.getPayload().get(0);

        List<Customer> payload1 = customerApi.getCustomer(customerKey.getId()).getPayload();

        Customer customer = payload1.get(0);

        LOGGER.info(" {} ", customer);
        return customer;
    }

    Invoice crudInvoice(Customer customer) {
        String dateTimeInstant = Instant.now().toString();
        CreateInvoiceRequest createInvoiceRequest = new CreateInvoiceRequest();
        createInvoiceRequest.createdAt(dateTimeInstant)
                .dueAmount(23.7)
                .paid(34.0)
                .notes("asf")
                .discount(24354.9)
                .modifiedBy("josng")
                .customer(new CustomerKey().id(customer.getId()))
                .modifiedAt(dateTimeInstant)
                .invoiceDate(dateTimeInstant)
                .adjustment(4545.5)
                .createdBy("ABCD");


        InvoiceApi invoiceApi = new InvoiceApi(defaultClient);
        BulkCreateInvoiceResponse bulkCreateInvoice200Response = invoiceApi.bulkCreateInvoice(Lists.newArrayList(createInvoiceRequest));

        InvoiceKey invoiceKey = bulkCreateInvoice200Response.getPayload().get(0);

        List<Invoice> payload1 = invoiceApi.getInvoice(invoiceKey.getId()).getPayload();
        Invoice lightInvoice = payload1.get(0);
        LOGGER.info(" {} ", lightInvoice);
        return lightInvoice;
    }

    User crudUser(Company company) {
        String dateTimeInstant = Instant.now().toString();
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.createdAt(dateTimeInstant)
                .createdAt(dateTimeInstant)
                .deletedAt(dateTimeInstant)
                .updatedAt(dateTimeInstant)
                .phoneNo("0984738269")
                .email("aksmfaksmf@sad.cc")
                .name("New Customer")
                .company(new CompanyKey().id(company.getId()))
                .profilePic("gg4g");

        UserApi userApi = new UserApi(defaultClient);

        BulkCreateUserResponse bulkCreateUser200Response = userApi.bulkCreateUser(Lists.newArrayList(createUserRequest));

        UserKey userKey = bulkCreateUser200Response.getPayload().get(0);

        List<User> payload1 = userApi.getUser(userKey.getId()).getPayload();

        User payload = payload1.get(0);
        LOGGER.info(" {} ", payload);
        return payload;
    }

    Inventory crudInventory(Procurement procurement, Product product, Invoice invoice) {
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

        List<Inventory> payload1 = inventoryApi.getInventory(userKey.getId()).getPayload();

        Inventory payload = payload1.get(0);
        LOGGER.info(" {} ", payload);
        return payload;
    }

    void crudCurrency() throws JsonProcessingException {
        CurrencyApi currencyApi = new CurrencyApi(defaultClient);
        CreateCurrencyRequest createCurrencyRequest = new CreateCurrencyRequest();
        createCurrencyRequest.currencyAbbr("CED")
                .currencyName("Core Demo Currency")
                .currencySymbol("yhd")
                .country("Earth");

        BulkCreateCurrencyResponse bulkCreateCurrency200Response = currencyApi
                .bulkCreateCurrency(Lists.newArrayList(createCurrencyRequest));

        Assertions.assertNotNull(bulkCreateCurrency200Response);
        Assertions.assertNotNull(bulkCreateCurrency200Response.getPayload());
        Assertions.assertFalse(bulkCreateCurrency200Response.getPayload().isEmpty());
        Assertions.assertNotNull(bulkCreateCurrency200Response.getPayload().get(0));

        CurrencyKey currencyKey = bulkCreateCurrency200Response.getPayload().get(0);

        Assertions.assertNotNull(currencyKey.getId());
        Assertions.assertFalse(currencyKey.getId().isBlank());

        FilteredCurrencyResponse filteredCurrencyResponse = currencyApi.getCurrency(currencyKey.getId());

        Assertions.assertNotNull(filteredCurrencyResponse);
        Assertions.assertNotNull(filteredCurrencyResponse.getPayload());
        Assertions.assertFalse(filteredCurrencyResponse.getPayload().isEmpty());
        Assertions.assertNotNull(filteredCurrencyResponse.getPayload().get(0));

        Currency currency = filteredCurrencyResponse.getPayload().get(0);

        Assertions.assertNotNull(currency);
        Assertions.assertNotNull(currency.getCreatedAt());
        Assertions.assertNotNull(currency.getCountry());
        Assertions.assertNotNull(currency.getCurrencyAbbr());
        Assertions.assertNotNull(currency.getCurrencyName());
        Assertions.assertNotNull(currency.getCurrencySymbol());
        Assertions.assertNotNull(currency.getId());

        Assertions.assertEquals("CED", currency.getCurrencyAbbr());
        Assertions.assertEquals("Core Demo Currency", currency.getCurrencyName());
        Assertions.assertFalse(currency.getCurrencySymbol().isBlank());
        Assertions.assertEquals("Earth", currency.getCountry());

        LOGGER.info("filteredCurrencyResponse {}", currency);

        UpdateCurrencyRequest updateCurrencyRequest = new UpdateCurrencyRequest();
        updateCurrencyRequest.country("Earth2");
        updateCurrencyRequest.setId(currencyKey.getId());

        UpdateCurrencyResponse updateCurrency200Response = currencyApi
                .updateCurrency(updateCurrencyRequest);

        Assertions.assertNotNull(updateCurrency200Response);
        Assertions.assertNotNull(updateCurrency200Response.getPayload());
        Currency updatedCurrency = updateCurrency200Response.getPayload();

        Assertions.assertNotNull(updatedCurrency);
        Assertions.assertNotNull(updatedCurrency.getCountry());
        Assertions.assertEquals("Earth2", updatedCurrency.getCountry());


        filteredCurrencyResponse = currencyApi.getCurrency(currencyKey.getId());

        Assertions.assertNotNull(filteredCurrencyResponse);
        Assertions.assertNotNull(filteredCurrencyResponse.getPayload());
        Assertions.assertFalse(filteredCurrencyResponse.getPayload().isEmpty());
        currency = filteredCurrencyResponse.getPayload().get(0);

        Assertions.assertNotNull(currency);
        Assertions.assertNotNull(currency.getCountry());
        Assertions.assertEquals("Earth2", updatedCurrency.getCountry());

        BulkDeleteCurrencyResponse bulkDeleteCurrencyResponse = currencyApi.bulkDeleteCurrency(Lists.newArrayList(currencyKey));

        Assertions.assertNotNull(bulkDeleteCurrencyResponse);
        Assertions.assertNotNull(bulkDeleteCurrencyResponse.getPayload());
        Assertions.assertFalse(bulkDeleteCurrencyResponse.getPayload().isEmpty());
        Assertions.assertNotNull(bulkDeleteCurrencyResponse.getPayload().get(0));

        CurrencyKey deletedCurrencyKey = bulkDeleteCurrencyResponse.getPayload().get(0);
        Assertions.assertEquals(currencyKey.getId(), deletedCurrencyKey.getId());

        FilteredCurrencyResponse someCurrency = currencyApi.queryCurrency(
                SpeedyQuery.builder()
                        .$where(
                                $condition("currencyAbbr", $eq("NZD"))
                        ).build()
        );

        Assertions.assertNotNull(someCurrency);
        Assertions.assertNotNull(someCurrency.getPayload());
        Assertions.assertFalse(someCurrency.getPayload().isEmpty());

        someCurrency.getPayload().forEach(currency1 -> {
            Assertions.assertNotNull(currency1);
            Assertions.assertNotNull(currency1.getCountry());
            Assertions.assertEquals("New Zealand", currency1.getCountry());
        });
    }

    void crudExchangeRates() throws JsonProcessingException {
        CurrencyApi currencyApi = new CurrencyApi(defaultClient);

        FilteredCurrencyResponse someCurrency = currencyApi.queryCurrency(
                SpeedyQuery.builder()
                        .$where(
                                $condition("currencyAbbr", $eq("NZD"))
                        ).build()
        );

        Assertions.assertNotNull(someCurrency);
        Assertions.assertNotNull(someCurrency.getPayload());
        Assertions.assertFalse(someCurrency.getPayload().isEmpty());
        Assertions.assertNotNull(someCurrency.getPayload().get(0));

        Currency baseCurrency = someCurrency.getPayload().get(0);

        someCurrency = currencyApi.queryCurrency(
                SpeedyQuery.builder()
                        .$where(
                                $condition("currencyAbbr", $eq("NZD"))
                        ).build()
        );
        Assertions.assertNotNull(someCurrency);
        Assertions.assertNotNull(someCurrency.getPayload());
        Assertions.assertFalse(someCurrency.getPayload().isEmpty());
        Assertions.assertNotNull(someCurrency.getPayload().get(0));

        Currency forgeinCurrency = someCurrency.getPayload().get(0);

        ExchangeRateApi exchangeRateApi = new ExchangeRateApi(defaultClient);
        CreateExchangeRateRequest createExchangeRateRequest = new CreateExchangeRateRequest();
        createExchangeRateRequest.baseCurrency(new CurrencyKey().id(baseCurrency.getId()))
                .foreignCurrency(new CurrencyKey().id(forgeinCurrency.getId()))
                .exchangeRate(1.0)
                .invExchangeRate(1.0);

        BulkCreateExchangeRateResponse bulkCreateExchangeRate200Response = exchangeRateApi.bulkCreateExchangeRate(Lists.newArrayList(createExchangeRateRequest));

        Assertions.assertNotNull(bulkCreateExchangeRate200Response);
        Assertions.assertNotNull(bulkCreateExchangeRate200Response.getPayload());
        Assertions.assertFalse(bulkCreateExchangeRate200Response.getPayload().isEmpty());
        Assertions.assertNotNull(bulkCreateExchangeRate200Response.getPayload().get(0));

        ExchangeRateKey exchangeRateKey = bulkCreateExchangeRate200Response.getPayload().get(0);

        FilteredExchangeRateResponse filteredExchangeRateResponse = exchangeRateApi.getExchangeRate(exchangeRateKey.getId());

        Assertions.assertNotNull(filteredExchangeRateResponse);
        Assertions.assertNotNull(filteredExchangeRateResponse.getPayload());
        Assertions.assertFalse(filteredExchangeRateResponse.getPayload().isEmpty());
        Assertions.assertNotNull(filteredExchangeRateResponse.getPayload().get(0));

        ExchangeRate exchangeRate = filteredExchangeRateResponse.getPayload().get(0);
        Assertions.assertNotNull(exchangeRate);
        Assertions.assertNotNull(exchangeRate.getBaseCurrency());
        Assertions.assertNotNull(exchangeRate.getForeignCurrency());
        Assertions.assertNotNull(exchangeRate.getExchangeRate());
        Assertions.assertNotNull(exchangeRate.getInvExchangeRate());
        Assertions.assertNotNull(exchangeRate.getId());

        Assertions.assertEquals(baseCurrency.getId(), exchangeRate.getBaseCurrency().getId());
        Assertions.assertEquals(forgeinCurrency.getId(), exchangeRate.getForeignCurrency().getId());
        Assertions.assertEquals(1.0, exchangeRate.getExchangeRate());
        Assertions.assertEquals(1.0, exchangeRate.getInvExchangeRate());

        LOGGER.info("exchangeRate {}", exchangeRate);


    }

    @Test
    void normal() throws Exception {


        Company company = crudCompany();
        Product product = crudProduct();
        Supplier supplier = crudSupplier();

        Procurement procurement = crudProcurement(product, supplier);

        Customer customer = crudCustomer();

        Invoice invoice = crudInvoice(customer);

        User user = crudUser(company);

        Inventory inventory = crudInventory(procurement, product, invoice);

        Assertions.assertNotNull(inventory);
        Assertions.assertNotNull(inventory.getId());
        Assertions.assertFalse(inventory.getId().isBlank());

        crudCurrency();

        crudExchangeRates();

    }


}


