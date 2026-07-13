package com.github.silent.samurai.speedy.policy;

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

import static com.github.silent.samurai.speedy.policy.PolicyBuilder.denyByDefault;
import static com.github.silent.samurai.speedy.policy.PolicyConditions.fieldEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Exercises a lifecycle-state ABAC policy on {@link
/// com.github.silent.samurai.speedy.entity.Article}: UPDATE and DELETE are both gated on the
/// *current persisted* {@code status} being {@code DRAFT}. Because the row-condition check reads
/// the row as it exists before the write (see {@code AbstractUpdateHandler#enforceUpdateFieldPolicy}
/// and {@code DeleteHandler#enforceDeleteRowPolicy}), a single condition naturally yields a
/// one-way workflow: a draft can be edited (including being published) or deleted, but once
/// published, nothing about it — not even the status field itself — can be changed back.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PolicyTestConfiguration.class)
class DraftPublishWorkflowPolicyTest {

    @Autowired
    private MockMvc mvc;

    private static String operationUrl(String entity, SpeedyEndpoint endpoint) {
        return SpeedyConstants.URI + "/" + entity + "/" + endpoint.suffix();
    }

    private static com.fasterxml.jackson.databind.JsonNode responseBody(MvcResult result) throws Exception {
        return CommonUtil.json().readTree(result.getResponse().getContentAsString());
    }

    private static RequestPostProcessor withPolicy(SpeedyAuthContext authContext) {
        return request -> {
            request.setAttribute(PolicyTestConfiguration.AUTH_CONTEXT_ATTRIBUTE, authContext);
            return request;
        };
    }

    /// The policy under test in every case below: read is unconditional (required for the write
    /// conditions' `status` field to pass the write-condition oracle guard), while UPDATE and
    /// DELETE are each allowed only when the row's current status is DRAFT.
    private static SpeedyAuthContext draftOnlyWorkflowPolicy() {
        return denyByDefault()
                .allow("read-articles", PermissionType.READ, "Article.*")
                .allow("edit-draft-articles", PermissionType.UPDATE, "Article.*",
                        fieldEquals("status", "DRAFT"))
                .allow("delete-draft-articles", PermissionType.DELETE, "Article.*",
                        fieldEquals("status", "DRAFT"))
                .build();
    }

    @Test
    void draftArticleCanBeDeleted() throws Exception {
        String articleId = createScratchArticle("Draft Article", "DRAFT");

        mvc.perform(delete(operationUrl("Article", SpeedyEndpoint.DELETE))
                        .with(withPolicy(draftOnlyWorkflowPolicy()))
                        .content("[{\"id\":\"" + articleId + "\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void publishedArticleCannotBeDeleted() throws Exception {
        String articleId = createScratchArticle("Published Article", "PUBLISHED");

        mvc.perform(delete(operationUrl("Article", SpeedyEndpoint.DELETE))
                        .with(withPolicy(draftOnlyWorkflowPolicy()))
                        .content("[{\"id\":\"" + articleId + "\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("delete not allowed for Article"));
    }

    @Test
    void conditionalDeletePolicyRunsBeforeStaleIfMatchCheck() throws Exception {
        String noteId = createScratchNote("restricted note");
        SpeedyAuthContext authContext = denyByDefault()
                .allow("read-notes", PermissionType.READ, "Note.*")
                .allow("delete-unrestricted-notes", PermissionType.DELETE, "Note.*",
                        fieldEquals("title", "deletable"))
                .build();

        mvc.perform(delete(operationUrl("Note", SpeedyEndpoint.DELETE))
                        .with(withPolicy(authContext))
                        .header("If-Match", "W/\"stale-token\"")
                        .content("[{\"id\":\"" + noteId + "\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("delete not allowed for Note"));
    }

    /// Editing a draft is allowed, and that includes moving it forward into PUBLISHED — the
    /// condition is checked against the row's state *before* this write is applied, so a still-
    /// draft row satisfies it even though the submitted value moves it out of DRAFT.
    @Test
    void draftArticleCanBeEditedAndPublished() throws Exception {
        String articleId = createScratchArticle("Draft Article", "DRAFT");

        mvc.perform(patch(operationUrl("Article", SpeedyEndpoint.UPDATE))
                        .with(withPolicy(draftOnlyWorkflowPolicy()))
                        .content("{\"id\":\"" + articleId + "\",\"title\":\"Edited Draft\",\"status\":\"PUBLISHED\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].title").value("Edited Draft"))
                .andExpect(jsonPath("$.payload[0].status").value("PUBLISHED"));
    }

    /// Once published, the row no longer satisfies the DRAFT condition, so every UPDATE is
    /// rejected by the document's default deny — including an attempt to revert status back to
    /// DRAFT. Publishing is therefore one-way under this policy.
    @Test
    void publishedArticleCannotBeEditedOrRevertedToDraft() throws Exception {
        String articleId = createScratchArticle("Published Article", "PUBLISHED");

        mvc.perform(patch(operationUrl("Article", SpeedyEndpoint.UPDATE))
                        .with(withPolicy(draftOnlyWorkflowPolicy()))
                        .content("{\"id\":\"" + articleId + "\",\"status\":\"DRAFT\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Field 'status' not permitted on update"));
    }

    /// Creates a throwaway Article, already at the requested status, using an unrestricted
    /// creation policy — so each test starts from a known lifecycle state without depending on
    /// the DRAFT-only workflow policy under test.
    private String createScratchArticle(String title, String status) throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("scratch-create", PermissionType.CREATE, "Article.*")
                .build();

        MvcResult result = mvc.perform(post(operationUrl("Article", SpeedyEndpoint.CREATE))
                        .with(withPolicy(authContext))
                        .content("[{\"title\":\"" + title + "\",\"status\":\"" + status + "\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return responseBody(result).path("payload").get(0).path("id").asText();
    }

    private String createScratchNote(String title) throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("scratch-create-note", PermissionType.CREATE, "Note.*")
                .build();

        MvcResult result = mvc.perform(post(operationUrl("Note", SpeedyEndpoint.CREATE))
                        .with(withPolicy(authContext))
                        .content("[{\"title\":\"" + title + "\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return responseBody(result).path("payload").get(0).path("id").asText();
    }

}
