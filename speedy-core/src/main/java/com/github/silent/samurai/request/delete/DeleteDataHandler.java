package com.github.silent.samurai.request.delete;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityTransaction;

public class DeleteDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteDataHandler.class);

    private final DeleteRequestContext context;

    public DeleteDataHandler(DeleteRequestContext context) {
        this.context = context;
    }

    public void process() {
        EntityTransaction transaction = context.getEntityManager().getTransaction();
        try {
            if (!context.getParsedObjects().isEmpty()) {
                transaction.begin();
                for (Object parsedObject : context.getParsedObjects()) {
                    context.getEntityManager().remove(parsedObject);
                }
                transaction.commit();
            }
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
    }
}
