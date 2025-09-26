package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.validation.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "annotated_person")
public class AnnotatedPerson extends AbstractBaseEntity {

    @Column(name = "name", nullable = false)
    @SpeedyNotBlank
    @SpeedyLength(min = 3, max = 20)
    @NotBlank
    @Size(min = 3, max = 20)
    private String name;

    @Column(name = "age", nullable = false)
    @SpeedyMin(18)
    @SpeedyMax(60)
    @Min(18)
    private Integer age;

    @Column(name = "email", nullable = false)
    @SpeedyEmail
    @Email
    private String email;

    @Column(name = "code", nullable = false)
    @SpeedyRegex("^[A-Z]{3}[0-9]{2}$")
    @jakarta.validation.constraints.Pattern(regexp = "^[A-Z]{3}[0-9]{2}$")
    private String code;
}
