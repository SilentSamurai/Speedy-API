package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyCreateBody;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestValidationHandlerTest {

    @Test
    void createValidation_validatesEveryEntity() throws Exception {
        ValidationProcessor validationProcessor = mock(ValidationProcessor.class);
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        SpeedyEntity first = mock(SpeedyEntity.class);
        SpeedyEntity second = mock(SpeedyEntity.class);
        SpeedyCreateBody body = SpeedyCreateBody.builder()
                .entities(List.of(first, second))
                .build();

        new RequestValidationHandler(SpeedyRequestType.CREATE)
                .process(writeContext(validationProcessor, entityMetadata).put(SpeedyBody.class, body));

        verify(validationProcessor).validateCreateRequestEntity(entityMetadata, first);
        verify(validationProcessor).validateCreateRequestEntity(entityMetadata, second);
    }

    @Test
    void updateAndReplaceValidation_useTheirRespectiveValidators() throws Exception {
        ValidationProcessor validationProcessor = mock(ValidationProcessor.class);
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        SpeedyEntity entity = mock(SpeedyEntity.class);
        SpeedyUpdateBody body = SpeedyUpdateBody.builder()
                .items(List.of(SpeedyUpdateBody.Item.builder()
                        .entity(entity)
                        .pk(mock(SpeedyEntityKey.class))
                        .build()))
                .build();

        new RequestValidationHandler(SpeedyRequestType.UPDATE)
                .process(writeContext(validationProcessor, entityMetadata).put(SpeedyBody.class, body));
        new RequestValidationHandler(SpeedyRequestType.REPLACE)
                .process(writeContext(validationProcessor, entityMetadata).put(SpeedyBody.class, body));

        verify(validationProcessor).validateUpdateRequestEntity(entityMetadata, entity);
        verify(validationProcessor).validateReplaceRequestEntity(entityMetadata, entity);
    }

    @Test
    void deleteValidation_validatesEveryKey() throws Exception {
        ValidationProcessor validationProcessor = mock(ValidationProcessor.class);
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        SpeedyEntityKey first = mock(SpeedyEntityKey.class);
        SpeedyEntityKey second = mock(SpeedyEntityKey.class);
        SpeedyDeleteBody body = SpeedyDeleteBody.builder()
                .keys(List.of(first, second))
                .build();

        new RequestValidationHandler(SpeedyRequestType.DELETE)
                .process(writeContext(validationProcessor, entityMetadata).put(SpeedyBody.class, body));

        verify(validationProcessor).validateDeleteRequestEntity(entityMetadata, first);
        verify(validationProcessor).validateDeleteRequestEntity(entityMetadata, second);
    }

    @Test
    void queryValidation_supportsUriAndBodyQueries() throws Exception {
        ValidationProcessor validationProcessor = mock(ValidationProcessor.class);
        SpeedyQueryImpl uriQuery = mock(SpeedyQueryImpl.class);
        SpeedyQuery bodyQuery = mock(SpeedyQuery.class);
        SpeedyUriContext uriContext = mock(SpeedyUriContext.class);
        when(uriContext.getParsedQuery()).thenReturn(uriQuery);

        new RequestValidationHandler(SpeedyRequestType.GET_LIST)
                .process(new SpeedyContext()
                        .put(ValidationProcessor.class, validationProcessor)
                        .put(SpeedyUriContext.class, uriContext));
        new RequestValidationHandler(SpeedyRequestType.QUERY)
                .process(new SpeedyContext()
                        .put(ValidationProcessor.class, validationProcessor)
                        .put(SpeedyBody.class, bodyQuery));

        verify(validationProcessor).validateQueryRequest(uriQuery);
        verify(validationProcessor).validateQueryRequest(bodyQuery);
    }

    @Test
    void metadataValidation_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RequestValidationHandler(SpeedyRequestType.METADATA));
    }

    private static SpeedyContext writeContext(ValidationProcessor validationProcessor,
                                              EntityMetadata entityMetadata) {
        SpeedyQueryImpl query = mock(SpeedyQueryImpl.class);
        when(query.getFrom()).thenReturn(entityMetadata);
        SpeedyUriContext uriContext = mock(SpeedyUriContext.class);
        when(uriContext.getParsedQuery()).thenReturn(query);
        return new SpeedyContext()
                .put(ValidationProcessor.class, validationProcessor)
                .put(SpeedyUriContext.class, uriContext);
    }
}
