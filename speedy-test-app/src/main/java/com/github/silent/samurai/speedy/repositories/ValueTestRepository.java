package com.github.silent.samurai.speedy.repositories;


import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.ValueTestEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Optional;

@Component
public interface ValueTestRepository extends CrudRepository<ValueTestEntity, String> {

    Optional<ValueTestEntity> findByLocalDateTime(LocalDateTime localDateTime);

    Optional<ValueTestEntity> findByLocalDate(LocalDate localDate);

    Optional<ValueTestEntity> findByLocalTime(LocalTime localTime);

    Optional<ValueTestEntity> findByInstantTime(Instant instantTime);

    Optional<ValueTestEntity> findByZonedDateTime(ZonedDateTime zonedDateTime);

}
