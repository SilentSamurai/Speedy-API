package com.github.silent.samurai.speedy.jpa.impl.processors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.silent.samurai.speedy.annotations.*;
import com.github.silent.samurai.speedy.annotations.validation.*;
import com.github.silent.samurai.speedy.enums.*;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModelProcessor;
import com.github.silent.samurai.speedy.jpa.impl.util.JavaType2ColumnType;
import com.github.silent.samurai.speedy.metadata.AssociationColumnRef;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.FieldBuilder;
import com.github.silent.samurai.speedy.metadata.KeyFieldBuilder;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
import com.github.silent.samurai.speedy.models.DynamicEnum;
import com.github.silent.samurai.speedy.validation.rules.*;
import jakarta.persistence.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
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

// Jakarta Bean Validation annotations

public class JpaMetaModelProcessorV2 implements MetaModelProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaMetaModelProcessorV2.class);
    private final Map<Class<?>, EntityType<?>> typeMap = new HashMap<>();
    private MetaModel metaModel;
    private final ISpeedyConfiguration configuration;
    private final EntityManagerFactory entityManagerFactory;

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
        SpeedyBulk bulkAnnotation = entityType.getBindableJavaType().getAnnotation(SpeedyBulk.class);
        if (bulkAnnotation != null) {
            Arrays.stream(bulkAnnotation.value())
                    .forEach(entity::addBulkOperation);
        }
        // Entity-level @SpeedySensitive sets the default sensitivity for all fields
        SpeedySensitive speedySensitive = entityType.getBindableJavaType().getAnnotation(SpeedySensitive.class);
        if (speedySensitive != null) {
            entity.sensitive(speedySensitive.value());
        }
        for (Attribute<?, ?> attribute : attributesInDeclarationOrder(entityType)) {
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

    /// {@link EntityType#getAttributes()} is backed by a {@code Set} with no ordering guarantee, so
    /// relying on it directly leaves field order — and, downstream, the order OASGenerator emits
    /// composite-key parameters (which becomes the generated Java client's *positional* method
    /// arguments) — to whatever a hash-bucket layout happens to produce. Re-sort by each attribute's
    /// position in the entity's own declared-field order (walking superclasses first) so it matches
    /// the order a reader of the entity source would expect, and stays stable across regenerations.
    private static List<Attribute<?, ?>> attributesInDeclarationOrder(EntityType<?> entityType) {
        Deque<Class<?>> hierarchy = new ArrayDeque<>();
        for (Class<?> c = entityType.getJavaType(); c != null && c != Object.class; c = c.getSuperclass()) {
            hierarchy.addFirst(c);
        }
        Map<String, Integer> declarationIndex = new HashMap<>();
        for (Class<?> klass : hierarchy) {
            for (Field field : klass.getDeclaredFields()) {
                declarationIndex.putIfAbsent(field.getName(), declarationIndex.size());
            }
        }
        return entityType.getAttributes().stream()
                .sorted(Comparator.comparingInt(a ->
                        declarationIndex.getOrDefault(a.getJavaMember().getName(), Integer.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    boolean isIgnorable(Attribute<?, ?> attribute, Class<?> entityClass) {
        Field field = findReflectionField(attribute, entityClass);
        if (AnnotationUtils.getAnnotation(field, SpeedyIgnore.class) != null) {
            return true;
        }
        if (attribute.getJavaType().getAnnotation(SpeedyIgnore.class) != null) {
            return true;
        }
        if (attribute.isAssociation() && AnnotationUtils.getAnnotation(field, OneToMany.class) != null) {
            LOGGER.warn("Skipping @OneToMany field '{}' on entity '{}' — one-to-many associations are not supported by Speedy-API", field.getName(), entityClass.getSimpleName());
            return true;
        }
        if (attribute.isAssociation() && AnnotationUtils.getAnnotation(field, ManyToMany.class) != null) {
            LOGGER.warn("Skipping @ManyToMany field '{}' on entity '{}' — many-to-many associations are not supported by Speedy-API", field.getName(), entityClass.getSimpleName());
            return true;
        }
        return false;
    }

    private FieldBuilder processField(Attribute<?, ?> attribute,
                                      Class<?> entityClass,
                                      EntityBuilder entity) {

        boolean isId = attribute instanceof SingularAttribute && ((SingularAttribute<?, ?>) attribute).isId();
        boolean isJpaVersion = attribute instanceof SingularAttribute && ((SingularAttribute<?, ?>) attribute).isVersion();
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

        // A multi-column foreign key spreads its @JoinColumns over several columns, each of which
        // may carry its own flags, but Speedy reads and writes the whole key as one unit — so the
        // flags are combined rather than taken from an arbitrary one of them. Writable only if
        // every column is; null only when every column is (the same rule $isnull applies); unique
        // as soon as any single column is, since that alone pins the association down.
        // A lone @JoinColumn — every association that isn't composite — reduces to its own flags.
        List<JoinColumn> joinColumns = findJoinColumns(field);
        if (!joinColumns.isEmpty()) {
            boolean insertable = true;
            boolean updatable = true;
            boolean nullable = true;
            boolean unique = false;
            for (JoinColumn joinColumn : joinColumns) {
                insertable &= joinColumn.insertable();
                updatable &= joinColumn.updatable();
                nullable &= joinColumn.nullable();
                unique |= joinColumn.unique();
            }
            fieldMetadata.insertable(insertable);
            fieldMetadata.unique(unique);
            fieldMetadata.updatable(updatable);
            fieldMetadata.nullable(nullable);
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
            if (!isId) {
                fieldMetadata.deserializable(false);
            }
        }

        Generated hibernateGenerated = AnnotationUtils.getAnnotation(field, Generated.class);
        if (hibernateGenerated != null) {
            fieldMetadata.insertable(false);
            fieldMetadata.updatable(false);
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
            if (actionTypesSet.contains(UPDATE) || actionTypesSet.contains(REPLACE)) {
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

        // Field-level @SpeedySensitive overrides entity-level default;
        // if absent, inherit the entity's sensitivity setting
        SpeedySensitive speedySensitive = AnnotationUtils.getAnnotation(field, SpeedySensitive.class);
        if (speedySensitive != null) {
            fieldMetadata.sensitive(speedySensitive.value());
        } else {
            fieldMetadata.sensitive(entity.isSensitive());
        }

        // @SpeedyETag (or a compatible JPA @Version) makes this field Speedy-managed: stamped
        // fresh on every create/update/replace, never client-supplied. Applied last so it wins
        // over whatever insertable/updatable/deserializable an earlier annotation block set.
        SpeedyETag speedyETag = AnnotationUtils.getAnnotation(field, SpeedyETag.class);
        if (speedyETag != null) {
            markEtagManaged(fieldMetadata, speedyETag.strategy());
        } else if (isJpaVersion) {
            // Speedy persists via jOOQ, bypassing Hibernate's own version-increment, so a bare
            // JPA @Version is only auto-managed here when its type fits an implemented strategy
            // (a temporal column -> TIMESTAMP). A numeric @Version is left as a plain,
            // Speedy-unmanaged field; annotate it with @SpeedyETag directly to opt in.
            ValueType candidateType = columnType != null ? columnType.getValueType() : null;
            boolean isTemporal = candidateType == ValueType.DATE || candidateType == ValueType.TIME
                    || candidateType == ValueType.DATE_TIME || candidateType == ValueType.ZONED_DATE_TIME;
            if (isTemporal) {
                markEtagManaged(fieldMetadata, EtagStrategy.TIMESTAMP);
            } else {
                LOGGER.warn("Entity field '{}' is a JPA @Version column of type {} — Speedy only " +
                                "auto-manages temporal @Version columns as an ETag; annotate it with " +
                                "@SpeedyETag directly to opt into ETag support for a numeric version.",
                        outputName, candidateType);
            }
        }

        return fieldMetadata;
    }

    private void markEtagManaged(FieldBuilder fieldMetadata, EtagStrategy strategy) {
        fieldMetadata.etagField(strategy);
        // Same shape as @Generated/@Formula: the column is computed by the server, not the
        // client, so it's excluded from the generated Create/Update request schemas
        // (SpeedyOpenApiCustomizer keys those on isInsertable/isUpdatable, not isDeserializable).
        // EtagStampHandler still writes it — SpeedyToRecord persists whatever entity.has(field)
        // finds, regardless of these flags.
        fieldMetadata.insertable(false);
        fieldMetadata.updatable(false);
        fieldMetadata.deserializable(false);
        fieldMetadata.serializable(true);
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

    /// Resolves the property name a field is exposed under. Both call sites — {@link #processField}
    /// and the attribute lookup in {@link #processAssociations} — go through here, so any rename
    /// stays consistent across the two passes.
    String findOutputName(Field field, Member member) {
        JsonProperty propertyAnnotation = AnnotationUtils.getAnnotation(field, JsonProperty.class);
        String jsonPropertyName = propertyAnnotation != null ? propertyAnnotation.value() : null;

        SpeedyAssociation manualAssociation = AnnotationUtils.getAnnotation(field, SpeedyAssociation.class);
        if (manualAssociation != null && !manualAssociation.name().isBlank()) {
            String associationName = manualAssociation.name();
            if (jsonPropertyName != null && !jsonPropertyName.equals(associationName)) {
                throw new RuntimeException(String.format(
                        "@SpeedyAssociation(name = \"%s\") on %s.%s conflicts with @JsonProperty(\"%s\") — " +
                                "a field can only be exposed under one name",
                        associationName, member.getDeclaringClass().getSimpleName(), member.getName(), jsonPropertyName));
            }
            return associationName;
        }

        if (jsonPropertyName != null) {
            return jsonPropertyName;
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
        // A multi-column foreign key (target with a composite primary key) declares its columns via
        // @JoinColumns. Any of them stands in until processAssociations builds the complete mapping
        // (which needs the target's key fields, resolvable only in that later pass) — from then on
        // FieldMetadata.getDbColumnName() reports the first *mapped* column, so which one is picked
        // here does not decide anything.
        List<JoinColumn> joinColumns = findJoinColumns(field);
        if (!joinColumns.isEmpty()) {
            return joinColumns.get(0).name();
        }
        throw new RuntimeException("no column annotation found");
    }

    /// The {@code @JoinColumn}s declared on {@code field}, whether written as a single
    /// {@code @JoinColumn} or as a plural {@code @JoinColumns}. Empty when the field declares neither.
    static List<JoinColumn> findJoinColumns(Field field) {
        JoinColumns plural = AnnotationUtils.getAnnotation(field, JoinColumns.class);
        if (plural != null && plural.value().length > 0) {
            return List.of(plural.value());
        }
        JoinColumn single = AnnotationUtils.getAnnotation(field, JoinColumn.class);
        return single == null ? List.of() : List.of(single);
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
                Member member = attribute.getJavaMember();
                Field field = findReflectionField(attribute, entityType.getJavaType());

                SpeedyAssociation manualAssociation = AnnotationUtils.getAnnotation(field, SpeedyAssociation.class);
                if (!attribute.isAssociation() && manualAssociation == null) {
                    continue;
                }

                String outputName = findOutputName(field, member);

                if (manualAssociation != null) {
                    String associatedEntityName = resolveManualAssociationEntityName(manualAssociation, entityType, member);
                    if (!builder.hasEntity(associatedEntityName)) {
                        throw new RuntimeException(String.format("association not found %s.%s for %s", entityType.getName(), member.getName(), associatedEntityName));
                    }
                    EntityBuilder associatedEntity = builder.ref(associatedEntityName);
                    fieldBuilder.associateWith(associatedEntityName,
                            resolveAssociationColumns(field, entityType, member, associatedEntity));
                    continue;
                }

                Class<?> fieldType = attribute.isCollection() ? resolveGenericFieldType(field) : field.getType();

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
                    fieldBuilder.associateWith(associatedEntityType.getName(),
                            resolveAssociationColumns(field, entityType, member, associatedEntity));
                } else if (isOneToMany) {
                    String mappedBy = Objects.requireNonNull(AnnotationUtils.getAnnotation(field, OneToMany.class)).mappedBy();
                    EntityBuilder associatedEntity = builder.ref(associatedEntityType.getName());
                    FieldBuilder associatedField = associatedEntity.ref(mappedBy);
                    fieldBuilder.associateWith(associatedField);
                } else {
                    LOGGER.warn("Skipping @ManyToMany association for field '{}' on entity '{}' — many-to-many associations are not supported by Speedy-API", field.getName(), entity.getName());
                }
            }
        }
    }

    /// Maps a to-one association onto the foreign-key columns it is stored in, one per column of the
    /// target's primary key, ordered by the target's key-field order.
    ///
    /// A target with a single-column key needs no annotation detail: whatever column the field
    /// resolved to *is* the foreign key (a null local column defers to it, so a later rename still
    /// applies). A target with a **composite** key must declare one {@code @JoinColumn} per key
    /// column inside {@code @JoinColumns}, each naming the key column it references via
    /// {@code referencedColumnName} — there is nothing else to match a local column against.
    List<AssociationColumnRef> resolveAssociationColumns(Field field,
                                                         EntityType<?> entityType,
                                                         Member member,
                                                         EntityBuilder associatedEntity) {
        List<KeyFieldBuilder> targetKeys = new ArrayList<>();
        associatedEntity.keyFields().forEach(targetKeys::add);
        if (targetKeys.isEmpty()) {
            throw new RuntimeException(String.format(
                    "association %s.%s targets %s, which declares no primary key",
                    entityType.getName(), member.getName(), associatedEntity.getName()));
        }
        if (targetKeys.size() == 1) {
            return List.of(new AssociationColumnRef(null, targetKeys.get(0).getOutputPropertyName()));
        }

        List<JoinColumn> joinColumns = findJoinColumns(field);
        if (joinColumns.size() != targetKeys.size()) {
            throw new RuntimeException(String.format(
                    "association %s.%s targets %s, whose primary key spans %d columns [%s], but declares %d " +
                            "join column(s) — annotate it with @JoinColumns({@JoinColumn(name = ..., " +
                            "referencedColumnName = ...), ...}), one per key column",
                    entityType.getName(), member.getName(), associatedEntity.getName(),
                    targetKeys.size(), targetKeyColumnNames(targetKeys), joinColumns.size()));
        }

        Map<String, JoinColumn> byReferencedColumn = new HashMap<>();
        for (JoinColumn joinColumn : joinColumns) {
            String referenced = joinColumn.referencedColumnName();
            if (referenced.isBlank()) {
                throw new RuntimeException(String.format(
                        "association %s.%s targets %s, whose primary key spans %d columns [%s], so every " +
                                "@JoinColumn must set referencedColumnName; '%s' does not",
                        entityType.getName(), member.getName(), associatedEntity.getName(),
                        targetKeys.size(), targetKeyColumnNames(targetKeys), joinColumn.name()));
            }
            byReferencedColumn.put(referenced.toUpperCase(Locale.ROOT), joinColumn);
        }

        // Ordered by the target's key fields, not by annotation order, so the pairs line up
        // positionally with the target key everywhere downstream regardless of how they were written.
        List<AssociationColumnRef> columns = new ArrayList<>(targetKeys.size());
        for (KeyFieldBuilder targetKey : targetKeys) {
            JoinColumn joinColumn = byReferencedColumn.remove(targetKey.getDbColumnName().toUpperCase(Locale.ROOT));
            if (joinColumn == null) {
                throw new RuntimeException(String.format(
                        "association %s.%s targets %s but no @JoinColumn references its key column '%s'",
                        entityType.getName(), member.getName(), associatedEntity.getName(),
                        targetKey.getDbColumnName()));
            }
            columns.add(new AssociationColumnRef(joinColumn.name(), targetKey.getOutputPropertyName()));
        }
        return columns;
    }

    private static String targetKeyColumnNames(List<KeyFieldBuilder> targetKeys) {
        return targetKeys.stream()
                .map(KeyFieldBuilder::getDbColumnName)
                .collect(Collectors.joining(", "));
    }

    /// Resolves the target entity name for a {@link SpeedyAssociation}, which accepts exactly one
    /// of {@code value()} (a JPA entity class) or {@code entity()} (a Speedy entity name directly).
    String resolveManualAssociationEntityName(SpeedyAssociation manualAssociation, EntityType<?> entityType, Member member) {
        boolean hasClass = manualAssociation.value() != Void.class;
        boolean hasEntityName = !manualAssociation.entity().isBlank();
        if (hasClass == hasEntityName) {
            throw new RuntimeException(String.format(
                    "@SpeedyAssociation on %s.%s must specify exactly one of value() or entity()",
                    entityType.getName(), member.getName()));
        }
        if (hasClass) {
            return entityManagerFactory.getMetamodel().entity(manualAssociation.value()).getName();
        }
        return manualAssociation.entity();
    }
}
