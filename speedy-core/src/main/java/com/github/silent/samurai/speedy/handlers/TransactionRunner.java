package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;

/// Shared helpers for the write-op handlers (Create/Update/Delete).
///
/// Eliminates the 5x duplicated try/catch rewrap boilerplate that was born from
/// {@link com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor#runInTransaction}
/// taking a plain {@link Runnable} (which cannot throw checked exceptions). The
/// inner rewrapping converts a checked {@link SpeedyHttpException} to an unchecked
/// {@link SpeedyHttpRuntimeException} so it can escape the lambda; the outer
/// unwrap reverses that to restore the correct HTTP status.
public final class TransactionRunner {

    private TransactionRunner() {
    }

    /// A block of work that may throw a checked {@link SpeedyHttpException}.
    @FunctionalInterface
    public interface SpeedyAction {
        void run() throws SpeedyHttpException;
    }

    /// Wraps a {@link SpeedyAction} into a {@link Runnable} suitable for
    /// {@code runInTransaction(Runnable)}. Checked {@link SpeedyHttpException}s
    /// are converted to unchecked {@link SpeedyHttpRuntimeException} so they
    /// propagate out of the transaction boundary; the outer caller recovers the
    /// original status with {@link #unwrap}.
    public static Runnable wrap(SpeedyAction action) {
        return () -> {
            try {
                action.run();
            } catch (SpeedyHttpException she) {
                throw new SpeedyHttpRuntimeException(she.getStatus(), she);
            } catch (Exception ex) {
                if (ex instanceof SpeedyHttpRuntimeException re) throw re;
                if (ex instanceof RuntimeException re) throw re;
                throw new SpeedyHttpRuntimeException(500, ex);
            }
        };
    }

    /// Unwraps an exception thrown by {@link #wrap} back to a
    /// {@link SpeedyHttpException} with the correct HTTP status. Used by batch
    /// and single-entity write ops after {@code runInTransaction} has thrown.
    public static SpeedyHttpException unwrap(String operation, Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        if (cause instanceof SpeedyHttpException she) {
            return she;
        }
        if (cause instanceof SpeedyHttpRuntimeException sre) {
            return new SpeedyHttpException(sre.getStatus(), sre.getMessage(), sre);
        }
        return new InternalServerError(operation + " failed", e);
    }

    /// Extracts the root cause from an exception thrown by {@link #wrap}, for
    /// handlers that collect per-entity failures instead of aborting the whole
    /// batch (PER_ENTITY mode in Create/Delete).
    public static Throwable extractCause(Exception e) {
        return e.getCause() != null ? e.getCause() : e;
    }
}
