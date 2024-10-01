package com.github.silent.samurai.speedy.jpa.impl.data;


import lombok.Data;

import jakarta.persistence.Id;
import jakarta.persistence.IdClass;


@Data
@IdClass(UniqueProductKey.class)
public class UniqueProduct {


    String category;
    @Id
    private String id;
    @Id
    private String name;

}
