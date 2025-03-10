package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "currencies")
@Entity
public class Currency extends AbstractBaseEntity {

    @NotNull
    @Column(name = "currency_name", length = 64)
    private String currencyName;

    @NotNull
    @Column(name = "currency_symbol", length = 10)
    private String currencySymbol;

    @NotNull
    @Column(name = "currency_abbr", length = 10)
    private String currencyAbbr;

    @Column(name = "country", length = 32)
    private String country;

    @Generated(value = GenerationTime.ALWAYS)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}