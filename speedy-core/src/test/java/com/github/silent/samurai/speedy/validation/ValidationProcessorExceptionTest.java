package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.annotations.SpeedyValidator;
import com.github.silent.samurai.speedy.conversion.walker.java.JavaToSpeedy;
import com.github.silent.samurai.speedy.conversion.walker.java.SpeedyToJava;
import com.github.silent.samurai.speedy.enums.SpeedyValidationRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidationProcessorExceptionTest {

    @Test
    void preservesDeclaredHttpExceptionsFromCustomValidators() throws Exception {
        BadRequestException expected = new BadRequestException("domain rule failed");
        EntityMetadata metadata = metadata();
        ValidationProcessor processor = processorFor(new DeclaredHttpFailureValidator(expected), metadata);

        SpeedyHttpException exception = assertThrows(SpeedyHttpException.class,
                () -> processor.validateCreateRequestEntity(metadata, new SpeedyEntity(metadata)));

        assertSame(expected, exception);
    }

    @Test
    void preservesHttpRuntimeStatusFromCustomValidators() throws Exception {
        SpeedyHttpRuntimeException expected = new SpeedyHttpRuntimeException(422, "unprocessable");
        EntityMetadata metadata = metadata();
        ValidationProcessor processor = processorFor(new RuntimeFailureValidator(expected), metadata);

        SpeedyHttpRuntimeException exception = assertThrows(SpeedyHttpRuntimeException.class,
                () -> processor.validateCreateRequestEntity(metadata, new SpeedyEntity(metadata)));

        assertSame(expected, exception);
        assertEquals(422, exception.getStatus());
    }

    @Test
    void wrapsUnexpectedValidatorExceptionsAsServerErrors() throws Exception {
        EntityMetadata metadata = metadata();
        ValidationProcessor processor = processorFor(new UnexpectedFailureValidator(), metadata);

        SpeedyHttpRuntimeException exception = assertThrows(SpeedyHttpRuntimeException.class,
                () -> processor.validateCreateRequestEntity(metadata, new SpeedyEntity(metadata)));

        assertEquals(500, exception.getStatus());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    private static ValidationProcessor processorFor(ISpeedyCustomValidation validator,
                                                    EntityMetadata metadata) throws Exception {
        MetaModel metaModel = mock(MetaModel.class);
        when(metaModel.findEntityMetadata("ValidationTarget")).thenReturn(metadata);

        ValidationProcessor processor = new ValidationProcessor(List.of(validator), metaModel,
                mock(SpeedyToJava.class), mock(JavaToSpeedy.class), mock(ISpeedyConfiguration.class));
        processor.process();
        return processor;
    }

    private static EntityMetadata metadata() {
        EntityMetadata metadata = mock(EntityMetadata.class);
        when(metadata.getName()).thenReturn("ValidationTarget");
        return metadata;
    }

    public static class DeclaredHttpFailureValidator implements ISpeedyCustomValidation {
        private final BadRequestException failure;

        public DeclaredHttpFailureValidator(BadRequestException failure) {
            this.failure = failure;
        }

        @SpeedyValidator(entity = "ValidationTarget", requests = SpeedyValidationRequestType.CREATE,
                replacesDefault = true)
        public boolean validate(SpeedyEntity entity) throws SpeedyHttpException {
            throw failure;
        }
    }

    public static class RuntimeFailureValidator implements ISpeedyCustomValidation {
        private final SpeedyHttpRuntimeException failure;

        public RuntimeFailureValidator(SpeedyHttpRuntimeException failure) {
            this.failure = failure;
        }

        @SpeedyValidator(entity = "ValidationTarget", requests = SpeedyValidationRequestType.CREATE,
                replacesDefault = true)
        public boolean validate(SpeedyEntity entity) {
            throw failure;
        }
    }

    public static class UnexpectedFailureValidator implements ISpeedyCustomValidation {

        @SpeedyValidator(entity = "ValidationTarget", requests = SpeedyValidationRequestType.CREATE,
                replacesDefault = true)
        public boolean validate(SpeedyEntity entity) {
            throw new IllegalStateException("validator bug");
        }
    }
}
