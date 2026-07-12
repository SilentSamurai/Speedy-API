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
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String OWNER_SCOPED_CATEGORY_NAME = "owner-scoped-category";
    private static final String BULK_OWNER_CATEGORY_NAME = "bulk-owner-category";
    private static final String BULK_OTHER_CATEGORY_NAME = "bulk-other-category";

    @Autowired
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

    @Test
    void explicitFieldDenyOverridesWildcardAllowWithoutBlockingTheEntityGate() throws Exception {
        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Product")
                        .header(POLICY_HEADER, "deny-overrides-allow-product")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(7))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        for (JsonNode product : payload) {
            assertTrue(product.has("name"));
            assertFalse(product.has("description"));
        }
    }

    @Test
    void multipleAllowRowConditionsAcrossRulesCombineWithOr() throws Exception {
        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Product")
                        .header(POLICY_HEADER, "product-visibility-multi-rule-or")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(2))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        productWithDescription(payload, "Description 1");
        productWithDescription(payload, "Description 4");
    }

    @Test
    void orCombinatorWithinASingleConditionGrantsEitherBranch() throws Exception {
        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Category")
                        .header(POLICY_HEADER, "category-or-condition")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(2))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        rowWithFieldValue(payload, "name", "cat-5-5");
        rowWithFieldValue(payload, "name", "cat-6-6");
    }

    @Test
    void nonEqOperatorInIsHonoredByRowVisibilityFiltering() throws Exception {
        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Category")
                        .header(POLICY_HEADER, "category-id-in-set")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(3))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        rowWithFieldValue(payload, "name", "cat-1-1");
        rowWithFieldValue(payload, "name", "cat-2-2");
        rowWithFieldValue(payload, "name", "cat-3-3");
    }

    @Test
    void singlePolicyRuleGrantsReadAndUpdateIndependently() throws Exception {
        String scratchId = createScratchCategory(OWNER_SCOPED_CATEGORY_NAME);

        mvc.perform(get(SpeedyConstants.URI + "/Category")
                        .header(POLICY_HEADER, "read-update-own-category")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(1))
                .andExpect(jsonPath("$.payload[0].name").value(OWNER_SCOPED_CATEGORY_NAME));

        mvc.perform(patch(operationUrl("Category", SpeedyEndpoint.UPDATE))
                .header(POLICY_HEADER, "read-update-own-category")
                .content("{\"id\":\"" + scratchId + "\",\"name\":\"" + OWNER_SCOPED_CATEGORY_NAME + "-renamed\"}")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].name").value(OWNER_SCOPED_CATEGORY_NAME + "-renamed"));
    }

    @Test
    void bulkCreateFailsTheEntireRequestWhenAnyItemViolatesFieldPolicy() throws Exception {
        mvc.perform(post(operationUrl("Category", SpeedyEndpoint.CREATE))
                        .header(POLICY_HEADER, "bulk-create-allowed-names")
                        .content("[{\"name\":\"bulk-ok-1\"},{\"name\":\"bulk-blocked\"},{\"name\":\"bulk-ok-2\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Field 'name' not permitted on create"));
    }

    @Test
    void bulkUpdatePerEntityPartialFailureReturns207() throws Exception {
        String ownedId = createScratchCategory(BULK_OWNER_CATEGORY_NAME);
        String otherId = createScratchCategory(BULK_OTHER_CATEGORY_NAME);

        mvc.perform(patch(operationUrl("Category", SpeedyEndpoint.UPDATE))
                        .header(POLICY_HEADER, "bulk-update-own-categories")
                        .content("[{\"id\":\"" + ownedId + "\",\"name\":\"" + BULK_OWNER_CATEGORY_NAME + "-renamed\"},"
                                + "{\"id\":\"" + otherId + "\",\"name\":\"whatever\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(207))
                .andExpect(jsonPath("$.succeeded.length()").value(1))
                .andExpect(jsonPath("$.failed.length()").value(1))
                .andExpect(jsonPath("$.failed[0].index").value(1))
                .andExpect(jsonPath("$.failed[0].status").value(403))
                .andExpect(jsonPath("$.failed[0].message").value("Field 'name' not permitted on update"));
    }

    @Test
    void entityLevelGateBlocksCreateAndUpdateWhenNoRuleGrantsTheAction() throws Exception {
        mvc.perform(post(operationUrl("Category", SpeedyEndpoint.CREATE))
                        .header(POLICY_HEADER, "no-create-or-update-access-category")
                        .content("[{\"name\":\"should-not-be-created\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("create not allowed for Category"));

        mvc.perform(patch(operationUrl("Category", SpeedyEndpoint.UPDATE))
                        .header(POLICY_HEADER, "no-create-or-update-access-category")
                        .content("{\"id\":\"nonexistent-id-for-gate-test\",\"name\":\"x\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("update not allowed for Category"));
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

    private static JsonNode rowWithFieldValue(JsonNode payload, String field, String value) {
        for (JsonNode row : payload) {
            if (value.equals(row.path(field).asText())) {
                return row;
            }
        }
        throw new AssertionError("Row with " + field + "='" + value + "' was not returned");
    }

    /// Creates a throwaway Category via an unconditionally-permissive scenario, so tests that
    /// need a row scoped to a specific principal don't have to mutate seed data (which other
    /// tests in this class depend on staying in its original shape).
    private String createScratchCategory(String name) throws Exception {
        MvcResult result = mvc.perform(post(operationUrl("Category", SpeedyEndpoint.CREATE))
                        .header(POLICY_HEADER, "manage-scratch-category")
                        .content("[{\"name\":\"" + name + "\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return responseBody(result).path("payload").get(0).path("id").asText();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PolicyTestConfiguration {

        @Bean
        @Primary
        ISpeedyConfiguration policyAwareConfiguration(SpeedyConfig delegate) {
            return new PolicyAwareConfiguration(delegate);
        }
    }

    private record PolicyAwareConfiguration(SpeedyConfig delegate) implements ISpeedyConfiguration {

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
            case "deny-overrides-allow-product" -> Optional.of(policy("n/a",
                    rule("allow-all-product-fields", PermissionType.READ, "Product.*"),
                    denyRule("deny-product-description", PermissionType.READ, "Product.description")));
            case "product-visibility-multi-rule-or" -> Optional.of(policy("n/a",
                    rule("read-description-1", PermissionType.READ, "Product.*", fieldEquals("description", "Description 1")),
                    rule("read-description-4", PermissionType.READ, "Product.*", fieldEquals("description", "Description 4"))));
            case "category-or-condition" -> Optional.of(policy("n/a",
                    rule("read-two-categories", PermissionType.READ, "Category.*", orOf("name", "cat-5-5", "cat-6-6"))));
            case "category-id-in-set" -> Optional.of(policy("n/a",
                    rule("read-selected-categories", PermissionType.READ, "Category.*", fieldIn("id", "1", "2", "3"))));
            case "manage-scratch-category" -> Optional.of(policy("n/a",
                    rule("scratch-create", PermissionType.CREATE, "Category.name")));
            case "read-update-own-category" -> Optional.of(policy(OWNER_SCOPED_CATEGORY_NAME,
                    rule("read-update-own", Set.of(PermissionType.READ, PermissionType.UPDATE), "Category.name", namedByPrincipal())));
            case "bulk-create-allowed-names" -> Optional.of(policy("n/a",
                    rule("create-allowed-names", PermissionType.CREATE, "Category.name", fieldIn("name", "bulk-ok-1", "bulk-ok-2"))));
            case "bulk-update-own-categories" -> Optional.of(policy(BULK_OWNER_CATEGORY_NAME,
                    rule("bulk-update-own", PermissionType.UPDATE, "Category.name", namedByPrincipal())));
            case "no-create-or-update-access-category" -> Optional.of(policy("n/a",
                    rule("read-only-categories", PermissionType.READ, "Category.*")));
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
                List.of(resource), List.copyOf(conditions));
    }

    private static SpeedyPolicy rule(String name, Set<PermissionType> actions, String resource,
                                   QueryCondition condition) {
        return new SpeedyPolicy(name, PolicyEffect.ALLOW, actions, List.of(resource), List.of(condition));
    }

    private static SpeedyPolicy denyRule(String name, PermissionType action, String resource) {
        return new SpeedyPolicy(name, PolicyEffect.DENY, Set.of(action),
                List.of(resource), List.of());
    }

    private static QueryCondition ownedByPrincipal() {
        return new QueryCondition(Map.of("description", "${principal.id}"));
    }

    private static QueryCondition namedByPrincipal() {
        return new QueryCondition(Map.of("name", "${principal.id}"));
    }

    private static QueryCondition fieldEquals(String field, String value) {
        return new QueryCondition(Map.of(field, value));
    }

    private static QueryCondition fieldIn(String field, String... values) {
        return new QueryCondition(Map.of(field, Map.of("$in", List.of(values))));
    }

    private static QueryCondition orOf(String field, String first, String second) {
        return new QueryCondition(Map.of("$or", List.of(Map.of(field, first), Map.of(field, second))));
    }
}
