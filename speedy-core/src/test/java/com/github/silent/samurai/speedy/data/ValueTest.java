package com.github.silent.samurai.speedy.data;


import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;


@Data
public class ValueTest {

    @Id
    String id;
    String name;
    String category;
    Integer cost;
    Integer intVal;
    Double doubleVal;
    LocalDate localDate;
    LocalTime localTime;
    LocalDateTime localDateTime;
    ZonedDateTime zonedDateTime;
    Boolean booleanVal;

}
