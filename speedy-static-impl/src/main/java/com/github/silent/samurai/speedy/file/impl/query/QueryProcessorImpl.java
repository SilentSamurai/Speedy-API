package com.github.silent.samurai.speedy.file.impl.query;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.file.impl.util.JooqUtil;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.jooq.*;
import org.jooq.Record;

import javax.sql.DataSource;
import java.util.List;

public class QueryProcessorImpl implements QueryProcessor {

    DataSource dataSource;
    SQLDialect dialect;

    public QueryProcessorImpl(DataSource dataSource, SQLDialect dialect) {
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

    @Override
    public SpeedyEntity executeOne(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            QueryBuilder qb = new QueryBuilder(speedyQuery, dataSource, dialect);
            SelectJoinStep<Record> query = qb.getQuery();
            Result<Record> result = query.fetch();
            Record record = result.get(0);
            return JooqUtil.fromJpaEntity(record, speedyQuery.getFrom(), speedyQuery.getExpand());
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public List<SpeedyEntity> executeMany(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            QueryBuilder qb = new QueryBuilder(speedyQuery, dataSource, dialect);
            SelectJoinStep<Record> query = qb.getQuery();
            Result<Record> result = query.fetch();
            Record record = result.get(0);
            SpeedyEntity speedyEntity = JooqUtil.fromJpaEntity(record, speedyQuery.getFrom(), speedyQuery.getExpand());
            return List.of(speedyEntity);
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public boolean exists(SpeedyEntityKey entityKey) throws SpeedyHttpException {
        return false;
    }

    @Override
    public SpeedyEntity create(SpeedyEntity entity) throws SpeedyHttpException {
        return null;
    }

    @Override
    public SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        return null;
    }

    @Override
    public SpeedyEntity delete(SpeedyEntityKey entityKey) throws SpeedyHttpException {
        return null;
    }
}
