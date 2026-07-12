package com.github.silent.samurai.speedy.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.config.SpeedyConfig;
import com.github.silent.samurai.speedy.conversion.ext.SpeedyTypeModule;
import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.ISpeedyRegistry;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.interfaces.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModelProcessor;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.policy.condition.QueryCondition;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.ResourceSelector;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PolicyIntegrationTest.PolicyTestConfiguration.class)
class PolicyIntegrationTest {

    private static final String POLICY_HEADER = "X-Speedy-Test-Policy";

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mvc;

    @Test
    void rowPolicyFiltersResultsBeforePagingAndCounting() throws Exception {
        MvcResult result = mvc.perform(post(queryUrl("Product"))
                        .header(POLICY_HEADER, "owned-products")
                        .content("""
                                {"$from":"Product","$page":{"$index":0,"$size":10}}
                                """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload.length()").value(1))
                .andExpect(jsonPath("$.payload[0].description").value("Description 1"))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        assertEquals("Product 1", payload.get(0).path("name").asText());
    }

    @Test
    void fieldPolicyOmitsConditionalFieldsWithoutDroppingUnconditionallyReadableRows() throws Exception {
        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Product")
                        .header(POLICY_HEADER, "mixed-product-visibility")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(7))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        JsonNode owned = productWithDescription(payload, "Description 1");
        JsonNode other = productWithDescription(payload, "Description 2");

        assertTrue(owned.has("name"));
        assertTrue(owned.has("description"));
        assertFalse(other.has("name"));
        assertTrue(other.has("description"));
    }

    @Test
    void conditionalReadFieldsCannotBeUsedForFiltering() throws Exception {
        mvc.perform(get(SpeedyConstants.URI + "/Product?name='Product 1'")
                        .header(POLICY_HEADER, "owned-products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Field 'name' cannot be used in a filter or sort"));
    }

    @Test
    void associatedFieldsAreCheckedAgainstTheirOwnEntityPolicy() throws Exception {
        mvc.perform(get(SpeedyConstants.URI + "/Product?category.name='cat-1-1'")
                        .header(POLICY_HEADER, "product-name-only")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Field 'name' cannot be used in a filter or sort"));
    }

    @Test
    void unconditionalWholeEntityReadDenyIsRejectedBeforeAnyRowsOrCountsAreReturned() throws Exception {
        mvc.perform(get(SpeedyConstants.URI + "/Product")
                        .header(POLICY_HEADER, "deny-products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("read not allowed for Product"));
    }

    @Test
    void createRejectsAFieldThatDoesNotSatisfyItsPolicy() throws Exception {
        mvc.perform(post(operationUrl("Category", SpeedyEndpoint.CREATE))
                        .header(POLICY_HEADER, "create-category")
                        .content("[{\"name\":\"blocked-category\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Field 'name' not permitted on create"));
    }

    @Test
    void updateAndDeleteRejectRowsOutsideTheCallersScope() throws Exception {
        mvc.perform(patch(operationUrl("Category", SpeedyEndpoint.UPDATE))
                        .header(POLICY_HEADER, "own-category-writes")
                        .content("{\"id\":\"2\",\"name\":\"not-allowed\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Field 'name' not permitted on update"));

        mvc.perform(delete(operationUrl("Category", SpeedyEndpoint.DELETE))
                        .header(POLICY_HEADER, "own-category-writes")
                        .content("[{\"id\":\"2\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("delete not allowed for Category"));
    }

    private static String queryUrl(String entity) {
        return operationUrl(entity, SpeedyEndpoint.QUERY);
    }

    private static String operationUrl(String entity, SpeedyEndpoint endpoint) {
        return SpeedyConstants.URI + "/" + entity + "/" + endpoint.suffix();
    }

    private static JsonNode responseBody(MvcResult result) throws Exception {
        return CommonUtil.json().readTree(result.getResponse().getContentAsString());
    }

    private static JsonNode productWithDescription(JsonNode payload, String description) {
        for (JsonNode product : payload) {
            if (description.equals(product.path("description").asText())) {
                return product;
            }
        }
        throw new AssertionError("Product with description '" + description + "' was not returned");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PolicyTestConfiguration {

        @Bean
        @Primary
        ISpeedyConfiguration policyAwareConfiguration(SpeedyConfig delegate) {
            return new PolicyAwareConfiguration(delegate);
        }
    }

    private static final class PolicyAwareConfiguration implements ISpeedyConfiguration {

        private final SpeedyConfig delegate;

        private PolicyAwareConfiguration(SpeedyConfig delegate) {
            this.delegate = delegate;
        }

        @Override
        public MetaModelProcessor metaModelProcessor() {
            return delegate.metaModelProcessor();
        }

        @Override
        public void register(ISpeedyRegistry registry) {
            delegate.register(registry);
        }

        @Override
        public DataSource dataSourcePerReq() {
            return delegate.dataSourcePerReq();
        }

        @Override
        public SpeedyDialect getDialect() {
            return delegate.getDialect();
        }

        @Override
        public SpeedyBackend queryBackend(DataSource dataSource, SpeedyDialect dialect) {
            return delegate.queryBackend(dataSource, dialect);
        }

        @Override
        public List<SpeedyTypeModule> typeModules() {
            return delegate.typeModules();
        }

        @Override
        public Optional<SpeedyAuthContext> authContextPerReq() {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                return Optional.empty();
            }
            return policyFor(attributes.getRequest().getHeader(POLICY_HEADER));
        }
    }

    private static Optional<SpeedyAuthContext> policyFor(String scenario) {
        if (scenario == null) {
            return Optional.empty();
        }
        return switch (scenario) {
            case "owned-products" -> Optional.of(policy("Description 1",
                    rule("read-own-products", PermissionType.READ, "Product.*", ownedByPrincipal())));
            case "mixed-product-visibility" -> Optional.of(policy("Description 1",
                    rule("read-descriptions", PermissionType.READ, "Product.description"),
                    rule("read-own-names", PermissionType.READ, "Product.name", ownedByPrincipal())));
            case "product-name-only" -> Optional.of(policy("Description 1",
                    rule("read-product-names", PermissionType.READ, "Product.name")));
            case "deny-products" -> Optional.of(policy("Description 1",
                    denyRule("deny-products", PermissionType.READ, "Product.*")));
            case "create-category" -> Optional.of(policy("allowed-category",
                    rule("create-own-name", PermissionType.CREATE, "Category.name", namedByPrincipal())));
            case "own-category-writes" -> Optional.of(policy("cat-1-1",
                    rule("identify-categories", PermissionType.UPDATE, "Category.id"),
                    rule("change-own-categories", PermissionType.UPDATE, "Category.name", namedByPrincipal()),
                    rule("delete-own-categories", PermissionType.DELETE, "Category.*", namedByPrincipal())));
            default -> Optional.empty();
        };
    }

    private static SpeedyAuthContext policy(String principalId, SpeedyPolicy... rules) {
        return new SpeedyAuthContext(new PolicyDocument(PolicyEffect.DENY, List.of(rules)),
                Map.of("principal.id", new SpeedyText(principalId)));
    }

    private static SpeedyPolicy rule(String name, PermissionType action, String resource) {
        return rule(name, action, resource, List.of());
    }

    private static SpeedyPolicy rule(String name, PermissionType action, String resource,
                                   QueryCondition condition) {
        return rule(name, action, resource, List.of(condition));
    }

    private static SpeedyPolicy rule(String name, PermissionType action, String resource,
                                   List<QueryCondition> conditions) {
        return new SpeedyPolicy(name, PolicyEffect.ALLOW, Set.of(action),
                List.of(ResourceSelector.parse(resource)), List.copyOf(conditions));
    }

    private static SpeedyPolicy denyRule(String name, PermissionType action, String resource) {
        return new SpeedyPolicy(name, PolicyEffect.DENY, Set.of(action),
                List.of(ResourceSelector.parse(resource)), List.of());
    }

    private static QueryCondition ownedByPrincipal() {
        return new QueryCondition(Map.of("description", "${principal.id}"));
    }

    private static QueryCondition namedByPrincipal() {
        return new QueryCondition(Map.of("name", "${principal.id}"));
    }
}
