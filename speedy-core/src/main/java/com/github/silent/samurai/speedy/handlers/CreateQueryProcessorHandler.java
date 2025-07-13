package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.query.jooq.JooqQueryProcessorImpl;
import com.github.silent.samurai.speedy.request.RequestContext;

import javax.sql.DataSource;

public class CreateQueryProcessorHandler implements Handler {

    final Handler next;

    public CreateQueryProcessorHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        DataSource dataSource = context.getConfiguration().dataSourcePerReq();
        QueryProcessor queryProcessor = new JooqQueryProcessorImpl(dataSource, context.getDialect());
        context.setQueryProcessor(queryProcessor);

        next.process(context);
    }
}
