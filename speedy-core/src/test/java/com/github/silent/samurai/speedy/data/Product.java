package com.github.silent.samurai.speedy.data;


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
