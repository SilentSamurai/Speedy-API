package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.BulkOperation;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.BulkRequestBody;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

/// Rejects multi-element (bulk) request bodies for entities that have not opted into bulk for
/// this operation via {@code @SpeedyBulk}. Parameterized by {@link BulkOperation} so each write
/// chain enforces its own toggle — mirroring {@link PermissionCheckHandler}. Because PATCH
/// ({@code UPDATE}) and PUT ({@code REPLACE}) run in separate chains, this is where they are
/// distinguished: the shared body parser cannot tell them apart.
///
/// Runs after the body has been parsed (the {@link BulkRequestBody} is already in the
/// context), so it inspects the parsed item count rather than re-reading the raw body.
public class BulkCheckHandler implements Handler {

    private final BulkOperation operation;

    public BulkCheckHandler(BulkOperation operation) {
        this.operation = operation;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyBody body = context.get(SpeedyBody.class);
        if (!(body instanceof BulkRequestBody multiItemBody) || !multiItemBody.isBulk()) {
            return;
        }
        EntityMetadata entity = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        if (!entity.isBulkAllowed(operation)) {
            throw new BadRequestException(
                    String.format("Bulk %s is disabled for entity '%s'",
                            operation.name().toLowerCase(), entity.getName()));
        }
    }
}
