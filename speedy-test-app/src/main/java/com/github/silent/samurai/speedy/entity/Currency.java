package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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

    @Column(name = "country", length = 10)
    private String country;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

}