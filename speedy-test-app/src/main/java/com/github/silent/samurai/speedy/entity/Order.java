package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "orders")
@Entity
@IdClass(OrderId.class)
public class Order implements Serializable {

    @Id
    @Column(name = "product_id", nullable = false, length = 250)
    private String productId;

    @Id
    @Column(name = "supplier_id", nullable = false, length = 250)
    private String supplierId;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "price")
    private Double price;

    @Column(name = "discount")
    private Double discount;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Product product;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id", nullable = false, insertable = false, updatable = false)
    private Supplier supplier;

}