package com.github.silent.samurai.speedy.file.impl;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.file.impl.processor.FileProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModelProcessor;
import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileMetaModelProcessor implements MetaModelProcessor {

    private final String metaModelFile;

    private MetaModel metaModel;

    public FileMetaModelProcessor(String fileName) {
        this.metaModelFile = fileName;
    }

    @Override
    public MetaModel getMetaModel() {
        return metaModel;
    }

    @Override
    public void processMetaModel(MetaModelBuilder builder) {
        try (InputStream in = openResource()) {
            FileProcessor.process(in, builder);
            metaModel = builder.build();
        } catch (IOException | NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream openResource() throws FileNotFoundException {
        String resourceName = metaModelFile.startsWith("/") ? metaModelFile.substring(1) : metaModelFile;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = FileMetaModelProcessor.class.getClassLoader();
        }
        InputStream inputStream = classLoader.getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new FileNotFoundException("Classpath resource not found: " + resourceName);
        }
        return inputStream;
    }
}
