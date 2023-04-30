package com.github.silent.samurai.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import java.io.Serializable;

@Getter
@Setter
public class OrderId implements Serializable {

    @Column(name = "product_id", nullable = false, length = 250)
    private String product;

    @Column(name = "supplier_id", nullable = false, length = 250)
    private String supplier;


}
