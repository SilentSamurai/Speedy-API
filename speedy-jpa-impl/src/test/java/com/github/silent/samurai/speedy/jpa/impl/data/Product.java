package com.github.silent.samurai.speedy.jpa.impl.data;


import lombok.Data;

import javax.persistence.Id;


@Data
public class Product {

    @Id
    String id;
    String name;
    String category;
    Integer cost;

}
