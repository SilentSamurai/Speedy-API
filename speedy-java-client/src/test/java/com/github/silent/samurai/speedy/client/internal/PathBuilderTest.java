package com.github.silent.samurai.speedy.client.internal;

import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathBuilderTest {

    private final PathBuilder paths = new PathBuilder("http://localhost:8080", "/speedy/v1/");

    @Test
    void entityPathShouldConstructCorrectly() {
        assertEquals("http://localhost:8080/speedy/v1/User", paths.entityPath("User"));
    }

    @Test
    void createPathShouldAppendSuffix() {
        assertEquals("http://localhost:8080/speedy/v1/User/" + SpeedyEndpoint.CREATE.suffix(), paths.createPath("User"));
    }

    @Test
    void updatePathShouldAppendSuffix() {
        assertEquals("http://localhost:8080/speedy/v1/User/" + SpeedyEndpoint.UPDATE.suffix(), paths.updatePath("User"));
    }

    @Test
    void deletePathShouldAppendSuffix() {
        assertEquals("http://localhost:8080/speedy/v1/User/" + SpeedyEndpoint.DELETE.suffix(), paths.deletePath("User"));
    }

    @Test
    void queryPathShouldAppendSuffix() {
        assertEquals("http://localhost:8080/speedy/v1/User/" + SpeedyEndpoint.QUERY.suffix(), paths.queryPath("User"));
    }

    @Test
    void countPathShouldAppendSuffix() {
        assertEquals("http://localhost:8080/speedy/v1/User/$count", paths.countPath("User"));
    }

    @Test
    void metadataPathShouldNotIncludeEntity() {
        assertEquals("http://localhost:8080/speedy/v1" + SpeedyEndpoint.METADATA.path(), paths.metadataPath());
    }

    @Test
    void baseUrlTrailingSlashShouldBeHandled() {
        PathBuilder p = new PathBuilder("http://localhost:8080/", "/speedy/v1/");
        assertEquals("http://localhost:8080/speedy/v1/User/" + SpeedyEndpoint.CREATE.suffix(), p.createPath("User"));
    }

    @Test
    void apiPathWithoutLeadingSlashShouldWork() {
        PathBuilder p = new PathBuilder("http://localhost:8080", "speedy/v1");
        assertEquals("http://localhost:8080/speedy/v1/User", p.entityPath("User"));
    }
}
