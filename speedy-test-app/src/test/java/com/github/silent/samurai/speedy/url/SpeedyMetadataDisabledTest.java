package com.github.silent.samurai.speedy.url;

import com.github.silent.samurai.speedy.SpeedyFactory;
import com.github.silent.samurai.speedy.controllers.SpeedyApiController;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests that the $metadata endpoint returns 404 when ISpeedyConfiguration.isMetadataEndpointEnabled() returns false.
///
/// ## What we test
/// SpeedyApiController.metadata() checks `speedyFactory.getConfiguration().isMetadataEndpointEnabled()`
/// and throws `ResponseStatusException(HttpStatus.NOT_FOUND)` when the flag is false.
/// This is the code path in SpeedyApiController:85-87 that gate-keeps the metadata endpoint.
///
/// ## How we test
/// We construct a SpeedyApiController manually, inject a mocked SpeedyFactory
/// whose configuration returns `false` for `isMetadataEndpointEnabled()`, then call
/// `controller.metadata(response)` directly and assert `ResponseStatusException` with 404.
class SpeedyMetadataDisabledTest {

    @Test
    @DisplayName("metadata endpoint throws 404 when isMetadataEndpointEnabled returns false")
    void metadataDisabled_throwsNotFound() {
        ISpeedyConfiguration config = mock(ISpeedyConfiguration.class);
        when(config.isMetadataEndpointEnabled()).thenReturn(false);

        SpeedyFactory factory = mock(SpeedyFactory.class);
        when(factory.getConfiguration()).thenReturn(config);

        SpeedyApiController controller = new SpeedyApiController();
        ReflectionTestUtils.setField(controller, "speedyFactory", factory);

        HttpServletResponse response = mock(HttpServletResponse.class);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.metadata(response));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
