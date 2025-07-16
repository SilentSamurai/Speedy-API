package com.github.silent.samurai.speedy.jpa.impl.data;


import jakarta.persistence.Id;
import lombok.Data;


@Data
public class Product {

    @Id
    String id;
    String name;
    String category;
    Long cost;

}
