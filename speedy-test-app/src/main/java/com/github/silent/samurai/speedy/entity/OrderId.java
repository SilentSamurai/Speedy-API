package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import java.io.Serializable;

@Getter
@Setter
public class OrderId implements Serializable {

    @Column(name = "product_id", nullable = false, length = 250)
    private String productId;

    @Column(name = "supplier_id", nullable = false, length = 250)
    private String supplierId;


}
