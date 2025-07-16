package com.github.silent.samurai.speedy.data;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductItem {

    @Id
    String id;
    String name;

}
