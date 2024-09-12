package com.github.silent.samurai.speedy.file.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
class FileMetaModelProcessorTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void getAllEntityMetadata() throws IOException {
        FileMetaModelProcessor fileMetaModelProcessor = new FileMetaModelProcessor("metamodel.json");
    }
}