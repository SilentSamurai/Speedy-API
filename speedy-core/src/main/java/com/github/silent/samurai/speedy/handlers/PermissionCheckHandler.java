package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

public class PermissionCheckHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    private final PermissionType permission;

    public PermissionCheckHandler(PermissionType permission) {
        this.permission = permission;
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        boolean allowed = switch (permission) {
            case READ -> entityMetadata.isReadAllowed();
            case CREATE -> entityMetadata.isCreateAllowed();
            case UPDATE -> entityMetadata.isUpdateAllowed();
            case DELETE -> entityMetadata.isDeleteAllowed();
        };
        if (!allowed) {
            String op = permission.name().toLowerCase();
            throw new BadRequestException(
                    String.format("%s not allowed for %s", op, entityMetadata.getName()));
        }
    }
}
