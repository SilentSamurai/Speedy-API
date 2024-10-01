package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.*;

@Getter
@Setter
@Table(name = "value_test_table")
@Entity
public class ValueTestEntity extends AbstractBaseEntity {


    @Column(name = "local_date_time")
    private LocalDateTime localDateTime;

    @Column(name = "local_date")
    private LocalDate localDate;

    @Column(name = "local_time")
    private LocalTime localTime;

    @Column(name = "instant_time")
    private Instant instantTime;

    @Column(name = "zoned_date_time")
    private ZonedDateTime zonedDateTime;

    @Column(name = "boolean_value")
    private Boolean booleanValue;


}
