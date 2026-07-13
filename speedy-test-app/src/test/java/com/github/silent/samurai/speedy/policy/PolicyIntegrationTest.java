package com.github.silent.samurai.speedy.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.github.silent.samurai.speedy.policy.condition.QueryCondition;

import java.util.List;
import java.util.Map;

import static com.github.silent.samurai.speedy.policy.PolicyBuilder.denyByDefault;
import static com.github.silent.samurai.speedy.policy.PolicyConditions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PolicyTestConfiguration.class)
class PolicyIntegrationTest {

    private static final String OWNER_SCOPED_CATEGORY_NAME = "owner-scoped-category";
    private static final String BULK_OWNER_CATEGORY_NAME = "bulk-owner-category";
    private static final String BULK_OTHER_CATEGORY_NAME = "bulk-other-category";

    @Autowired
    private MockMvc mvc;

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

    private static RequestPostProcessor withPolicy(SpeedyAuthContext authContext) {
        return request -> {
            request.setAttribute(PolicyTestConfiguration.AUTH_CONTEXT_ATTRIBUTE, authContext);
            return request;
        };
    }

    /// A row-scoped read rule is applied before pagination and counting, so every response total
    /// describes only the products visible to the current principal.
    @Test
    void rowPolicyFiltersResultsBeforePagingAndCounting() throws Exception {
        String principalId = "Description 1";
        SpeedyAuthContext authContext = denyByDefault()
                .principalId(principalId)
                .allow("read-own-products", PermissionType.READ, "Product.*",
                        fieldEquals("description", variable("principal.id")))
                .build();

        MvcResult result = mvc.perform(post(queryUrl("Product"))
                        .with(withPolicy(authContext))
                        .content("""
                                {"$from":"Product","$page":{"$index":0,"$size":10}}
                                """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
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

    /// A row condition (whole-entity selector) selects a small subset of rows the caller can see
    /// at all; within that subset, an unconditional field deny is flat — it doesn't depend on the
    /// row's own data, it's just gone. Row selection can still filter on a field's raw column
    /// value even when that same field is denied from the response.
    @Test
    void rowConditionSelectsSubsetAndFieldDenyIsUnconditionalWithinIt() throws Exception {
        String principalId = "Description 1";
        SpeedyAuthContext authContext = denyByDefault()
                .principalId(principalId)
                .allow("read-own-products", PermissionType.READ, "Product.*",
                        fieldEquals("description", variable("principal.id")))
                .deny("hide-description", PermissionType.READ, "Product.description")
                .build();

        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Product")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(1))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        assertEquals("Product 1", payload.get(0).path("name").asText());
        assertFalse(payload.get(0).has("description"));
    }

    /// A conditionally readable field may be returned for eligible rows but cannot be used as a
    /// filter, because doing so could reveal data from ineligible rows.
    @Test
    void conditionalReadFieldsCannotBeUsedForFiltering() throws Exception {
        String principalId = "Description 1";
        SpeedyAuthContext authContext = denyByDefault()
                .principalId(principalId)
                .allow("read-own-products", PermissionType.READ, "Product.*",
                        fieldEquals("description", variable("principal.id")))
                .build();

        mvc.perform(get(SpeedyConstants.URI + "/Product?name='Product 1'")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Field 'name' cannot be used in a filter or sort"));
    }

    /// A filter that follows an association is authorized against the associated entity's field
    /// policy, not the policy on the entity named in the request URL.
    @Test
    void associatedFieldsAreCheckedAgainstTheirOwnEntityPolicy() throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("read-product-names", PermissionType.READ, "Product.name")
                .build();

        mvc.perform(get(SpeedyConstants.URI + "/Product?category.name='cat-1-1'")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Field 'name' cannot be used in a filter or sort"));
    }

    /// An unconditional entity-wide deny rejects the read before it can return either rows or
    /// pagination totals.
    @Test
    void unconditionalWholeEntityReadDenyIsRejectedBeforeAnyRowsOrCountsAreReturned() throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .deny("deny-products", PermissionType.READ, "Product.*")
                .build();

        mvc.perform(get(SpeedyConstants.URI + "/Product")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("read not allowed for Product"));
    }

    /// Create authorization evaluates the submitted field values; a value outside the caller's
    /// policy scope is rejected before any entity is created.
    @Test
    void createRejectsAFieldThatDoesNotSatisfyItsPolicy() throws Exception {
        String principalId = "allowed-category";
        SpeedyAuthContext authContext = denyByDefault()
                .principalId(principalId)
                .allow("create-own-name", PermissionType.CREATE, "Category.name",
                        fieldEquals("name", variable("principal.id")))
                .build();

        mvc.perform(post(operationUrl("Category", SpeedyEndpoint.CREATE))
                        .with(withPolicy(authContext))
                        .content("[{\"name\":\"blocked-category\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Field 'name' not permitted on create"));
    }

    /// Update and delete rules evaluate the persisted target row, preventing a caller from
    /// changing or deleting categories outside their name-based ownership scope.
    @Test
    void updateAndDeleteRejectRowsOutsideTheCallersScope() throws Exception {
        String principalId = "cat-1-1";
        SpeedyAuthContext authContext = denyByDefault()
                .principalId(principalId)
                // The write conditions below gate on `name`, so `name` must be plainly readable
                // (see WriteConditionPolicyHandler); grant an unconditional read of it.
                .allow("read-category-name", PermissionType.READ, "Category.name")
                .allow("identify-categories", PermissionType.UPDATE, "Category.id")
                .allow("change-own-categories", PermissionType.UPDATE, "Category.name",
                        fieldEquals("name", variable("principal.id")))
                .allow("delete-own-categories", PermissionType.DELETE, "Category.*",
                        fieldEquals("name", variable("principal.id")))
                .build();

        mvc.perform(patch(operationUrl("Category", SpeedyEndpoint.UPDATE))
                        .with(withPolicy(authContext))
                        .content("{\"id\":\"2\",\"name\":\"not-allowed\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Field 'name' not permitted on update"));

        mvc.perform(delete(operationUrl("Category", SpeedyEndpoint.DELETE))
                        .with(withPolicy(authContext))
                        .content("[{\"id\":\"2\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("delete not allowed for Category"));
    }

    @Test
    void replaceRequiresReplacePermissionRatherThanUpdatePermission() throws Exception {
        String categoryId = createScratchCategory("replace-policy-category");
        String url = operationUrl("Category", SpeedyEndpoint.UPDATE);
        String body = "{\"id\":\"" + categoryId + "\",\"name\":\"replace-policy-category-renamed\"}";
        SpeedyAuthContext updateOnly = denyByDefault()
                .allow("update-category-name", PermissionType.UPDATE, "Category.name")
                .build();

        mvc.perform(put(url)
                        .with(withPolicy(updateOnly))
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("replace not allowed for Category"));

        SpeedyAuthContext replaceOnly = denyByDefault()
                .allow("replace-category-name", PermissionType.REPLACE, "Category.name")
                .build();

        mvc.perform(put(url)
                        .with(withPolicy(replaceOnly))
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].name").value("replace-policy-category-renamed"));
    }

    /// The write-condition oracle guard: an UPDATE rule that gates on a field the caller cannot
    /// read is a misconfiguration — permitting it would let per-row write success/failure leak that
    /// field's values — so the request is rejected up front, before any row is examined. The
    /// decision depends only on the policy and the caller, never on a row, so it is constant per
    /// caller and cannot itself become an oracle.
    @Test
    void updateGatedOnUnreadableConditionFieldIsRejectedAsMisconfiguration() throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("update-gated-on-unreadable-name", PermissionType.UPDATE, "Category.name",
                        fieldEquals("name", "cat-1-1"))
                .build();

        mvc.perform(patch(operationUrl("Category", SpeedyEndpoint.UPDATE))
                        .with(withPolicy(authContext))
                        .content("{\"id\":\"2\",\"name\":\"cat-1-1\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Field 'name' is used in a policy update condition but is not readable"));
    }

    /// A field-level deny overrides a wildcard allow for that field without rejecting reads of
    /// the rest of the entity.
    @Test
    void explicitFieldDenyOverridesWildcardAllowWithoutBlockingTheEntityGate() throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("allow-all-product-fields", PermissionType.READ, "Product.*")
                .deny("deny-product-description", PermissionType.READ, "Product.description")
                .build();

        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Product")
                        .with(withPolicy(authContext))
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

    /// Separate conditional allow rules grant visibility independently, so their row conditions
    /// combine with logical OR.
    @Test
    void multipleAllowRowConditionsAcrossRulesCombineWithOr() throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("read-description-1", PermissionType.READ, "Product.*",
                        fieldEquals("description", "Description 1"))
                .allow("read-description-4", PermissionType.READ, "Product.*",
                        fieldEquals("description", "Description 4"))
                .build();

        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Product")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(2))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        productWithDescription(payload, "Description 1");
        productWithDescription(payload, "Description 4");
    }

    /// A single policy condition may contain an explicit {@code $or} expression to grant either
    /// branch of the row predicate.
    @Test
    void orCombinatorWithinASingleConditionGrantsEitherBranch() throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("read-two-categories", PermissionType.READ, "Category.*",
                        new QueryCondition(Map.of("$or",
                                List.of(Map.of("name", "cat-5-5"), Map.of("name", "cat-6-6")))))
                .build();

        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Category")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(2))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        rowWithFieldValue(payload, "name", "cat-5-5");
        rowWithFieldValue(payload, "name", "cat-6-6");
    }

    /// Row visibility conditions support non-equality operators such as {@code $in}.
    @Test
    void nonEqOperatorInIsHonoredByRowVisibilityFiltering() throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("read-selected-categories", PermissionType.READ, "Category.*",
                        new QueryCondition(Map.of("id", Map.of("$in", List.of("1", "2", "3")))))
                .build();

        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Category")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(3))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        rowWithFieldValue(payload, "name", "cat-1-1");
        rowWithFieldValue(payload, "name", "cat-2-2");
        rowWithFieldValue(payload, "name", "cat-3-3");
    }

    /// Row-level read scoping and field-level update scoping are independent mechanisms that can
    /// still express the same "own records only" restriction together: a whole-entity condition
    /// grants read of the caller's own category (and every one of its fields), while a separate
    /// field-specific condition grants update of that same category's name.
    @Test
    void rowConditionGrantsReadWhileFieldConditionGrantsUpdateIndependently() throws Exception {
        String principalId = OWNER_SCOPED_CATEGORY_NAME;
        SpeedyAuthContext authContext = denyByDefault()
                .principalId(principalId)
                .allow("read-own-category", PermissionType.READ, "Category.*",
                        fieldEquals("name", variable("principal.id")))
                // The UPDATE condition gates on `name`, so it must be plainly readable too.
                .allow("read-category-name", PermissionType.READ, "Category.name")
                .allow("update-own-name", PermissionType.UPDATE, "Category.name",
                        fieldEquals("name", variable("principal.id")))
                .build();
        String scratchId = createScratchCategory(OWNER_SCOPED_CATEGORY_NAME);

        mvc.perform(get(SpeedyConstants.URI + "/Category")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(1))
                .andExpect(jsonPath("$.payload[0].name").value(OWNER_SCOPED_CATEGORY_NAME));

        mvc.perform(patch(operationUrl("Category", SpeedyEndpoint.UPDATE))
                        .with(withPolicy(authContext))
                        .content("{\"id\":\"" + scratchId + "\",\"name\":\"" + OWNER_SCOPED_CATEGORY_NAME + "-renamed\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].name").value(OWNER_SCOPED_CATEGORY_NAME + "-renamed"));
    }

    /// Create is all-or-nothing: if any submitted item violates the field condition, no item in
    /// the bulk request is created.
    @Test
    void bulkCreateFailsTheEntireRequestWhenAnyItemViolatesFieldPolicy() throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("create-allowed-names", PermissionType.CREATE, "Category.name",
                        new QueryCondition(Map.of("name", Map.of("$in", List.of("bulk-ok-1", "bulk-ok-2")))))
                .build();

        mvc.perform(post(operationUrl("Category", SpeedyEndpoint.CREATE))
                        .with(withPolicy(authContext))
                        .content("[{\"name\":\"bulk-ok-1\"},{\"name\":\"bulk-blocked\"},{\"name\":\"bulk-ok-2\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Field 'name' not permitted on create"));
    }

    /// Per-entity bulk update returns {@code 207 Multi-Status} when the request contains both an
    /// authorized target row and a row outside the caller's policy scope.
    @Test
    void bulkUpdatePerEntityPartialFailureReturns207() throws Exception {
        String principalId = BULK_OWNER_CATEGORY_NAME;
        SpeedyAuthContext authContext = denyByDefault()
                .principalId(principalId)
                // The UPDATE condition gates on `name`, so it must be plainly readable too.
                .allow("read-category-name", PermissionType.READ, "Category.name")
                .allow("bulk-update-own", PermissionType.UPDATE, "Category.name",
                        fieldEquals("name", variable("principal.id")))
                .build();
        String ownedId = createScratchCategory(BULK_OWNER_CATEGORY_NAME);
        String otherId = createScratchCategory(BULK_OTHER_CATEGORY_NAME);

        mvc.perform(patch(operationUrl("Category", SpeedyEndpoint.UPDATE))
                        .with(withPolicy(authContext))
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

    /// Field-level rules are insufficient by themselves: create and update also require a rule
    /// that grants the corresponding entity-level action.
    @Test
    void entityLevelGateBlocksCreateAndUpdateWhenNoRuleGrantsTheAction() throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("read-only-categories", PermissionType.READ, "Category.*")
                .build();

        mvc.perform(post(operationUrl("Category", SpeedyEndpoint.CREATE))
                        .with(withPolicy(authContext))
                        .content("[{\"name\":\"should-not-be-created\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("create not allowed for Category"));

        mvc.perform(patch(operationUrl("Category", SpeedyEndpoint.UPDATE))
                        .with(withPolicy(authContext))
                        .content("{\"id\":\"nonexistent-id-for-gate-test\",\"name\":\"x\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("update not allowed for Category"));
    }

    /// Creates a throwaway Category with an explicit creation policy, so tests that
    /// need a row scoped to a specific principal don't have to mutate seed data (which other
    /// tests in this class depend on staying in its original shape).
    private String createScratchCategory(String name) throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("scratch-create", PermissionType.CREATE, "Category.name")
                .build();

        MvcResult result = mvc.perform(post(operationUrl("Category", SpeedyEndpoint.CREATE))
                        .with(withPolicy(authContext))
                        .content("[{\"name\":\"" + name + "\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return responseBody(result).path("payload").get(0).path("id").asText();
    }

}
