package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.annotations.SpeedyCustomValidation;
import com.github.silent.samurai.annotations.SpeedyIgnore;
import com.github.silent.samurai.enums.IgnoreType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.util.FieldUtils;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class JpaMetaModel {

    public static Logger logger = LogManager.getLogger(JpaMetaModel.class);
    private final Map<String, ResourceMetadata> entityMap = new HashMap<>();

    public static MemberMetadata findMetadata(Attribute<?, ?> attribute, Class<?> entityClass) {
        Member member = attribute.getJavaMember();
        MemberMetadata memberMetadata = new MemberMetadata();
        memberMetadata.setJpaAttribute(attribute);
        memberMetadata.setName(member.getName());
        if (attribute instanceof SingularAttribute) {
            memberMetadata.setId(((SingularAttribute<?, ?>) attribute).isId());
        } else {
            memberMetadata.setId(false);
        }

        // MZ: Find the correct method
        for (Method method : entityClass.getMethods()) {
            if (method.getName().toLowerCase().endsWith(member.getName().toLowerCase())) {
                try {
                    if ((method.getName().startsWith("get")) && (method.getName().length() == (member.getName().length() + 3))) {
                        memberMetadata.setGetter(method);
                    }
                    if ((method.getName().startsWith("set")) && (method.getName().length() == (member.getName().length() + 3))) {
                        memberMetadata.setSetter(method);
                    }
                    memberMetadata.setField(FieldUtils.getField(member.getDeclaringClass(), member.getName()));
                    SpeedyCustomValidation annotation = AnnotationUtils.getAnnotation(memberMetadata.getField(), SpeedyCustomValidation.class);
                    if (annotation != null) {
                        memberMetadata.setCustomValidation(annotation.value());
                    }
                } catch (IllegalStateException e) {
                    logger.fatal("Could not determine method: {} ", member, e);
                }
            }
        }
        return memberMetadata;
    }

    public void addEntity(EntityType<?> entityType) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setName(entityType.getName());
        metadata.setJpaEntityType(entityType);
        for (Attribute<?, ?> attribute : entityType.getAttributes()) {
            MemberMetadata memberMetadata = findMetadata(attribute, entityType.getJavaType());
            SpeedyIgnore annotation = AnnotationUtils.getAnnotation(memberMetadata.getField(), SpeedyIgnore.class);
            if (annotation != null) {
                if (annotation.value() == IgnoreType.ALL) {
                    continue;
                }
                memberMetadata.setIgnoreType(annotation.value());
            }
            metadata.getMembersMetadata().add(memberMetadata);
            metadata.getMemberMap().put(attribute.getName(), memberMetadata);
            if (memberMetadata.isId()) {
                metadata.getIdFields().add(attribute.getName());
            }

        }
        entityMap.put(entityType.getName(), metadata);
    }

    public ResourceMetadata getEntityMetadata(String entity) {
        return entityMap.get(entity);
    }


}
