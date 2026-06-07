package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.request.RequestContext;

public class PermissionCheckHandler implements Handler {

    private final Handler next;
    private final PermissionType permission;

    public PermissionCheckHandler(Handler next, PermissionType permission) {
        this.next = next;
        this.permission = permission;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
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
        next.process(context);
    }
}
