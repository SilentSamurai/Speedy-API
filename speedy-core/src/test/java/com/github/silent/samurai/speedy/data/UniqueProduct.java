package com.github.silent.samurai.speedy.data;


import lombok.Data;

import javax.persistence.Id;
import javax.persistence.IdClass;


@Data
@IdClass(UniqueProductKey.class)
public class UniqueProduct {


    @Id
    private String id;
    @Id
    private String name;

    String category;

}
