package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.annotations.SpeedyValidator;
import com.github.silent.samurai.speedy.enums.SpeedyValidationRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.conversion.walker.java.JavaToSpeedy;
import com.github.silent.samurai.speedy.conversion.walker.java.SpeedyToJava;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationProcessor {


    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationProcessor.class);

    private final List<ISpeedyCustomValidation> validationList;
    private final MetaModel metaModel;
    /// Shared serializer for converting SpeedyEntity to user POJOs before
    /// invoking custom validation methods.
    ///
    /// @see SpeedyToJava#toJavaEntity
    private final SpeedyToJava serializer;
    /// Shared deserializer for synchronizing changes from user POJOs back to
    /// SpeedyEntity after validation methods return.
    ///
    /// @see JavaToSpeedy#updateEntity
    private final JavaToSpeedy deserializer;
    private final Map<String, RegisteredValidator> createValidationMethods = new HashMap<>();
    private final Map<String, RegisteredValidator> updateValidationMethods = new HashMap<>();
    private final Map<String, RegisteredValidator> deleteValidationMethods = new HashMap<>();
    private final DefaultFieldValidator defaultFieldValidator;
    private final DefaultQueryValidator defaultQueryValidator;

    /// Holds a captured custom validation method together with the {@code replacesDefault}
    /// flag from its {@link SpeedyValidator} annotation, so the dispatcher knows whether the
    /// built-in {@link DefaultFieldValidator} checks should still run alongside it.
    private record RegisteredValidator(ISpeedyCustomValidation instance, MethodHandle handle, boolean replacesDefault) {
    }

    /// Creates the validation processor with the necessary serialization infrastructure.
    ///
    /// @param validationList list of user-registered custom validation beans
    /// @param metaModel      the global metamodel
    /// @param serializer     serializer for {@code SpeedyEntity -> POJO}
    /// @param deserializer   deserializer for {@code POJO -> SpeedyEntity}
    /// @param configuration  global configuration supplying query-complexity limits
    public ValidationProcessor(List<ISpeedyCustomValidation> validationList, MetaModel metaModel, SpeedyToJava serializer, JavaToSpeedy deserializer, ISpeedyConfiguration configuration) {
        this.validationList = validationList;
        this.metaModel = metaModel;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.defaultFieldValidator = new DefaultFieldValidator();
        this.defaultQueryValidator = new DefaultQueryValidator(
                configuration.getMaxConditionDepth(),
                configuration.getMaxFilterCount(),
                configuration.getMaxExpandCount());
    }

    private void captureValidators() {
        for (ISpeedyCustomValidation instance : validationList) {
            Class<? extends ISpeedyCustomValidation> validationClass = instance.getClass();
            for (Method declaredMethod : validationClass.getDeclaredMethods()) {
                try {
                    if (declaredMethod.isAnnotationPresent(SpeedyValidator.class)) {
                        SpeedyValidator annotation = declaredMethod.getAnnotation(SpeedyValidator.class);
                        String entityName = annotation.entity();
                        EntityMetadata entityMetadata = this.metaModel.findEntityMetadata(entityName);
                        MethodHandle methodHandle = MethodHandles.lookup().unreflect(declaredMethod);
                        RegisteredValidator registeredValidator = new RegisteredValidator(instance, methodHandle, annotation.replacesDefault());
                        if (Arrays.stream(annotation.requests()).anyMatch(speedyValidationRequestType -> speedyValidationRequestType == SpeedyValidationRequestType.CREATE)) {
                            createValidationMethods.put(entityMetadata.getName(), registeredValidator);
                        }
                        if (Arrays.stream(annotation.requests()).anyMatch(speedyValidationRequestType -> speedyValidationRequestType == SpeedyValidationRequestType.UPDATE)) {
                            updateValidationMethods.put(entityMetadata.getName(), registeredValidator);
                        }
                        if (Arrays.stream(annotation.requests()).anyMatch(speedyValidationRequestType -> speedyValidationRequestType == SpeedyValidationRequestType.DELETE)) {
                            deleteValidationMethods.put(entityMetadata.getName(), registeredValidator);
                        }
                    }
                } catch (NotFoundException | IllegalAccessException e) {
                    throw new RuntimeException("Misconfigured @SpeedyValidator: entity '" + declaredMethod.getAnnotation(SpeedyValidator.class).entity() + "' could not be registered", e);
                }
            }
        }

    }

    public void process() {
        if (validationList != null && !validationList.isEmpty()) {
            captureValidators();
        }
    }

    private void invokeValidationMethod(RegisteredValidator registeredValidator, SpeedyEntity entity) throws SpeedyHttpException {
        ISpeedyCustomValidation instance = registeredValidator.instance();
        MethodHandle methodHandle = registeredValidator.handle();

        MethodType methodType = methodHandle.type();
        Class<?>[] paramTypes = methodType.parameterArray();
        if (paramTypes.length != 2) {
            throw new IllegalArgumentException("Validator method must have exactly one parameter");
        }

        Class<?> ioClass = paramTypes[1];
        Object param;

        // 1. If the method expects SpeedyEntity (or a supertype like Object/SpeedyValue) -> use entity directly
        // 2. Otherwise, convert the SpeedyEntity to the requested Java class
        if (ioClass.isAssignableFrom(SpeedyEntity.class)) {
            param = entity;
        } else {
            param = serializer.toJavaEntity(entity, ioClass);
        }

        Object valid;
        try {
            valid = methodHandle.invoke(instance, param);
        } catch (Throwable t) {
            if (t instanceof SpeedyHttpException she) {
                throw she;
            }
            if (t instanceof SpeedyHttpRuntimeException re) {
                throw re;
            }
            if (t instanceof Exception e) {
                throw new SpeedyHttpRuntimeException(500, e);
            }
            throw new SpeedyHttpRuntimeException(500, new RuntimeException(t));
        }

        // If the validator modified the Java object, synchronise the changes back to the SpeedyEntity
        if (!ioClass.isAssignableFrom(SpeedyEntity.class)) {
            deserializer.updateEntity(param, entity);
        }

        if (valid instanceof Boolean) {
            boolean validVal = (Boolean) valid;
            if (!validVal) {
                throw new BadRequestException("validation failed for " + entity);
            }
        }
    }

    public void validateCreateRequestEntity(EntityMetadata entityMetadata, SpeedyEntity entity) throws SpeedyHttpException {
        RegisteredValidator custom = createValidationMethods.get(entityMetadata.getName());
        if (custom == null || !custom.replacesDefault()) {
            defaultFieldValidator.validateCreate(entityMetadata, entity);
        }
        if (custom != null) {
            invokeValidationMethod(custom, entity);
        }
    }

    public void validateUpdateRequestEntity(EntityMetadata entityMetadata, SpeedyEntity entity) throws SpeedyHttpException {
        RegisteredValidator custom = updateValidationMethods.get(entityMetadata.getName());
        if (custom == null || !custom.replacesDefault()) {
            // For PATCH/UPDATE only validate supplied fields, required check not enforced
            defaultFieldValidator.validateUpdate(entityMetadata, entity);
        }
        if (custom != null) {
            invokeValidationMethod(custom, entity);
        }
    }

    /// Validation for a full-replace (PUT). The payload is the complete representation, so the
    /// default path enforces required fields like create. By default the built-in checks run
    /// first and a custom {@code @SpeedyValidator} then runs on top; a validator registered for
    /// {@code UPDATE} also guards PUT (reuses {@link #updateValidationMethods}). If that validator
    /// sets {@code replacesDefault = true}, only the custom validator runs.
    public void validateReplaceRequestEntity(EntityMetadata entityMetadata, SpeedyEntity entity) throws SpeedyHttpException {
        RegisteredValidator custom = updateValidationMethods.get(entityMetadata.getName());
        if (custom == null || !custom.replacesDefault()) {
            defaultFieldValidator.validateReplace(entityMetadata, entity);
        }
        if (custom != null) {
            invokeValidationMethod(custom, entity);
        }
    }

    public void validateDeleteRequestEntity(EntityMetadata entityMetadata, SpeedyEntityKey entityKey) throws SpeedyHttpException {
        RegisteredValidator custom = deleteValidationMethods.get(entityMetadata.getName());
        if (custom == null || !custom.replacesDefault()) {
            // For delete requests, only validate the entity key fields
            defaultFieldValidator.validateEntityKey(entityMetadata, entityKey);
        }
        if (custom != null) {
            invokeValidationMethod(custom, entityKey);
        }
    }

    /**
     * Validates a read request ({@code GET} or {@code POST /$query}). Both resolve
     * to a {@link SpeedyQuery}, so they share one validator. There is no custom
     * validation hook for reads ({@code SpeedyValidationRequestType} only models
     * CREATE/UPDATE/DELETE), so this always runs the default query rules.
     */
    public void validateQueryRequest(SpeedyQuery query) throws BadRequestException {
        defaultQueryValidator.validateQuery(query);
    }
}
