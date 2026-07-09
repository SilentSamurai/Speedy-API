package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/// A composite-key entity whose temporal key column is a *zoned* instant rather than a local
/// timestamp: {@code sensorId} (VARCHAR) + {@code recordedAt} ({@link Instant}, which Speedy maps to
/// {@code TIMESTAMP_WITH_ZONE} / {@code ZONED_DATE_TIME}). {@link PriceHistory} covers the
/// zone-less {@code LocalDateTime} key; this covers the zoned variant, whose URL parsing goes
/// through {@code ZonedDateTime.parse} and requires the {@code Z} (UTC) form.
@Getter
@Setter
@Table(name = "sensor_reading")
@Entity
@IdClass(SensorReadingId.class)
public class SensorReading implements Serializable {

    @Id
    @Column(name = "sensor_id", nullable = false, length = 100)
    private String sensorId;

    @Id
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "label", length = 200)
    private String label;

}
