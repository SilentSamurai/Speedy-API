package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.hamcrest.Matchers.equalTo;

/// A key is not always a value Speedy wrote. Rows arrive from a seed script, a migration, an ETL job
/// or another application, and a lookup by their key has to find them.
///
/// That is a real distinction only on a backend with no temporal storage class. SQLite keeps a DATE
/// column as text, so comparing it is comparing text — which holds only while every value in the
/// column has the same width. Speedy writes a date as `yyyy-MM-dd 00:00:00.000`; the row below is
/// written the way anything else would write it, as `2025-06-01`. The two are not equal as text, so
/// before the dialect canonicalised the key column the row was invisible to GET-by-key, and
/// PATCH and DELETE addressed nothing.
///
/// {@link com.github.silent.samurai.speedy.entity.TypedCompositeKeyEntity} is the entity to prove it
/// on: its key spans a DATE alongside a BIGINT and a VARCHAR, so the canonicalisation has to apply
/// to the temporal column and leave the other two alone. The strict backends compare dates as dates
/// and pass either way — this test can only fail on the backend that disproves it.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class DatabaseWrittenKeyLookupTest {

    private static final String ENTITY = "TypedCompositeKeyEntity";
    private static final long NUM_ID = 9100L;
    private static final String CODE = "DBWRITTEN";
    /// The date as anything other than Speedy would write it: no time part at all.
    private static final String DATE = "2025-06-01";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DataSource dataSource;

    private SpeedyTest speedy;

    @BeforeEach
    void setUp() throws Exception {
        speedy = SpeedyTest.mockMvc(mvc);
        removeRow();
        insertRowOutsideSpeedy();
    }

    /// Removed the same way it was inserted. Going through the JPA repository would not do: reading
    /// the row back is Hibernate's problem too, and on SQLite its driver rejects a date narrower than
    /// the form it writes — so the row would survive the cleanup and collide with the next insert.
    @AfterEach
    void cleanup() throws Exception {
        removeRow();
    }

    @Test
    void getByKey_findsARowTheDatabaseWrote() {
        speedy.get(ENTITY)
                .key("numId", NUM_ID)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(1))
                .expectJsonPath("$.payload[0].description", equalTo("written-outside-speedy"));
    }

    @Test
    void patchByKey_updatesARowTheDatabaseWrote() {
        speedy.update(ENTITY)
                .key("numId", NUM_ID)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .field("description", "patched")
                .execute()
                .expectOk();

        speedy.get(ENTITY)
                .key("numId", NUM_ID)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload[0].description", equalTo("patched"));
    }

    @Test
    void deleteByKey_removesARowTheDatabaseWrote() {
        speedy.delete(ENTITY)
                .key("numId", NUM_ID)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .execute()
                .expectOk();

        // GET-by-key is a filtered read, not a single-resource address, so a key matching nothing is
        // an empty payload rather than a 404 — see docs/get-operation.md.
        speedy.get(ENTITY)
                .key("numId", NUM_ID)
                .key("effectiveDate", DATE)
                .key("code", CODE)
                .execute()
                .expectOk()
                .expectJsonPath("$.payload.length()", equalTo(0));
    }

    /// Written as SQL rather than through Speedy or JPA, because both of those would encode the date
    /// the way Speedy reads it back and there would be nothing to prove. The literal is a bare date,
    /// which every backend accepts for a DATE column — and which SQLite, having no such column,
    /// stores verbatim.
    private void insertRowOutsideSpeedy() throws Exception {
        execute("insert into typed_composite_key"
                + " (num_id, effective_date, code, quantity, amount, description)"
                + " values (" + NUM_ID + ", '" + DATE + "', '" + CODE + "', 1, 1.5,"
                + " 'written-outside-speedy')");
    }

    private void removeRow() throws Exception {
        execute("delete from typed_composite_key where num_id = " + NUM_ID);
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
