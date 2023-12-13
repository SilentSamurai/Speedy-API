package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.metamodel.JpaEntityMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class JpaQueryProcessorImpl implements QueryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaQueryProcessorImpl.class);

    final EntityManager entityManager;

    public JpaQueryProcessorImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public SpeedyEntity executeOne(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            QueryBuilder qb = new QueryBuilder(speedyQuery, entityManager);
            Query query = qb.getQuery();
            List<?> resultList = query.getResultList();
            Object reqObj = resultList.get(0);
            return CommonUtil.fromJpaEntity(reqObj, speedyQuery.getFrom());
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public List<SpeedyEntity> executeMany(SpeedyQuery speedyQuery) throws SpeedyHttpException {
        try {
            QueryBuilder qb = new QueryBuilder(speedyQuery, entityManager);
            Query query = qb.getQuery();
            List<?> resultList = query.getResultList();
            List<SpeedyEntity> list = new ArrayList<>();
            for (Object e : resultList) {
                SpeedyEntity speedyEntity = CommonUtil.fromJpaEntity(e, speedyQuery.getFrom());
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
            JpaEntityMetadata entityMetadata = (JpaEntityMetadata) entityKey.getEntityMetadata();
            Object pk = CommonUtil.getPKFromSpeedyValue(entityKey, entityMetadata);
            return entityManager.find(entityMetadata.getEntityClass(), pk) != null;
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public boolean create(SpeedyEntity speedyEntity) throws SpeedyHttpException {
        try {
            JpaEntityMetadata entityMetadata = (JpaEntityMetadata) speedyEntity.getEntityMetadata();
            Object entity = CommonUtil.fromSpeedyEntity(speedyEntity, entityMetadata, entityManager);
            this.saveEntity(entity, entityMetadata);
            return true;
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public boolean update(SpeedyEntityKey speedyEntityKey, SpeedyEntity speedyEntity) throws SpeedyHttpException {
        try {
            JpaEntityMetadata entityMetadata = (JpaEntityMetadata) speedyEntity.getEntityMetadata();
            Object pk = CommonUtil.getPKFromSpeedyValue(speedyEntityKey, entityMetadata);
            Object jpaEntity = entityManager.find(entityMetadata.getEntityClass(), pk);
            CommonUtil.updateFromSpeedyEntity(speedyEntity, jpaEntity, entityMetadata, entityManager);
            this.saveEntity(jpaEntity, entityMetadata);
            return true;
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public boolean delete(SpeedyEntityKey speedyEntityKey) throws SpeedyHttpException {
        try {
            JpaEntityMetadata entityMetadata = (JpaEntityMetadata) speedyEntityKey.getEntityMetadata();
            Object pk = CommonUtil.getPKFromSpeedyValue(speedyEntityKey, entityMetadata);
            Object jpaEntity = entityManager.find(entityMetadata.getEntityClass(), pk);
            this.deleteEntity(jpaEntity, entityMetadata);
            return true;
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    private Object saveEntity(Object entityInstance, EntityMetadata entityMetadata) throws Exception {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(entityInstance);
            entityManager.flush();
            LOGGER.info("{} saved {}", entityMetadata.getName(), entityInstance);
            transaction.commit();
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
        return entityInstance;
    }

    private Object deleteEntity(Object entityInstance, EntityMetadata entityMetadata) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.remove(entityInstance);
            LOGGER.info("{} deleted {}", entityMetadata.getName(), entityInstance);
            transaction.commit();
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
        return entityInstance;
    }


}
