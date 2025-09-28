package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.annotations.SpeedyValidator;
import com.github.silent.samurai.speedy.enums.SpeedyValidationRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.mappings.SpeedyDeserializer;
import com.github.silent.samurai.speedy.mappings.SpeedySerializer;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationProcessor {


    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationProcessor.class);

    private final List<ISpeedyCustomValidation> validationList;
    private final MetaModel metaModel;
    private final Map<String, Pair<? extends ISpeedyCustomValidation, Method>> createValidationMethods = new HashMap<>();
    private final Map<String, Pair<? extends ISpeedyCustomValidation, Method>> updateValidationMethods = new HashMap<>();
    private final Map<String, Pair<? extends ISpeedyCustomValidation, Method>> deleteValidationMethods = new HashMap<>();
    private final DefaultFieldValidator defaultFieldValidator;

    public ValidationProcessor(List<ISpeedyCustomValidation> validationList, MetaModel metaModel) {
        this.validationList = validationList;
        this.metaModel = metaModel;
        this.defaultFieldValidator = new DefaultFieldValidator();
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
                        if (Arrays.stream(annotation.requests()).anyMatch(speedyValidationRequestType -> speedyValidationRequestType == SpeedyValidationRequestType.CREATE)) {
                            createValidationMethods.put(entityMetadata.getName(), Pair.of(instance, declaredMethod));
                        }
                        if (Arrays.stream(annotation.requests()).anyMatch(speedyValidationRequestType -> speedyValidationRequestType == SpeedyValidationRequestType.UPDATE)) {
                            updateValidationMethods.put(entityMetadata.getName(), Pair.of(instance, declaredMethod));
                        }
                        if (Arrays.stream(annotation.requests()).anyMatch(speedyValidationRequestType -> speedyValidationRequestType == SpeedyValidationRequestType.DELETE)) {
                            deleteValidationMethods.put(entityMetadata.getName(), Pair.of(instance, declaredMethod));
                        }
                    }
                } catch (NotFoundException e) {
                    LOGGER.warn("Exception during instance capture ", e);
                }
            }
        }

    }

    public void process() {
        if (validationList != null && !validationList.isEmpty()) {
            captureValidators();
        }
    }

    private void invokeValidationMethod(Pair<? extends ISpeedyCustomValidation, Method> pair, SpeedyEntity entity) throws Exception {
        ISpeedyCustomValidation instance = pair.getFirst();
        Method method = pair.getSecond();

        // Determine expected parameter type (validator methods must have exactly one parameter)
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) {
            throw new IllegalArgumentException("Validator method " + method.getName() + " must have exactly one parameter");
        }

        Class<?> ioClass = paramTypes[0];
        Object param;

        // 1. If the method expects SpeedyEntity (or subclass) -> use entity directly
        // 2. Otherwise, convert the SpeedyEntity to the requested Java class
        if (SpeedyEntity.class.isAssignableFrom(ioClass)) {
            param = entity;
        } else {
            param = SpeedySerializer.toJavaEntity(entity, ioClass);
        }

        Object valid;
        try {
            valid = method.invoke(instance, param);
        } catch (InvocationTargetException ite) {
            if (ite.getCause() instanceof SpeedyHttpException she) {
                throw she;
            }
            throw ite;
        }

        // If the validator modified the Java object, synchronise the changes back to the SpeedyEntity
        if (!SpeedyEntity.class.isAssignableFrom(ioClass)) {
            SpeedyDeserializer.updateEntity(param, entity);
        }

        if (valid instanceof Boolean) {
            boolean validVal = (Boolean) valid;
            if (!validVal) {
                throw new BadRequestException("validation failed for " + entity);
            }
        }
    }

    public void validateCreateRequestEntity(EntityMetadata entityMetadata, SpeedyEntity entity) throws Exception {
        if (createValidationMethods.containsKey(entityMetadata.getName())) {
            Pair<? extends ISpeedyCustomValidation, Method> pair = createValidationMethods.get(entityMetadata.getName());
            invokeValidationMethod(pair, entity);
        } else {
            defaultFieldValidator.validateCreate(entityMetadata, entity);
        }
    }

    public void validateUpdateRequestEntity(EntityMetadata entityMetadata, SpeedyEntity entity) throws Exception {
        if (updateValidationMethods.containsKey(entityMetadata.getName())) {
            Pair<? extends ISpeedyCustomValidation, Method> pair = updateValidationMethods.get(entityMetadata.getName());
            invokeValidationMethod(pair, entity);
        } else {
            // For PATCH/UPDATE only validate supplied fields, required check not enforced
            defaultFieldValidator.validateUpdate(entityMetadata, entity);
        }
    }

    public void validateDeleteRequestEntity(EntityMetadata entityMetadata, SpeedyEntityKey entityKey) throws Exception {
        if (deleteValidationMethods.containsKey(entityMetadata.getName())) {
            Pair<? extends ISpeedyCustomValidation, Method> pair = deleteValidationMethods.get(entityMetadata.getName());
            invokeValidationMethod(pair, entityKey);
        } else {
            // For delete requests, only validate the entity key fields
            defaultFieldValidator.validateEntityKey(entityMetadata, entityKey);
        }
    }
}
