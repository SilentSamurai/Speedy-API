package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.SpeedyEntityUtil;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
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

    private final DSLContext dslContext;

    public JooqQueryProcessorImpl(DataSource dataSource, SQLDialect dialect) {
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.dslContext = DSL.using(dataSource, dialect, settings);
    }

    @Override
    public SpeedyEntity executeOne(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            JooqQueryBuilder qb = new JooqQueryBuilder(speedyQuery, dslContext);
            Result<Record> result = qb.executeQuery();
            Record record = result.get(0);
            return new JooqSqlToSpeedy(dslContext)
                    .fromRecord(record, speedyQuery.getFrom(), speedyQuery.getExpand());
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public List<SpeedyEntity> executeMany(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            JooqQueryBuilder qb = new JooqQueryBuilder(speedyQuery, dslContext);
            Result<Record> result = qb.executeQuery();
            List<SpeedyEntity> list = new ArrayList<>();
            JooqSqlToSpeedy jooqSQLToSpeedy = new JooqSqlToSpeedy(dslContext);
            for (Record record : result) {
                SpeedyEntity speedyEntity = jooqSQLToSpeedy
                        .fromRecord(record, speedyQuery.getFrom(), speedyQuery.getExpand());
                list.add(speedyEntity);
            }
            return list;
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public boolean exists(SpeedyEntityKey entityKey) throws SpeedyHttpException {
        try {
            Result<Record> result = new SpeedyToJooqSql(dslContext)
                    .findByPrimaryKey(entityKey);
            return !result.isEmpty();
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public SpeedyEntity create(SpeedyEntity entity) throws SpeedyHttpException {
        try {
            SpeedyToJooqSql speedyToJooqSql = new SpeedyToJooqSql(dslContext);
            speedyToJooqSql.insertEntity(entity);

            SpeedyEntityKey entityKey = SpeedyEntityUtil.toEntityKey(entity);
            Result<Record> result = speedyToJooqSql.findByPrimaryKey(entityKey);

            return new JooqSqlToSpeedy(dslContext)
                    .fromRecord(result.get(0), entity.getMetadata(), Set.of());
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public SpeedyEntity update(SpeedyEntityKey pk, SpeedyEntity entity) throws SpeedyHttpException {
        try {
            SpeedyToJooqSql speedyToJooqSql = new SpeedyToJooqSql(dslContext);

            speedyToJooqSql.updateEntity(pk, entity);

            Result<Record> result = speedyToJooqSql.findByPrimaryKey(pk);

            return new JooqSqlToSpeedy(dslContext)
                    .fromRecord(result.get(0), entity.getMetadata(), Set.of());
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public SpeedyEntity delete(SpeedyEntityKey pk) throws SpeedyHttpException {
        try {
            SpeedyToJooqSql speedyToJooqSql = new SpeedyToJooqSql(dslContext);

            Result<Record> result = speedyToJooqSql.findByPrimaryKey(pk);
            SpeedyEntity entity = new JooqSqlToSpeedy(dslContext)
                    .fromRecord(result.get(0), pk.getMetadata(), Set.of());

            speedyToJooqSql.deleteEntity(pk);
            return entity;
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }
}
