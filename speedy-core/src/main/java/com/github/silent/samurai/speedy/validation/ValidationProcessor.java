package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.annotations.SpeedyValidator;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Method;
import java.util.*;

public class ValidationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationProcessor.class);

    private final List<ISpeedyCustomValidation> validationList;
    private final MetaModelProcessor metaModelProcessor;
    private final Map<String, Pair<? extends ISpeedyCustomValidation, Method>> createValidationMethods = new HashMap<>();
    private final Map<String, Pair<? extends ISpeedyCustomValidation, Method>> updateValidationMethods = new HashMap<>();
    private final Map<String, Pair<? extends ISpeedyCustomValidation, Method>> deleteValidationMethods = new HashMap<>();
    private final Validator validator;

    public ValidationProcessor(List<ISpeedyCustomValidation> validationList, MetaModelProcessor metaModelProcessor) {
        this.validationList = validationList;
        this.metaModelProcessor = metaModelProcessor;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private void captureValidators() {
        for (ISpeedyCustomValidation instance : validationList) {
            Class<? extends ISpeedyCustomValidation> validationClass = instance.getClass();
            for (Method declaredMethod : validationClass.getDeclaredMethods()) {
                try {
                    if (declaredMethod.isAnnotationPresent(SpeedyValidator.class)) {
                        SpeedyValidator annotation = declaredMethod.getAnnotation(SpeedyValidator.class);
                        String entityName = annotation.entity();
                        EntityMetadata entityMetadata = this.metaModelProcessor.findEntityMetadata(entityName);
                        if (Arrays.stream(annotation.requests()).anyMatch(speedyRequestType -> speedyRequestType == SpeedyRequestType.CREATE)) {
                            createValidationMethods.put(entityMetadata.getName(), Pair.of(instance, declaredMethod));
                        }
                        if (Arrays.stream(annotation.requests()).anyMatch(speedyRequestType -> speedyRequestType == SpeedyRequestType.UPDATE)) {
                            updateValidationMethods.put(entityMetadata.getName(), Pair.of(instance, declaredMethod));
                        }
                        if (Arrays.stream(annotation.requests()).anyMatch(speedyRequestType -> speedyRequestType == SpeedyRequestType.DELETE)) {
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

    private void defaultValidator(SpeedyEntity entity) throws BadRequestException {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(entity);
        if (!constraintViolations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<Object> violation : constraintViolations) {
                String violationMessage = String.format("%s %s", violation.getPropertyPath(), violation.getMessage());
                sb.append(violationMessage).append(" | ");
            }
            throw new BadRequestException(sb.toString());
        }
    }

    private void invokeValidationMethod(Pair<? extends ISpeedyCustomValidation, Method> pair, SpeedyEntity entity) throws Exception {
        ISpeedyCustomValidation instance = pair.getFirst();
        Method method = pair.getSecond();
        Object valid = method.invoke(instance, entity);
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
            defaultValidator(entity);
        }
    }

    public void validateUpdateRequestEntity(EntityMetadata entityMetadata, SpeedyEntity entity) throws Exception {
        if (updateValidationMethods.containsKey(entityMetadata.getName())) {
            Pair<? extends ISpeedyCustomValidation, Method> pair = updateValidationMethods.get(entityMetadata.getName());
            invokeValidationMethod(pair, entity);
        } else {
            defaultValidator(entity);
        }
    }

    public void validateDeleteRequestEntity(EntityMetadata entityMetadata, SpeedyEntityKey entityKey) throws Exception {
        if (deleteValidationMethods.containsKey(entityMetadata.getName())) {
            Pair<? extends ISpeedyCustomValidation, Method> pair = deleteValidationMethods.get(entityMetadata.getName());
            invokeValidationMethod(pair, entityKey);
        } else {
            defaultValidator(entityKey);
        }
    }
}
