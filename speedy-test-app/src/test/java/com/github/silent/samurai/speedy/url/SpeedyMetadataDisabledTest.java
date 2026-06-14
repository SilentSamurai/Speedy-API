package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.handlers.MetadataHandler;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests that the MetadataHandler throws 404 when
/// ISpeedyConfiguration.isMetadataEndpointEnabled() returns false.
class SpeedyMetadataDisabledTest {

    @Test
    @DisplayName("MetadataHandler throws 404 when isMetadataEndpointEnabled returns false")
    void metadataDisabled_throwsNotFound() {
        ISpeedyConfiguration config = mock(ISpeedyConfiguration.class);
        when(config.isMetadataEndpointEnabled()).thenReturn(false);

        SpeedyContext context = new SpeedyContext();
        context.put(ISpeedyConfiguration.class, config);

        MetadataHandler handler = new MetadataHandler();
        assertThrows(NotFoundException.class, () -> handler.process(context));
    }
}
