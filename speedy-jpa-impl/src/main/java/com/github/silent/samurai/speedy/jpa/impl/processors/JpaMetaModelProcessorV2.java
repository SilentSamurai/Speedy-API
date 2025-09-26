package com.github.silent.samurai.speedy.jpa.impl.processors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.silent.samurai.speedy.annotations.SpeedyAction;
import com.github.silent.samurai.speedy.annotations.SpeedyIgnore;
import com.github.silent.samurai.speedy.annotations.SpeedyType;
import com.github.silent.samurai.speedy.annotations.validation.*;
import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.jpa.impl.util.JavaType2ColumnType;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.FieldBuilder;
import com.github.silent.samurai.speedy.metadata.KeyFieldBuilder;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
import com.github.silent.samurai.speedy.models.DynamicEnum;
// Jakarta Bean Validation annotations
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import com.github.silent.samurai.speedy.validation.rules.*;
import jakarta.persistence.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.annotations.Formula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.silent.samurai.speedy.enums.ActionType.*;
import static com.github.silent.samurai.speedy.jpa.impl.util.JpaUtil.*;

public class JpaMetaModelProcessorV2 implements MetaModelProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaMetaModelProcessorV2.class);
    private final Map<Class<?>, EntityType<?>> typeMap = new HashMap<>();
    private MetaModel metaModel;
    private ISpeedyConfiguration configuration;
    private EntityManagerFactory entityManagerFactory;

    public JpaMetaModelProcessorV2(ISpeedyConfiguration configuration, EntityManagerFactory entityManagerFactory) {
        this.configuration = configuration;
        this.entityManagerFactory = entityManagerFactory;
    }

    static Field findReflectionField(Attribute<?, ?> attribute, Class<?> entityClass) {
        if (attribute.getJavaMember() instanceof Field) {
            return (Field) attribute.getJavaMember();
        } else {
            Member member = attribute.getJavaMember();
            return getField(entityClass, member.getName());
        }
    }

    @Override
    public MetaModel getMetaModel() {
        return metaModel;
    }

    @Override
    public void processMetaModel(MetaModelBuilder builder) {
        try {
            processEntities(builder);
            this.metaModel = builder.build();
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    void processEntities(MetaModelBuilder builder) throws NotFoundException {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();
        for (EntityType<?> entityType : entities) {
            SpeedyIgnore annotation = entityType.getBindableJavaType().getAnnotation(SpeedyIgnore.class);
            if (annotation != null) {
                continue;
            }
            EntityBuilder entity = processEntity(entityType, builder);
            typeMap.put(entityType.getJavaType(), entityType);
            LOGGER.info("registering resources {}", entityType.getName());
        }
        processAssociations(builder);
    }

    EntityBuilder processEntity(EntityType<?> entityType, MetaModelBuilder builder) {
        EntityBuilder entity = builder.entity(entityType.getName());
        entity.dbTableName(getTableName(entityType.getJavaType()));
        entity.name(entityType.getName());
        entity.hasCompositeKey(!entityType.hasSingleIdAttribute());
        SpeedyAction annotation = entityType.getBindableJavaType().getAnnotation(SpeedyAction.class);
        if (annotation != null) {
            Arrays.stream(annotation.value())
                    .forEach(entity::addActionType);
        }
        for (Attribute<?, ?> attribute : entityType.getAttributes()) {
            if (isIgnorable(attribute, entityType.getJavaType())) {
                continue;
            }
            FieldBuilder field = processField(
                    attribute,
                    entityType.getJavaType(),
                    entity
            );
        }
        return entity;
    }

    boolean isIgnorable(Attribute<?, ?> attribute, Class<?> entityClass) {
        if (attribute.getJavaType().getAnnotation(SpeedyIgnore.class) != null) {
            return true;
        }
        Field field = findReflectionField(attribute, entityClass);
        if (attribute.isAssociation() && AnnotationUtils.getAnnotation(field, OneToMany.class) != null) {
            return true;
        }
        return false;
    }

    private FieldBuilder processField(Attribute<?, ?> attribute,
                                      Class<?> entityClass,
                                      EntityBuilder entity) {

        boolean isId = attribute instanceof SingularAttribute && ((SingularAttribute<?, ?>) attribute).isId();
        Member member = attribute.getJavaMember();
        Field field = findReflectionField(attribute, entityClass);

        ColumnType columnType = findColumnTypeFromField(attribute);
        String outputName = findOutputName(field, member);
        String dbColumnName = findDbColumnName(attribute, entityClass, field, member);


        FieldBuilder fieldMetadata = isId ? entity.keyField(outputName) : entity.field(outputName);

        fieldMetadata.dbColumnName(dbColumnName);
        fieldMetadata.columnType(columnType);
        fieldMetadata.insertable(true);
        fieldMetadata.unique(false);
        fieldMetadata.updatable(true);
        fieldMetadata.nullable(false);
        fieldMetadata.required(false);
        fieldMetadata.serializable(true);
        fieldMetadata.deserializable(true);
        fieldMetadata.collection(attribute.isCollection());

        SpeedyType speedyType = AnnotationUtils.getAnnotation(field, SpeedyType.class);
        if (speedyType != null) {
            fieldMetadata.columnTypeOverride(speedyType.value());
        }

        // Populate enum metadata on FieldBuilder
        Enumerated enumeratedAnn = AnnotationUtils.getAnnotation(field, Enumerated.class);
        Class<?> effectiveType = attribute.isCollection() ? resolveGenericFieldType(field) : attribute.getJavaType();
        boolean isEnumType = effectiveType != null && effectiveType.isEnum();
        if (isEnumType) {

            if (enumeratedAnn != null) {
                EnumMode em = enumeratedAnn.value() == EnumType.STRING ? EnumMode.STRING : EnumMode.ORDINAL;
                DynamicEnum dynamicEnum = DynamicEnum.of((Class<? extends Enum<?>>) effectiveType);
                fieldMetadata.enumField(em, em, dynamicEnum);
            } else {
                DynamicEnum dynamicEnum = DynamicEnum.of((Class<? extends Enum<?>>) effectiveType);
                fieldMetadata.enumField(EnumMode.STRING, EnumMode.ORDINAL, dynamicEnum);
            }
        }

        Column columnAnnotation = AnnotationUtils.getAnnotation(field, Column.class);
        if (columnAnnotation != null) {
            fieldMetadata.insertable(columnAnnotation.insertable());
            fieldMetadata.unique(columnAnnotation.unique());
            fieldMetadata.updatable(columnAnnotation.updatable());
            fieldMetadata.nullable(columnAnnotation.nullable());
        }

        JoinColumn joinColumnAnnotation = AnnotationUtils.getAnnotation(field, JoinColumn.class);
        if (joinColumnAnnotation != null) {
            fieldMetadata.insertable(joinColumnAnnotation.insertable());
            fieldMetadata.unique(joinColumnAnnotation.unique());
            fieldMetadata.updatable(joinColumnAnnotation.updatable());
            fieldMetadata.nullable(joinColumnAnnotation.nullable());
        }

        GeneratedValue generatedValueAnnotation = AnnotationUtils.getAnnotation(field, GeneratedValue.class);
        if (generatedValueAnnotation != null) {
            if (isId) {
                boolean isUuidGenerationRequired =
                        generatedValueAnnotation.generator().toUpperCase().contains("UUID") ||
                                generatedValueAnnotation.strategy() == GenerationType.UUID;
                if (isUuidGenerationRequired) {
                    ((KeyFieldBuilder) fieldMetadata).shouldGenerateKey(true);
                }
            }
            fieldMetadata.insertable(false);
            fieldMetadata.updatable(false);
            fieldMetadata.nullable(false);
            fieldMetadata.deserializable(false);
        }

        Formula formulaAnnotation = AnnotationUtils.getAnnotation(field, Formula.class);
        if (formulaAnnotation != null) {
            fieldMetadata.insertable(false);
            fieldMetadata.updatable(false);
            fieldMetadata.nullable(false);
            fieldMetadata.deserializable(false);
        }

        // Apply consolidated validation annotations
        applyValidationAnnotations(field, fieldMetadata);


        JsonIgnore jsonIgnore = AnnotationUtils.getAnnotation(field, JsonIgnore.class);
        if (jsonIgnore != null) {
            fieldMetadata.serializable(false);
            fieldMetadata.deserializable(false);
        }

        SpeedyAction speedyAction = AnnotationUtils.getAnnotation(field, SpeedyAction.class);
        if (speedyAction != null) {
            Set<ActionType> actionTypesSet = Arrays.stream(speedyAction.value()).collect(Collectors.toSet());

            // give as less permission as you can
            fieldMetadata.serializable(false);
            fieldMetadata.insertable(false);
            fieldMetadata.updatable(false);
            fieldMetadata.deserializable(false);

            if (actionTypesSet.contains(CREATE)) {
                fieldMetadata.insertable(true);
                fieldMetadata.deserializable(true);
            }
            if (actionTypesSet.contains(UPDATE)) {
                fieldMetadata.updatable(true);
                fieldMetadata.deserializable(true);
            }
            if (actionTypesSet.contains(DELETE)) {
                // todo: figure out what logic can be done here
                fieldMetadata.deserializable(true);
            }
            if (actionTypesSet.contains(READ)) {
                fieldMetadata.serializable(true);
            }
            if (actionTypesSet.contains(ALL)) {
                fieldMetadata.insertable(true);
                fieldMetadata.updatable(true);
                fieldMetadata.serializable(true);
                fieldMetadata.deserializable(true);
            }
        }

        return fieldMetadata;
    }

    // Consolidated validation annotation processing
    private void applyValidationAnnotations(Field field, FieldBuilder fieldMetadata) {
        // Speedy custom annotations
        SpeedyMin minAnn = AnnotationUtils.getAnnotation(field, SpeedyMin.class);
        if (minAnn != null) {
            fieldMetadata.addValidationRule(new MinRule(minAnn.value()));
        }
        SpeedyMax maxAnn = AnnotationUtils.getAnnotation(field, SpeedyMax.class);
        if (maxAnn != null) {
            fieldMetadata.addValidationRule(new MaxRule(maxAnn.value()));
        }
        SpeedyLength lenAnn = AnnotationUtils.getAnnotation(field, SpeedyLength.class);
        if (lenAnn != null) {
            fieldMetadata.addValidationRule(new LengthRule(lenAnn.min(), lenAnn.max()));
        }
        SpeedyRegex regexAnn = AnnotationUtils.getAnnotation(field, SpeedyRegex.class);
        if (regexAnn != null) {
            fieldMetadata.addValidationRule(new RegexRule(regexAnn.value()));
        }
        SpeedyEmail emailAnn = AnnotationUtils.getAnnotation(field, SpeedyEmail.class);
        if (emailAnn != null) {
            fieldMetadata.addValidationRule(new EmailRule());
        }
        // URL validation
        SpeedyUrl urlAnn = AnnotationUtils.getAnnotation(field, SpeedyUrl.class);
        if (urlAnn != null) {
            fieldMetadata.addValidationRule(new UrlRule());
        }
        // Date validation rules
        SpeedyDateWithFormat dfAnn = AnnotationUtils.getAnnotation(field, SpeedyDateWithFormat.class);
        if (dfAnn != null) {
            fieldMetadata.addValidationRule(new DateFormatRule(dfAnn.iso()));
        }
        SpeedyFuture futureAnn = AnnotationUtils.getAnnotation(field, SpeedyFuture.class);
        if (futureAnn != null) {
            fieldMetadata.addValidationRule(new FutureRule(futureAnn.message()));
        }
        SpeedyPast pastAnn = AnnotationUtils.getAnnotation(field, SpeedyPast.class);
        if (pastAnn != null) {
            fieldMetadata.addValidationRule(new PastRule(pastAnn.message()));
        }
        SpeedyDateRange rangeAnn = AnnotationUtils.getAnnotation(field, SpeedyDateRange.class);
        if (rangeAnn != null) {
            fieldMetadata.addValidationRule(new DateRangeRule(rangeAnn.min(), rangeAnn.max(), rangeAnn.message()));
        }
        SpeedyNotBlank speedyNotBlank = AnnotationUtils.getAnnotation(field, SpeedyNotBlank.class);
        if (speedyNotBlank != null) {
            fieldMetadata.addValidationRule(new NotBlankRule());
        }
        // New Speedy numeric sign annotations
        SpeedyPositive spPos = AnnotationUtils.getAnnotation(field, SpeedyPositive.class);
        if (spPos != null) {
            fieldMetadata.addValidationRule(new PositiveRule());
        }
        SpeedyPositiveOrZero spPosZero = AnnotationUtils.getAnnotation(field, SpeedyPositiveOrZero.class);
        if (spPosZero != null) {
            fieldMetadata.addValidationRule(new PositiveOrZeroRule());
        }
        SpeedyNegative spNeg = AnnotationUtils.getAnnotation(field, SpeedyNegative.class);
        if (spNeg != null) {
            fieldMetadata.addValidationRule(new NegativeRule());
        }
        SpeedyNegativeOrZero spNegZero = AnnotationUtils.getAnnotation(field, SpeedyNegativeOrZero.class);
        if (spNegZero != null) {
            fieldMetadata.addValidationRule(new NegativeOrZeroRule());
        }
        // Decimal boundary and digits
        SpeedyDecimalMin spDecMin = AnnotationUtils.getAnnotation(field, SpeedyDecimalMin.class);
        if (spDecMin != null) {
            fieldMetadata.addValidationRule(new DecimalMinRule(spDecMin.value(), spDecMin.inclusive()));
        }
        SpeedyDecimalMax spDecMax = AnnotationUtils.getAnnotation(field, SpeedyDecimalMax.class);
        if (spDecMax != null) {
            fieldMetadata.addValidationRule(new DecimalMaxRule(spDecMax.value(), spDecMax.inclusive()));
        }
        SpeedyDigits spDigits = AnnotationUtils.getAnnotation(field, SpeedyDigits.class);
        if (spDigits != null) {
            fieldMetadata.addValidationRule(new DigitsRule(spDigits.integer(), spDigits.fraction()));
        }

        // Jakarta Bean Validation annotations
        Min beanMin = AnnotationUtils.getAnnotation(field, Min.class);
        if (beanMin != null) {
            fieldMetadata.addValidationRule(new MinRule(beanMin.value()));
        }
        Max beanMax = AnnotationUtils.getAnnotation(field, Max.class);
        if (beanMax != null) {
            fieldMetadata.addValidationRule(new MaxRule(beanMax.value()));
        }
        Size sizeAnn = AnnotationUtils.getAnnotation(field, Size.class);
        if (sizeAnn != null) {
            fieldMetadata.addValidationRule(new LengthRule(sizeAnn.min(), sizeAnn.max()));
        }
        Pattern patternAnn = AnnotationUtils.getAnnotation(field, Pattern.class);
        if (patternAnn != null) {
            fieldMetadata.addValidationRule(new RegexRule(patternAnn.regexp()));
        }
        Email emailBeanAnn = AnnotationUtils.getAnnotation(field, Email.class);
        if (emailBeanAnn != null) {
            fieldMetadata.addValidationRule(new EmailRule());
        }
        NotBlank notBlankAnn = AnnotationUtils.getAnnotation(field, NotBlank.class);
        if (notBlankAnn != null) {
            fieldMetadata.addValidationRule(new NotBlankRule());
        }
        // Numeric sign validations
        Positive posAnn = AnnotationUtils.getAnnotation(field, Positive.class);
        if (posAnn != null) {
            fieldMetadata.addValidationRule(new PositiveRule());
        }
        PositiveOrZero posZeroAnn = AnnotationUtils.getAnnotation(field, PositiveOrZero.class);
        if (posZeroAnn != null) {
            fieldMetadata.addValidationRule(new PositiveOrZeroRule());
        }
        Negative negAnnJak = AnnotationUtils.getAnnotation(field, Negative.class);
        if (negAnnJak != null) {
            fieldMetadata.addValidationRule(new NegativeRule());
        }
        NegativeOrZero negZeroJak = AnnotationUtils.getAnnotation(field, NegativeOrZero.class);
        if (negZeroJak != null) {
            fieldMetadata.addValidationRule(new NegativeOrZeroRule());
        }
        DecimalMin decMinAnnJak = AnnotationUtils.getAnnotation(field, DecimalMin.class);
        if (decMinAnnJak != null) {
            fieldMetadata.addValidationRule(new DecimalMinRule(decMinAnnJak.value(), decMinAnnJak.inclusive()));
        }
        DecimalMax decMaxAnnJak = AnnotationUtils.getAnnotation(field, DecimalMax.class);
        if (decMaxAnnJak != null) {
            fieldMetadata.addValidationRule(new DecimalMaxRule(decMaxAnnJak.value(), decMaxAnnJak.inclusive()));
        }
        Digits digitsAnnJak = AnnotationUtils.getAnnotation(field, Digits.class);
        if (digitsAnnJak != null) {
            fieldMetadata.addValidationRule(new DigitsRule(digitsAnnJak.integer(), digitsAnnJak.fraction()));
        }
        NotNull notNullAnn = AnnotationUtils.getAnnotation(field, NotNull.class);
        if (notNullAnn != null) {
            fieldMetadata.nullable(false);
            fieldMetadata.required(true);
        }
    }

    String findOutputName(Field field, Member member) {
        JsonProperty propertyAnnotation = AnnotationUtils.getAnnotation(field, JsonProperty.class);
        if (propertyAnnotation != null) {
            return propertyAnnotation.value();
        }
        return member.getName();
    }

    ColumnType findColumnTypeFromField(Attribute<?, ?> attribute) {
        try {
            return JavaType2ColumnType.fromClass(attribute.getJavaType());
        } catch (NotFoundException e) {
            return null;
        }
    }

    String findDbColumnName(Attribute<?, ?> attribute, Class<?> entityClass, Field field, Member member) {
        Column columnAnnotation = AnnotationUtils.getAnnotation(field, Column.class);
        if (columnAnnotation != null) {
            return columnAnnotation.name();
        }
        JoinColumn joinColumnAnnotation = AnnotationUtils.getAnnotation(field, JoinColumn.class);
        if (joinColumnAnnotation != null) {
            return joinColumnAnnotation.name();
        }
        throw new RuntimeException("no column annotation found");
    }

    void processAssociations(MetaModelBuilder builder) throws NotFoundException {
        Map<String, EntityType<?>> entityMap = entityManagerFactory
                .getMetamodel().getEntities().stream()
                .collect(Collectors.toMap(EntityType::getName, Function.identity()));

        for (EntityBuilder entity : builder.entities()) {
            EntityType<?> entityType = entityMap.get(entity.getName());

            Map<String, Attribute<?, ?>> attributeMap = entityType.getAttributes().stream()
                    .collect(Collectors.toMap(a -> {
                        Member member = a.getJavaMember();
                        Field field = findReflectionField(a, entityType.getJavaType());
                        return findOutputName(field, member);
                    }, Function.identity()));

            for (FieldBuilder fieldBuilder : entity.fields()) {
                Attribute<?, ?> attribute = attributeMap.get(fieldBuilder.getOutputPropertyName());
                if (!attribute.isAssociation()) {
                    continue;
                }
                Member member = attribute.getJavaMember();
                Field field = findReflectionField(attribute, entityType.getJavaType());

                String outputName = findOutputName(field, member);

                Class<?> fieldType = field.getType();
                if (attribute.isCollection()) {
                    fieldType = resolveGenericFieldType(field);
                }

                EntityType<?> associatedEntityType = entityManagerFactory.getMetamodel().entity(fieldType);
                if (!builder.hasEntity(associatedEntityType.getName())) {
                    throw new RuntimeException(String.format("association not found %s.%s for %s", entityType.getName(), member.getName(), associatedEntityType.getName()));
                }

                boolean isOneToOne = AnnotationUtils.getAnnotation(field, OneToOne.class) != null;
                boolean isOneToMany = AnnotationUtils.getAnnotation(field, OneToMany.class) != null;
                boolean isManyToOne = AnnotationUtils.getAnnotation(field, ManyToOne.class) != null;
                boolean isManyToMany = AnnotationUtils.getAnnotation(field, ManyToMany.class) != null;

                if (isManyToOne || isOneToOne) {
                    EntityBuilder associatedEntity = builder.ref(associatedEntityType.getName());
                    KeyFieldBuilder keyFieldBuilder = associatedEntity.keyFields().iterator().next();
                    fieldBuilder.associateWith(keyFieldBuilder);
                } else if (isOneToMany) {
                    String mappedBy = Objects.requireNonNull(AnnotationUtils.getAnnotation(field, OneToMany.class)).mappedBy();
                    EntityBuilder associatedEntity = builder.ref(associatedEntityType.getName());
                    FieldBuilder associatedField = associatedEntity.ref(mappedBy);
                    fieldBuilder.associateWith(associatedField);
                } else {
                    // many to many
                    throw new RuntimeException("many to many not supported");
                }
            }
        }
    }
}
