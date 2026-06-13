package com.github.silent.samurai.speedy.jooq.impl;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.exceptions.*;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryResult;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.jooq.impl.query.*;
import com.github.silent.samurai.speedy.conversion.registry.DbConversionRegistry;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
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
import java.util.*;

/// jOOQ-based implementation of QueryProcessor.
/// Translates SpeedyQuery into SQL via jOOQ and executes CRUD operations.
public class JooqQueryProcessorImpl implements QueryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqQueryProcessorImpl.class);

    private final SQLDialect dialect;
    private final Settings settings = new Settings()
            .withRenderQuotedNames(RenderQuotedNames.ALWAYS)
            .withRenderNameStyle(RenderNameStyle.AS_IS);

    private final DbConversionRegistry converter;

    private final DSLContext dslContext;
    private final ThreadLocal<DSLContext> transactionalDslContext = new ThreadLocal<>();

    public JooqQueryProcessorImpl(DataSource dataSource, SpeedyDialect speedyDialect, DbConversionRegistry converter) {
        this.dialect = JooqUtil.toJooqDialect(speedyDialect);
        this.dslContext = DSL.using(dataSource, dialect, settings);
        this.converter = converter;
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
            throw wrapSqlException("Invalid Request", e);
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
            throw wrapSqlException("Invalid Request", e);
        }
    }

    @Override
    public QueryResult executeManyWithCount(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            JooqQueryBuilder countQb = new JooqQueryBuilder(speedyQuery, dsl, converter);
            BigInteger totalCount = countQb.executeCountQuery();
            JooqQueryBuilder qb = new JooqQueryBuilder(speedyQuery, dsl, converter);
            Result<? extends Record> result = qb.executeQuery();
            List<SpeedyEntity> list = new ArrayList<>();
            JooqSqlToSpeedy jooqSQLToSpeedy = new JooqSqlToSpeedy(dsl, converter);
            for (Record record : result) {
                SpeedyEntity speedyEntity = jooqSQLToSpeedy
                        .fromRecord(record, speedyQuery.getFrom(), speedyQuery.getExpand());
                list.add(speedyEntity);
            }
            return new QueryResult(list, totalCount);
        } catch (Exception e) {
            throw wrapSqlException("Invalid Request", e);
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
            throw wrapSqlException("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> create(List<SpeedyEntity> entities) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            SpeedyInsertQuery speedyInsertQuery = new SpeedyInsertQuery(dsl, dialect, converter);
            speedyInsertQuery.insertEntity(entities);

            List<SpeedyEntity> entityList = new ArrayList<>(entities.size());

            if (!entities.isEmpty()) {
                List<SpeedyEntityKey> keys = new ArrayList<>(entities.size());
                for (SpeedyEntity entity : entities) {
                    keys.add(SpeedyEntityUtil.toEntityKey(entity));
                }
                Result<Record> result = new JooqPkQueryBuilder(dsl, dialect, converter).findByPrimaryKeys(keys);

                JooqSqlToSpeedy jooqSqlToSpeedy = new JooqSqlToSpeedy(dsl, converter);
                EntityMetadata entityMetadata = entities.get(0).getMetadata();

                Map<SpeedyEntityKey, SpeedyEntity> entityMap = new HashMap<>();
                for (Record record : result) {
                    SpeedyEntity entity = jooqSqlToSpeedy.fromRecord(record, entityMetadata, Set.of());
                    entityMap.put(SpeedyEntityUtil.toEntityKey(entity), entity);
                }

                for (SpeedyEntityKey key : keys) {
                    SpeedyEntity entity = entityMap.get(key);
                    if (entity != null) {
                        entityList.add(entity);
                    }
                }

                if (entityList.size() != keys.size()) {
                    LOGGER.warn("findByPrimaryKeys returned {} results for {} keys for entity '{}'",
                            entityList.size(), keys.size(), entityMetadata.getName());
                }
            }

            return entityList;
        } catch (Exception e) {
            throw wrapSqlException("Invalid Request", e);
        }
    }

    @Override
    public SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            SpeedyUpdateQuery speedyUpdateQuery = new SpeedyUpdateQuery(dsl, dialect, converter);
            speedyUpdateQuery.updateEntity(pk, entity);

            Result<Record> result = new JooqPkQueryBuilder(dsl, dialect, converter).findByPrimaryKey(pk);

            if (result.isEmpty()) {
                throw new NotFoundException("Entity not found for PK: " + pk);
            }

            return new JooqSqlToSpeedy(dsl, converter)
                    .fromRecord(result.get(0), entity.getMetadata(), Set.of());
        } catch (Exception e) {
            throw wrapSqlException("Invalid Request", e);
        }
    }

    @Override
    public List<SpeedyEntity> delete(List<SpeedyEntityKey> pks) throws SpeedyHttpException {
        try {
            DSLContext dsl = getDsl();
            List<SpeedyEntity> entities;
            if (pks.isEmpty()) {
                entities = new ArrayList<>();
            } else {
                JooqPkQueryBuilder jooqPkQueryBuilder = new JooqPkQueryBuilder(dsl, dialect, converter);
                Result<Record> result = jooqPkQueryBuilder.findByPrimaryKeys(pks);

                JooqSqlToSpeedy jooqSqlToSpeedy = new JooqSqlToSpeedy(dsl, converter);
                EntityMetadata entityMetadata = pks.get(0).getMetadata();

                Map<SpeedyEntityKey, SpeedyEntity> entityMap = new HashMap<>();
                for (Record record : result) {
                    SpeedyEntity entity = jooqSqlToSpeedy.fromRecord(record, entityMetadata, Set.of());
                    entityMap.put(SpeedyEntityUtil.toEntityKey(entity), entity);
                }

                entities = new ArrayList<>(pks.size());
                for (SpeedyEntityKey key : pks) {
                    SpeedyEntity entity = entityMap.get(key);
                    if (entity != null) {
                        entities.add(entity);
                    }
                }

                if (entities.size() != pks.size()) {
                    LOGGER.warn("findByPrimaryKeys returned {} results for {} keys for entity '{}'",
                            entities.size(), pks.size(), entityMetadata.getName());
                }
            }

            new SpeedyDeleteQuery(dsl, dialect, converter).deleteEntity(pks);
            return entities;
        } catch (Exception e) {
            throw wrapSqlException("Invalid Request", e);
        }
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

    private SpeedyHttpException wrapSqlException(String message, Exception cause) {
        if (cause instanceof SpeedyHttpException she) {
            return she;
        }
        if (cause instanceof SpeedyHttpRuntimeException re) {
            return new InternalServerError(re.getMessage(), re);
        }
        if (cause instanceof org.jooq.exception.DataAccessException dae) {
            Throwable sqlCause = dae.getCause();
            if (sqlCause instanceof java.sql.SQLException sqle) {
                String state = sqle.getSQLState();
                if (state != null && (state.startsWith("23") || state.startsWith("22"))) {
                    return new BadRequestException(message, dae);
                }
            }
        }
        return new InternalServerError(message, cause);
    }
}
