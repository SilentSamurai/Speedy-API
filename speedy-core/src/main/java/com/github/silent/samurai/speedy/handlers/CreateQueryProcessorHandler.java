package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.RequestContext;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.query.jooq.JooqQueryProcessorImpl;

import javax.sql.DataSource;

public class CreateQueryProcessorHandler implements Handler {

    final Handler next;
    final ISpeedyConfiguration speedyConfiguration;
    final SpeedyDialect speedyDialect;

    public CreateQueryProcessorHandler(Handler handler, ISpeedyConfiguration speedyConfiguration) {
        this.next = handler;
        this.speedyConfiguration = speedyConfiguration;
        this.speedyDialect = speedyConfiguration.getDialect();
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        DataSource dataSource = speedyConfiguration.dataSourcePerReq();
        QueryProcessor queryProcessor = new JooqQueryProcessorImpl(dataSource, this.speedyDialect);
        context.setQueryProcessor(queryProcessor);

        next.process(context);
    }
}
