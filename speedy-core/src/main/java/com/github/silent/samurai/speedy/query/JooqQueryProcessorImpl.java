package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.Converter;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.query.jooq.*;
import com.github.silent.samurai.speedy.utils.SpeedyEntityUtil;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JooqQueryProcessorImpl implements QueryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqQueryProcessorImpl.class);

    private final DataSource dataSource;
    private final SQLDialect dialect;
    private final Settings settings = new Settings()
            .withRenderQuotedNames(RenderQuotedNames.ALWAYS)
            .withRenderNameStyle(RenderNameStyle.AS_IS);

    private final Converter converter = new JooqConversionImpl();

    private final DSLContext dslContext;
    private final ThreadLocal<DSLContext> transactionalDslContext = new ThreadLocal<>();

    public JooqQueryProcessorImpl(DataSource dataSource, SpeedyDialect speedyDialect) {
        this.dataSource = dataSource;
        this.dialect = JooqUtil.toJooqDialect(speedyDialect);
        this.dslContext = DSL.using(dataSource, dialect, settings);
    }

    private DSLContext getDsl() {
        DSLContext tx = transactionalDslContext.get();
        return tx != null ? tx : dslContext;
    }

    @Override
    public BigInteger executeCount(SpeedyQuery query) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            JooqQueryBuilder qb = new JooqQueryBuilder(query, dsl, converter);
            return qb.executeCountQuery();
        } catch (Exception e) {
            throw new BadRequestException("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> executeMany(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            JooqQueryBuilder qb = new JooqQueryBuilder(speedyQuery, dsl, converter);
            Result<? extends Record> result = qb.executeQuery();
            List<SpeedyEntity> list = new ArrayList<>();
            JooqSqlToSpeedy jooqSQLToSpeedy = new JooqSqlToSpeedy(dsl, converter);
            for (Record record : result) {
                SpeedyEntity speedyEntity = jooqSQLToSpeedy
                        .fromRecord(record, speedyQuery.getFrom(), speedyQuery.getExpand());
                list.add(speedyEntity);
            }
            return list;
        } catch (Exception e) {
            throw new BadRequestException("Invalid Request", e);
        }
    }

    @Override
    public boolean exists(SpeedyEntityKey entityKey) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            Result<Record> result = new JooqPkQueryBuilder(dsl, dialect, converter)
                    .findByPrimaryKey(entityKey);
            return !result.isEmpty();
        } catch (Exception e) {
            throw new BadRequestException("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> create(List<SpeedyEntity> entities) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            SpeedyInsertQuery speedyInsertQuery = new SpeedyInsertQuery(dsl, dialect, converter);
            speedyInsertQuery.insertEntity(entities);

            List<SpeedyEntity> entityList = new ArrayList<>(entities.size());

            for (SpeedyEntity entity : entities) {
                SpeedyEntityKey entityKey = SpeedyEntityUtil.toEntityKey(entity);
                Result<Record> result = new JooqPkQueryBuilder(dsl, dialect, converter).findByPrimaryKey(entityKey);

                SpeedyEntity speedyEntity = new JooqSqlToSpeedy(dsl, converter)
                        .fromRecord(result.get(0), entity.getMetadata(), Set.of());

                entityList.add(speedyEntity);
            }

            return entityList;
        } catch (Exception e) {
            throw new BadRequestException("Invalid Request", e);
        }
    }

    @Override
    public SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            SpeedyUpdateQuery speedyUpdateQuery = new SpeedyUpdateQuery(dsl, dialect, converter);
            speedyUpdateQuery.updateEntity(pk, entity);

            Result<Record> result = new JooqPkQueryBuilder(dsl, dialect, converter).findByPrimaryKey(pk);

            return new JooqSqlToSpeedy(dsl, converter)
                    .fromRecord(result.get(0), entity.getMetadata(), Set.of());
        } catch (Exception e) {
            throw new BadRequestException("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> delete(List<SpeedyEntityKey> pks) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            List<SpeedyEntity> entities = new ArrayList<>(pks.size());
            JooqPkQueryBuilder jooqPkQueryBuilder = new JooqPkQueryBuilder(dsl, dialect, converter);

            for (SpeedyEntityKey pk : pks) {
                Result<Record> result = jooqPkQueryBuilder.findByPrimaryKey(pk);
                SpeedyEntity entity = new JooqSqlToSpeedy(dsl, converter)
                        .fromRecord(result.get(0), pk.getMetadata(), Set.of());

                entities.add(entity);
            }

            new SpeedyDeleteQuery(dsl, dialect, converter).deleteEntity(pks);
            return entities;
        } catch (Exception e) {
            throw new BadRequestException("Invalid Request", e);
        }
    }

    @Override
    public JooqConversionImpl getConversionProcessor() {
        return new JooqConversionImpl();
    }

    @Override
    public void runInTransaction(Runnable block) {
        dslContext.transaction(conf -> {
            transactionalDslContext.set(conf.dsl());
            try {
                block.run();
            } finally {
                transactionalDslContext.remove();
            }
        });
    }
}
