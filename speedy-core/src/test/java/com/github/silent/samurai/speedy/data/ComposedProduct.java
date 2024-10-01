package com.github.silent.samurai.speedy.data;


import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;


@Getter
@Setter
public class ComposedProduct {

    @Id
    String id;
    String name;
    String category;

    @OneToOne
    ProductItem productItem;

}
