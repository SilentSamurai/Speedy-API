package com.github.silent.samurai.speedy.data;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Id;

@Getter
@Setter
public class ProductItem {

    @Id
    String id;
    String name;

}
