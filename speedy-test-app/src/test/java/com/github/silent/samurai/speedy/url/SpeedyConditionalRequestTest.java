package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.repositories.NoteRepository;
import com.github.silent.samurai.speedy.repositories.PriceHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

/// End-to-end coverage for ETag / conditional-request support ([issue #98](https://github.com/SilentSamurai/Speedy-API/issues/98)):
/// `ETag` emission + `If-None-Match` -> 304 on GET, and `If-Match` -> 412/400 with a fresh `ETag`
/// returned on PATCH/PUT/DELETE. {@link com.github.silent.samurai.speedy.entity.Note} (default
/// RANDOM strategy, single key) and {@link com.github.silent.samurai.speedy.entity.PriceHistory}
/// (TIMESTAMP strategy, composite FK+temporal key) are the opted-in fixtures;
/// {@link com.github.silent.samurai.speedy.entity.Category} (no {@code @SpeedyETag} field) is the
/// negative case proving zero impact on entities that don't opt in.
///
/// Driven entirely through the {@link SpeedyTest} client facade: conditional headers are set via
/// {@code .header("If-Match", ...)} / {@code .header("If-None-Match", ...)} on the request builders,
/// and the response {@code ETag} is read back via {@link SpeedyTestResult#etag()}.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyConditionalRequestTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    private SpeedyTest speedy;

    @BeforeEach
    void setUp() {
        speedy = SpeedyTest.mockMvc(mvc);
    }

    @AfterEach
    void cleanup() {
        noteRepository.deleteAll();
        priceHistoryRepository.deleteAll();
    }

    /* ---------------------------------------------------------------- */
    /* Helpers                                                           */
    /* ---------------------------------------------------------------- */

    private SpeedyTestResult createNote(String title) {
        return speedy.create("Note").field("title", title).execute().expectOk();
    }

    private String idOf(SpeedyTestResult result) {
        return result.jsonPath("$.payload[0].id");
    }

    private SpeedyTestResult getNoteById(String id) {
        return speedy.get("Note").key("id", id).execute();
    }

    private SpeedyTestResult getNoteById(String id, String ifNoneMatch) {
        return speedy.get("Note").key("id", id).header("If-None-Match", ifNoneMatch).execute();
    }

    private SpeedyTestResult patchNoteTitle(String id, String newTitle, String ifMatch) {
        return speedy.update("Note").key("id", id).field("title", newTitle)
                .header("If-Match", ifMatch).execute();
    }

    private SpeedyTestResult deleteNote(String id, String ifMatch) {
        return speedy.delete("Note").key("id", id).header("If-Match", ifMatch).execute();
    }

    private String createCategory(String name) {
        return idOf(speedy.create("Category").field("name", name).execute().expectOk());
    }

    /* ---------------------------------------------------------------- */
    /* Create                                                            */
    /* ---------------------------------------------------------------- */

    @Test
    void create_stampsTokenAndReturnsEtag() {
        SpeedyTestResult created = createNote("first note");

        assertNotNull(created.etag(), "create response for an ETag-enabled entity must carry an ETag header");

        String id = idOf(created);
        getNoteById(id).expectOk()
                .expectJsonPathExists("$.payload[0].rowVersion");
    }

    /* ---------------------------------------------------------------- */
    /* GET / If-None-Match                                               */
    /* ---------------------------------------------------------------- */

    @Test
    void get_singleEntity_returnsEtag() {
        String id = idOf(createNote("note-a"));

        SpeedyTestResult got = getNoteById(id).expectOk();
        assertNotNull(got.etag(), "single-entity GET must carry an ETag");
    }

    @Test
    void get_ifNoneMatch_matchingEtag_returns304WithEtagAndNoBody() {
        String id = idOf(createNote("note-b"));
        String etag = getNoteById(id).etag();

        SpeedyTestResult notModified = getNoteById(id, etag).expectNotModified();
        assertEquals(etag, notModified.etag());
        assertEquals("", notModified.responseBody(), "304 must be bodiless");
    }

    @Test
    void get_ifNoneMatch_staleEtag_returns200WithBody() {
        String id = idOf(createNote("note-c"));

        getNoteById(id, "W/\"not-the-real-tag\"").expectOk()
                .expectJsonPath("$.payload[0].title", equalTo("note-c"));
    }

    @Test
    void get_multiRowList_noEtag() {
        createNote("note-d1");
        createNote("note-d2");

        SpeedyTestResult result = speedy.get("Note").execute().expectOk();

        assertNull(result.etag(), "a multi-row list must not carry an ETag");
    }

    @Test
    void get_nonOptedInEntity_neverReturnsEtag() {
        String categoryId = createCategory("etag-negative-" + System.nanoTime());

        SpeedyTestResult result = speedy.get("Category").key("id", categoryId)
                .header("If-None-Match", "\"anything\"").execute().expectOk();

        assertNull(result.etag(), "an entity with no @SpeedyETag field must never emit an ETag");
    }

    /* ---------------------------------------------------------------- */
    /* PATCH / If-Match                                                  */
    /* ---------------------------------------------------------------- */

    @Test
    void patch_matchingIfMatch_returns200WithFreshEtag_andOldTagThen412s() {
        String id = idOf(createNote("note-e"));
        String originalEtag = getNoteById(id).etag();

        SpeedyTestResult updated = patchNoteTitle(id, "note-e-v2", originalEtag).expectOk();
        String newEtag = updated.etag();
        assertNotNull(newEtag);
        assertNotEquals(originalEtag, newEtag, "a successful update must refresh the ETag");

        // The now-stale original tag must no longer satisfy If-Match.
        patchNoteTitle(id, "note-e-v3", originalEtag).expectPreconditionFailed();
    }

    @Test
    void patch_staleIfMatch_returns412() {
        String id = idOf(createNote("note-f"));

        patchNoteTitle(id, "note-f-v2", "W/\"stale-token\"").expectPreconditionFailed();
    }

    @Test
    void patch_ifMatchStar_onExistingRow_returns200() {
        String id = idOf(createNote("note-g"));

        patchNoteTitle(id, "note-g-v2", "*").expectOk();
    }

    @Test
    void patch_ifMatchStar_onMissingRow_returns412() {
        patchNoteTitle("does-not-exist", "irrelevant", "*").expectPreconditionFailed();
    }

    @Test
    void patch_multiItemBatchWithIfMatch_returns400() {
        String id1 = idOf(createNote("note-h1"));
        String id2 = idOf(createNote("note-h2"));

        speedy.updateMany("Note")
                .item(i -> i.key("id", id1).field("title", "x"))
                .item(i -> i.key("id", id2).field("title", "y"))
                .header("If-Match", "\"whatever\"")
                .execute()
                .expectBadRequest();
    }

    @Test
    void patch_nonOptedInEntity_ifMatch_returns400() {
        String categoryId = createCategory("etag-negative-patch-" + System.nanoTime());

        speedy.update("Category").key("id", categoryId).field("name", "renamed")
                .header("If-Match", "\"anything\"")
                .execute()
                .expectBadRequest();
    }

    /* ---------------------------------------------------------------- */
    /* DELETE / If-Match                                                 */
    /* ---------------------------------------------------------------- */

    @Test
    void delete_staleIfMatch_returns412() {
        String id = idOf(createNote("note-i"));

        deleteNote(id, "W/\"stale-token\"").expectPreconditionFailed();
    }

    @Test
    void delete_matchingIfMatch_returns200() {
        String id = idOf(createNote("note-j"));
        String etag = getNoteById(id).etag();

        deleteNote(id, etag).expectOk();
    }

    @Test
    void delete_multiItemBatchWithIfMatch_returns400() {
        String id1 = idOf(createNote("note-k1"));
        String id2 = idOf(createNote("note-k2"));

        speedy.deleteMany("Note")
                .item(i -> i.key("id", id1))
                .item(i -> i.key("id", id2))
                .header("If-Match", "\"whatever\"")
                .execute()
                .expectBadRequest();
    }

    @Test
    void delete_nonOptedInEntity_ifMatch_returns400() {
        String categoryId = createCategory("etag-negative-delete-" + System.nanoTime());

        speedy.delete("Category").key("id", categoryId)
                .header("If-Match", "\"anything\"")
                .execute()
                .expectBadRequest();
    }

    /* ---------------------------------------------------------------- */
    /* Composite key + TIMESTAMP strategy (PriceHistory)                 */
    /* ---------------------------------------------------------------- */

    @Test
    void compositeKey_getThenPatch_ifMatchRoundTrips() {
        String productId = "1";
        String effectiveAt = "2030-01-01T00:00:00";

        speedy.create("PriceHistory")
                .field("productId", productId)
                .field("effectiveAt", effectiveAt)
                .field("price", 9.99)
                .execute()
                .expectOk();

        SpeedyTestResult got = speedy.get("PriceHistory")
                .key("productId", productId)
                .key("effectiveAt", effectiveAt)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1));
        String etag = got.etag();
        assertNotNull(etag, "PriceHistory GET must carry an ETag (TIMESTAMP strategy)");

        speedy.update("PriceHistory")
                .key("productId", productId)
                .key("effectiveAt", effectiveAt)
                .field("price", 12.5)
                .header("If-Match", etag)
                .execute()
                .expectOk();

        // The tag captured before the update above is now stale.
        speedy.update("PriceHistory")
                .key("productId", productId)
                .key("effectiveAt", effectiveAt)
                .field("price", 20.0)
                .header("If-Match", etag)
                .execute()
                .expectPreconditionFailed();
    }
}
