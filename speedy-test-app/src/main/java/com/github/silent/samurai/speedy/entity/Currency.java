package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
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