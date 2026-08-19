package com.github.silent.samurai.speedy.service;

import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.FkNullEntity;
import com.github.silent.samurai.speedy.entity.PkUuidTest;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.repositories.FkNullEntityRepository;
import com.github.silent.samurai.speedy.repositories.PkUuidTestRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class InitData {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitData.class);

    @Autowired
    DataSource dataSource;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    PkUuidTestRepository pkUuidTestRepository;

    @Autowired
    FkNullEntityRepository fkNullEntityRepository;

    public static List<String> fetchSql() throws IOException {
        File file = ResourceUtils.getFile("classpath:x-data.sql");
        try (InputStream in = new FileInputStream(file)) {
            String content = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            return Arrays.stream(content.split(";"))
                    .map(String::trim)
                    .filter(Predicate.not(String::isEmpty))
                    .filter(Predicate.not(String::isBlank))
                    .collect(Collectors.toList());
        }
    }

    @PostConstruct
    public void initData() {
        try (Connection connection = dataSource.getConnection()) {
            for (String sql : fetchSql()) {
                LOGGER.info("sql : {}", sql);
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.executeUpdate();
                } catch (Exception e) {
                    LOGGER.error("init-data error", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        seedUuidKeyedRows();
    }

    /// Rows whose primary key is a {@code java.util.UUID}, seeded through JPA rather than from
    /// x-data.sql. Hibernate maps the field to a native `uuid` column on H2 and Postgres but to
    /// `binary(16)` on MySQL and HSQLDB, and no SQL literal is accepted by all four — HSQLDB rejects
    /// the textual form with "invalid character value for cast". Binding an entity leaves the
    /// conversion to the dialect. The keys are generated, so no test may depend on a fixed one.
    private void seedUuidKeyedRows() {
        pkUuidTestRepository.saveAll(List.of(
                pkUuidTest("UUID Test 1", "Description for UUID test 1"),
                pkUuidTest("UUID Test 2", "Description for UUID test 2"),
                pkUuidTest("UUID Test 3", null)
        ));

        Category category = categoryRepository.findById("1").orElse(null);
        fkNullEntityRepository.saveAll(List.of(
                fkNullEntity("FK Null 1", null),
                fkNullEntity("FK Null 2", category),
                fkNullEntity("FK Null 3", null)
        ));
    }

    private static PkUuidTest pkUuidTest(String name, String description) {
        PkUuidTest entity = new PkUuidTest();
        entity.setName(name);
        entity.setDescription(description);
        return entity;
    }

    private static FkNullEntity fkNullEntity(String name, Category category) {
        FkNullEntity entity = new FkNullEntity();
        entity.setName(name);
        entity.setCategory(category);
        return entity;
    }
}
