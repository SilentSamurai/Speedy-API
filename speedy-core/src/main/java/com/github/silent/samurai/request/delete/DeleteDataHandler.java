package com.github.silent.samurai.request.delete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityTransaction;

public class DeleteDataHandler {

    Logger logger = LogManager.getLogger(DeleteDataHandler.class);

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
