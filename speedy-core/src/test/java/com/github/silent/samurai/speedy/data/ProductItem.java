package com.github.silent.samurai.speedy.data;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;

@Getter
@Setter
public class ProductItem {

    @Id
    String id;
    String name;

}
