package com.github.silent.samurai.speedy.file.impl;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.file.impl.metadata.FileEntityMetadata;
import com.github.silent.samurai.speedy.file.impl.processor.FileProcessor;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FileMetaModelProcessor implements MetaModelProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileMetaModelProcessor.class);

    @Value("${speedy.metamodel.file}")
    private String metaModelFile;

    private MetaModel metaModel;

    public FileMetaModelProcessor(String fileName) {
        this.metaModelFile = fileName;
    }

    @Override
    public MetaModel getMetaModel() {
        return metaModel;
    }

    @Override
    public void processMetaModel(MetadataBuilder.MetaModelBuilder builder) {
        try {
            File file = ResourceUtils.getFile("classpath:" + metaModelFile);
            try (InputStream in = new FileInputStream(file)) {
                Map<String, FileEntityMetadata> entityMap = new HashMap<>();
                FileProcessor.process(in, builder);
            } catch (IOException | NotFoundException e) {
                throw new RuntimeException(e);
            }
            metaModel = builder.build();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}