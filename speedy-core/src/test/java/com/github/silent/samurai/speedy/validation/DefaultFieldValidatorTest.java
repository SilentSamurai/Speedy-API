package com.github.silent.samurai.speedy.validation;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.metadata.EntityBuilder;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultFieldValidatorTest {

    private final DefaultFieldValidator validator = new DefaultFieldValidator();

    @Test
    void validateCreateThrowsWhenRequiredFieldIsMissing() throws Exception {
        EntityMetadata metadata = requiredNameMetadata();
        SpeedyEntity entity = new SpeedyEntity(metadata);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> validator.validateCreate(metadata, entity));

        assertEquals(400, exception.getStatus());
        assertTrue(exception.getMessage().contains("name is required"));
    }

    @Test
    void validateCreatePassesWhenRequiredFieldIsPopulated() throws Exception {
        EntityMetadata metadata = requiredNameMetadata();
        SpeedyEntity entity = new SpeedyEntity(metadata);
        entity.put("name", new SpeedyText("Speedy"));

        assertDoesNotThrow(() -> validator.validateCreate(metadata, entity));
    }

    private static EntityMetadata requiredNameMetadata() throws Exception {
        EntityBuilder entity = MetadataBuilder.builder().entity("ValidationTarget");
        entity.field("name", "NAME", ColumnType.VARCHAR)
                .nullable(false)
                .required(true);
        return entity.build();
    }
}
