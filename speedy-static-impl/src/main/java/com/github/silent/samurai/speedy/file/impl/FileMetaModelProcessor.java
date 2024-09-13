package com.github.silent.samurai.speedy.file.impl;

import ch.qos.logback.core.db.dialect.SQLiteDialect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.file.impl.metadata.FileEntityMetadata;
import com.github.silent.samurai.speedy.file.impl.processor.FileProcessor;
import com.github.silent.samurai.speedy.file.impl.query.QueryProcessorImpl;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.jooq.SQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.ResourceUtils;


import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileMetaModelProcessor implements MetaModelProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileMetaModelProcessor.class);

    @Value("${speedy.metamodel.file}")
    private String metaModelFile;

    private DataSource dataSource;
    private SQLDialect dialect;

    private final Map<String, FileEntityMetadata> entityMap = new HashMap<>();

    public FileMetaModelProcessor(String fileName, DataSource dataSource, SQLDialect dialect) {
        this.metaModelFile = fileName;
        this.dataSource = dataSource;
        this.dialect = dialect;
        try {
            File file = ResourceUtils.getFile("classpath:" + metaModelFile);
            try (InputStream in = new FileInputStream(file)) {
                FileProcessor.process(in, entityMap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Collection<EntityMetadata> getAllEntityMetadata() {
        return entityMap.values().stream().map(em -> (EntityMetadata) em).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean hasEntityMetadata(Class<?> entityType) {
        return false;
    }

    @Override
    public EntityMetadata findEntityMetadata(Class<?> entityType) throws NotFoundException {
        return null;
    }

    @Override
    public boolean hasEntityMetadata(String entityName) {
        return entityMap.containsKey(entityName);
    }

    @Override
    public EntityMetadata findEntityMetadata(String entityName) throws NotFoundException {
        if (!entityMap.containsKey(entityName)) {
            throw new NotFoundException(entityName);
        }
        return entityMap.get(entityName);
    }

    @Override
    public FieldMetadata findFieldMetadata(String entityName, String fieldName) throws NotFoundException {
        EntityMetadata entityMetadata = findEntityMetadata(entityName);
        return entityMetadata.field(fieldName);
    }

    @Override
    public QueryProcessor getQueryProcessor() {
        return new QueryProcessorImpl(dataSource, dialect);
    }

    @Override
    public boolean closeQueryProcessor(QueryProcessor queryProcessor) {
        return false;
    }
}