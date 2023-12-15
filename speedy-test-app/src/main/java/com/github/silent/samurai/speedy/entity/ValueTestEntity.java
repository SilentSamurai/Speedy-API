package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
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


}
