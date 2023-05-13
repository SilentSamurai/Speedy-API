package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Table(name = "orders")
@Entity
@IdClass(OrderId.class)
public class Order implements Serializable {

    @Id
    @Column(name = "product_id", nullable = false, length = 250)
    private String product;

    @Id
    @Column(name = "supplier_id", nullable = false, length = 250)
    private String supplier;

    @Column(name = "order_date")
    private Instant orderDate;

    @Column(name = "price")
    private Double price;

    @Column(name = "discount")
    private Double discount;

}