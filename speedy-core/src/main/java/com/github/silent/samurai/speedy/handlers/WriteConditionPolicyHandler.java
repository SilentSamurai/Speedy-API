package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.ForbiddenException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.PolicyTarget;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;

/// Closes the write-condition oracle. Example: a policy allows UPDATE of `Invoice.status` only
/// where `ownerId == principal.id`, but the caller has no READ grant on `ownerId` at all. A bulk
/// PATCH over rows 101-103 then comes back `207 Multi-Status` — 101 succeeded, 102 failed (403),
/// 103 succeeded — and the caller has just learned which invoices they own by reading `ownerId`
/// off a field they were never granted READ on, one bit per row, without ever calling GET.
///
/// So every field a matching UPDATE/REPLACE/DELETE condition tests must itself be plainly (unconditionally)
/// readable by the caller — the same "you can't use as a lever what you can't read" rule that
/// {@link QueryFieldPolicyHandler} applies to filters and sorts. If it isn't, the whole request is
/// rejected up front, before any row is touched, instead of leaking the field through per-row
/// success/failure.
///
/// The check itself depends only on the policy document and the caller, never on any row, so it
/// gives the same answer for every row in the request. That makes it safe to run once per request,
/// before the write is attempted, instead of per row: unlike the write condition it guards, this
/// check can't become an oracle, because it can't vary row-by-row. Key fields are exempt because
/// they are always returned regardless of policy, so gating on one leaks nothing. Only UPDATE/REPLACE/DELETE
/// are guarded — a CREATE condition tests only values the caller just submitted in the same
/// request, so it can reveal nothing they didn't already know.
public class WriteConditionPolicyHandler implements Handler {

    private final PermissionType action;

    public WriteConditionPolicyHandler(PermissionType action) {
        this.action = action;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        PolicyEngine engine = context.find(PolicyEngine.class)
                .orElseThrow(() -> new InternalServerError("Policy engine is required"));
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();

        for (FieldMetadata field : engine.writeConditionFields(entityMetadata, action)) {
            if (field instanceof KeyFieldMetadata) {
                continue;
            }
            if (engine.isAuthorized(PermissionType.READ, PolicyTarget.field(entityMetadata, field), null)
                    != PolicyEffect.ALLOW) {
                throw new ForbiddenException("Field '" + field.getOutputPropertyName()
                        + "' is used in a policy " + action.name().toLowerCase() + " condition but is not readable");
            }
        }
    }
}
