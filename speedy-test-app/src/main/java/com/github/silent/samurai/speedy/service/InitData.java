package com.github.silent.samurai.speedy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Service
public class InitData {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitData.class);

    @Autowired
    DataSource dataSource;

    @Autowired
    CategoryRepository categoryRepository;

    public static String[] fetchSql() throws IOException {
        File file = ResourceUtils.getFile("classpath:x-data.sql");
        try (InputStream in = new FileInputStream(file)) {
            String content = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            return content.split(";");
        }
    }

    @PostConstruct
    public void initData() {
        try (Connection connection = dataSource.getConnection()) {
            String[] sqls = fetchSql();
            for (String sql : sqls) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.executeUpdate();
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }
}
