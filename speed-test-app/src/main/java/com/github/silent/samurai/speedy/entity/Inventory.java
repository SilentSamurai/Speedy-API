package com.github.silent.samurai.speedy.entity;

import lombok.Data;
import org.hibernate.annotations.Formula;

import javax.persistence.*;

@Data
@Table(name = "inventory")
@Entity
public class Inventory extends AbstractBaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "cost", nullable = false)
    private Double cost;

    @Column(name = "listing_price", nullable = false)
    private Double listingPrice;

    @Column(name = "sold_price", nullable = false)
    private Double soldPrice;

    @Column(name = "discount", nullable = false)
    private Double discount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "procurement_id", nullable = false)
    private Procurement procurement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @GeneratedValue
    @Formula("sold_price - cost")
    private Double profit;

}