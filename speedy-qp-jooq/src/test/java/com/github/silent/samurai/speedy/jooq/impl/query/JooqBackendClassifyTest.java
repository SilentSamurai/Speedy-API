package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/// A constraint violation is bad client input, so a failed write has to come back as a 400. The
/// driver reports it, but not always at the top of the chain: a batched write wraps the driver's
/// exception in a {@link BatchUpdateException}, and only the inner one carries the code. Looking at
/// a single link made those writes 500s.
///
/// SQLite is the dialect used here because its driver reports no SQLSTATE at all, so the whole
/// answer rests on the numeric code the walk has to reach.
class JooqBackendClassifyTest {

    /// SQLITE_CONSTRAINT_NOTNULL. The driver reports it with a null SQLSTATE.
    private static final int SQLITE_CONSTRAINT_NOTNULL = 1299;
    /// SQLITE_ERROR — a generic failure, not the client's fault.
    private static final int SQLITE_ERROR = 1;

    @Test
    void classifyReachesAConstraintCodeWrappedInABatchUpdateException() {
        DataAccessException wrapped = batchedWriteFailure(SQLITE_CONSTRAINT_NOTNULL);

        Optional<SpeedyHttpException> classified = backend().classify(wrapped);

        assertTrue(classified.isPresent(), "a constraint violation two links down must still be seen");
        assertInstanceOf(BadRequestException.class, classified.get());
    }

    /// The walk must not turn every wrapped failure into a 400 — only the ones the dialect owns.
    @Test
    void classifyLeavesANonClientErrorAlone() {
        DataAccessException wrapped = batchedWriteFailure(SQLITE_ERROR);

        assertFalse(backend().classify(wrapped).isPresent());
    }

    private static DataAccessException batchedWriteFailure(int driverErrorCode) {
        SQLException driverError = new SQLException("constraint failed", null, driverErrorCode);
        // The batch wrapper carries no code of its own — that is the point.
        BatchUpdateException batched = new BatchUpdateException("batch entry failed", new int[0], driverError);
        return new DataAccessException("SQL [insert into ...]", batched);
    }

    private static JooqBackend backend() {
        return new JooqBackend(mock(DataSource.class), SpeedyDialect.SQLITE, null);
    }
}
