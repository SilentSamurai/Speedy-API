package com.github.silent.samurai.speedy.jpa.impl.data;


import lombok.Data;

import javax.persistence.Id;
import javax.persistence.IdClass;


@Data
@IdClass(UniqueProductKey.class)
public class UniqueProduct {


    String category;
    @Id
    private String id;
    @Id
    private String name;

}
