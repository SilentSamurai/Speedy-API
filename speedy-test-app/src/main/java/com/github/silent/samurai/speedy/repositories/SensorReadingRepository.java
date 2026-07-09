package com.github.silent.samurai.speedy.repositories;

import com.github.silent.samurai.speedy.entity.SensorReading;
import com.github.silent.samurai.speedy.entity.SensorReadingId;
import org.springframework.data.repository.CrudRepository;

/// Repository for {@link SensorReading}, used by tests to tear down rows in the shared H2 context.
public interface SensorReadingRepository extends CrudRepository<SensorReading, SensorReadingId> {
}
