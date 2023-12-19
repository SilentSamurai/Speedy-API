package com.github.silent.samurai.speedy.jpa.impl.query;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaEntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.metamodel.JpaEntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.util.CommonUtil;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
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
            return CommonUtil.fromJpaEntity(reqObj, speedyQuery.getFrom(), speedyQuery.getExpand());
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
                SpeedyEntity speedyEntity = CommonUtil.fromJpaEntity(e, speedyQuery.getFrom(), speedyQuery.getExpand());
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
            JpaEntityMetadata entityMetadata = (JpaEntityMetadata) entityKey.getMetadata();
            Object pk = CommonUtil.getPKFromSpeedyValue(entityKey, entityMetadata);
            return entityManager.find(entityMetadata.getEntityClass(), pk) != null;
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public SpeedyEntity create(SpeedyEntity speedyEntity) throws SpeedyHttpException {
        try {
            IJpaEntityMetadata entityMetadata = (IJpaEntityMetadata) speedyEntity.getMetadata();
            Object entity = CommonUtil.fromSpeedyEntity(speedyEntity, entityMetadata, entityManager);
            Object saveEntity = this.saveEntity(entity, entityMetadata);
            return CommonUtil.fromJpaEntity(saveEntity, entityMetadata, Collections.emptySet());
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public SpeedyEntity update(SpeedyEntityKey speedyEntityKey, SpeedyEntity speedyEntity) throws SpeedyHttpException {
        try {
            IJpaEntityMetadata entityMetadata = (IJpaEntityMetadata) speedyEntity.getMetadata();
            Object pk = CommonUtil.getPKFromSpeedyValue(speedyEntityKey, entityMetadata);
            Object jpaEntity = entityManager.find(entityMetadata.getEntityClass(), pk);
            if (jpaEntity == null) {
                throw new BadRequestException("Entity not found");
            }
            CommonUtil.updateFromSpeedyEntity(speedyEntity, jpaEntity, entityMetadata, entityManager);
            Object savedEntity = this.updateEntity(jpaEntity, entityMetadata);
            return CommonUtil.fromJpaEntity(savedEntity, entityMetadata, Collections.emptySet());
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public SpeedyEntity delete(SpeedyEntityKey speedyEntityKey) throws SpeedyHttpException {
        try {
            JpaEntityMetadata entityMetadata = (JpaEntityMetadata) speedyEntityKey.getMetadata();
            Object pk = CommonUtil.getPKFromSpeedyValue(speedyEntityKey, entityMetadata);
            Object jpaEntity = entityManager.find(entityMetadata.getEntityClass(), pk);
            if (jpaEntity == null) {
                throw new BadRequestException("Entity not found");
            }
            Object deletedEntity = this.deleteEntity(jpaEntity, entityMetadata);
            return CommonUtil.fromJpaEntity(deletedEntity, entityMetadata, Collections.emptySet());
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    private Object saveEntity(Object entityInstance, EntityMetadata entityMetadata) throws Exception {
        EntityTransaction transaction = entityManager.getTransaction();
        Object savedEntity;
        try {
            transaction.begin();
            savedEntity = entityManager.merge(entityInstance);
            entityManager.flush();
            LOGGER.info("{} saved {}", entityMetadata.getName(), savedEntity);
            transaction.commit();
        } catch (Throwable throwable) {
            transaction.rollback();
            throw throwable;
        }
        return savedEntity;
    }

    private Object updateEntity(Object entityInstance, EntityMetadata entityMetadata) throws Exception {
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
