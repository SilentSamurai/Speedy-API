package com.github.silent.samurai.speedy.service;

import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.FkNullEntity;
import com.github.silent.samurai.speedy.entity.PkUuidTest;
import com.github.silent.samurai.speedy.entity.ValueTestEntity;
import com.github.silent.samurai.speedy.repositories.CategoryRepository;
import com.github.silent.samurai.speedy.repositories.FkNullEntityRepository;
import com.github.silent.samurai.speedy.repositories.PkUuidTestRepository;
import com.github.silent.samurai.speedy.repositories.ValueTestRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

    @Autowired
    ValueTestRepository valueTestRepository;

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

    /// Rows x-data.sql cannot express portably, seeded through JPA instead so each dialect binds the
    /// values its own way.
    ///
    /// Two kinds. Rows whose primary key is a {@code java.util.UUID}: Hibernate maps the field to a native `uuid` column on H2 and Postgres but to
    /// `binary(16)` on MySQL and HSQLDB, and no SQL literal is accepted by all four — HSQLDB rejects
    /// the textual form with "invalid character value for cast". Binding an entity leaves the
    /// conversion to the dialect. The keys are generated, so no test may depend on a fixed one.
    private void seedUuidKeyedRows() {
        valueTestRepository.save(valueTestRow());

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

    /// The other kind: the only seeded row with real `date` and `time` columns. No SQL literal suits
    /// every database — SQLite's driver reads a temporal only as a full `yyyy-MM-dd HH:mm:ss.SSS`
    /// timestamp, which a DATE column rejects on MySQL. Binding the values sidesteps the literal
    /// entirely. The key is generated, so no test may depend on a fixed one.
    private static ValueTestEntity valueTestRow() {
        LocalDate date = LocalDate.of(2022, 4, 30);
        LocalTime time = LocalTime.of(10, 0);
        ValueTestEntity entity = new ValueTestEntity();
        entity.setLocalDateTime(LocalDateTime.of(date, time));
        entity.setLocalDate(date);
        entity.setLocalTime(time);
        entity.setInstantTime(LocalDateTime.of(date, time).toInstant(ZoneOffset.UTC));
        entity.setZonedDateTime(ZonedDateTime.of(date, time, ZoneOffset.UTC));
        entity.setBooleanValue(true);
        entity.setDoubleValue(0.59393);
        return entity;
    }
}
