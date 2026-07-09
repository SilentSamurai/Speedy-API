package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/// {@code @IdClass} for {@link SensorReading}. Mirrors its two key columns:
/// {@code sensorId} (VARCHAR) + {@code recordedAt} (TIMESTAMP_WITH_ZONE / {@link Instant}).
@Getter
@Setter
@EqualsAndHashCode
public class SensorReadingId implements Serializable {

    @Column(name = "sensor_id", nullable = false, length = 100)
    private String sensorId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

}
