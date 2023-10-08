package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.sql.Date;

@Getter
@Setter
@Entity
@SpeedyIgnore
@Table(name = "exchange_rates")
public class ExchangeRate extends AbstractBaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency_id", nullable = false, referencedColumnName = "id")
    private Currency baseCurrency;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foreign_currency_id", nullable = false, referencedColumnName = "id")
    private Currency foreignCurrency;

    @NotNull
    @Column(name = "exchange_rate", nullable = false)
    private Double exchangeRate;

    @NotNull
    @Column(name = "inv_exchange_rate", nullable = false)
    private Double invExchangeRate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;
}
