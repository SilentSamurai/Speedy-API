package com.github.silent.samurai.speedy.request.delete;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityTransaction;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class DeleteDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteDataHandler.class);

    private final DeleteRequestContext context;

    public DeleteDataHandler(DeleteRequestContext context) {
        this.context = context;
    }

    public Optional<List<Object>> process() throws Exception {
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        List<Object> deletedObjects = new LinkedList<>();
        try {
            if (!context.getObjectsToBeRemoved().isEmpty()) {
                transaction.begin();
                for (Object parsedObject : context.getObjectsToBeRemoved()) {
                    context.getValidationProcessor().validateDeleteRequestEntity(
                            context.getParser().getPrimaryResource().getResourceMetadata(),
                            parsedObject);
                    context.getEntityManager().remove(parsedObject);
                    deletedObjects.add(parsedObject);
                }
                transaction.commit();
            }
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
        return Optional.of(deletedObjects);
    }
}