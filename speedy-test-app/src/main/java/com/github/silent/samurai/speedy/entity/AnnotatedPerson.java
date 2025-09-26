package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.validation.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

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

    // --- New numeric validation demo fields ---------------------------------

    /** Positive (>0) salary */
    @SpeedyPositive
    @Positive
    @Column(name = "salary", nullable = false)
    private java.math.BigDecimal salary;

    /** Positive or zero score */
    @SpeedyPositiveOrZero
    @PositiveOrZero
    @Column(name = "score", nullable = false)
    private Integer score;

    /** Negative value allowed (<0) */
    @SpeedyNegative
    @Negative
    @Column(name = "debt", nullable = false)
    private java.math.BigDecimal debt;

    /** Negative or zero */
    @SpeedyNegativeOrZero
    @NegativeOrZero
    @Column(name = "overdraft", nullable = false)
    private Integer overdraft;

    /** Decimal min/max between 0.5 and 5.0 */
    @SpeedyDecimalMin(value = "0.5")
    @SpeedyDecimalMax(value = "5.0")
    @DecimalMin(value = "0.5")
    @DecimalMax(value = "5.0")
    @Column(name = "rating", nullable = false)
    private java.math.BigDecimal rating;

    /** 5 integer digits and 2 fraction digits max */
    @SpeedyDigits(integer = 5, fraction = 2)
    @Digits(integer = 5, fraction = 2)
    @Column(name = "precision_val", nullable = false)
    private java.math.BigDecimal precisionVal;

    /** Website URL (optional, validated when supplied) */
    @SpeedyUrl
    @Column(name = "website")
    private String website;

    // Date validation demo fields

    /** Birth date must be in the past */
    @SpeedyPast
    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    /** Appointment date must be in the future */
    @SpeedyFuture(message = "appointment must be in the future")
    @Column(name = "appointment_date")
    private java.time.LocalDate appointmentDate;

    /** Booking date must be between 2025-01-01 and 2025-12-31 */
    @SpeedyDateRange(min = "2025-01-01", max = "2025-12-31", message = "booking must be in 2025")
    @Column(name = "booking_date")
    private java.time.LocalDate bookingDate;

    /** ISO DATE format enforced */
    @SpeedyDateWithFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "iso_date")
    private java.time.LocalDate isoDate;
}
