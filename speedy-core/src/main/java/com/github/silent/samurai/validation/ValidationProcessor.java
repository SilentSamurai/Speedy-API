package com.github.silent.samurai.validation;

import com.github.silent.samurai.annotations.SpeedyValidator;
import com.github.silent.samurai.enums.SpeedyRequestType;
import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.interfaces.MetaModelProcessor;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ValidationProcessor {

    private final ISpeedyCustomValidation validationInstance;
    private final MetaModelProcessor metaModelProcessor;
    private final Map<String, Method> createValidationMethods = new HashMap<>();
    private final Map<String, Method> updateValidationMethods = new HashMap<>();
    private final Map<String, Method> deleteValidationMethods = new HashMap<>();
    private final Validator validator;

    public ValidationProcessor(ISpeedyCustomValidation validationInstance, MetaModelProcessor metaModelProcessor) {
        this.validationInstance = validationInstance;
        this.metaModelProcessor = metaModelProcessor;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private void captureValidators() {
        Class<? extends ISpeedyCustomValidation> instance = this.validationInstance.getClass();
        for (Method declaredMethod : instance.getDeclaredMethods()) {
            if (declaredMethod.isAnnotationPresent(SpeedyValidator.class)) {
                SpeedyValidator annotation = declaredMethod.getAnnotation(SpeedyValidator.class);
                if (Arrays.stream(annotation.requests()).anyMatch(speedyRequestType -> speedyRequestType == SpeedyRequestType.CREATE)) {
                    Class<?> entityClass = annotation.value();
                    EntityMetadata entityMetadata = this.metaModelProcessor.findEntityMetadata(entityClass.getSimpleName());
                    createValidationMethods.put(entityMetadata.getName(), declaredMethod);
                }
                if (Arrays.stream(annotation.requests()).anyMatch(speedyRequestType -> speedyRequestType == SpeedyRequestType.UPDATE)) {
                    Class<?> entityClass = annotation.value();
                    EntityMetadata entityMetadata = this.metaModelProcessor.findEntityMetadata(entityClass.getSimpleName());
                    updateValidationMethods.put(entityMetadata.getName(), declaredMethod);
                }
                if (Arrays.stream(annotation.requests()).anyMatch(speedyRequestType -> speedyRequestType == SpeedyRequestType.DELETE)) {
                    Class<?> entityClass = annotation.value();
                    EntityMetadata entityMetadata = this.metaModelProcessor.findEntityMetadata(entityClass.getSimpleName());
                    deleteValidationMethods.put(entityMetadata.getName(), declaredMethod);
                }
            }
        }
    }

    public void process() {
        if (validationInstance != null) {
            captureValidators();
        }
    }

    private void defaultValidator(Object entity) {
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

    private void invokeValidationMethod(Method method, Object entity) throws Exception {
        Object valid = method.invoke(validationInstance, entity);
        if (valid instanceof Boolean) {
            boolean validVal = (Boolean) valid;
            if (!validVal) {
                throw new BadRequestException("validation failed for " + entity);
            }
        }
    }

    public void validateCreateRequestEntity(EntityMetadata entityMetadata, Object entity) throws Exception {
        if (createValidationMethods.containsKey(entityMetadata.getName())) {
            Method method = createValidationMethods.get(entityMetadata.getName());
            invokeValidationMethod(method, entity);
        } else {
            defaultValidator(entity);
        }
    }

    public void validateUpdateRequestEntity(EntityMetadata entityMetadata, Object entity) throws Exception {
        if (updateValidationMethods.containsKey(entityMetadata.getName())) {
            Method method = updateValidationMethods.get(entityMetadata.getName());
            invokeValidationMethod(method, entity);
        } else {
            defaultValidator(entity);
        }
    }

    public void validateDeleteRequestEntity(EntityMetadata entityMetadata, Object entity) throws Exception {
        if (deleteValidationMethods.containsKey(entityMetadata.getName())) {
            Method method = deleteValidationMethods.get(entityMetadata.getName());
            invokeValidationMethod(method, entity);
        } else {
            defaultValidator(entity);
        }
    }
}