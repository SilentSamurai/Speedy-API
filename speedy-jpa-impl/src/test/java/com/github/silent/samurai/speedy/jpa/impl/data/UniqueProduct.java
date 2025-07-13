package com.github.silent.samurai.speedy.jpa.impl.data;


import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Data;


@Data
@IdClass(UniqueProductKey.class)
public class UniqueProduct {


    String category;
    @Id
    private String id;
    @Id
    private String name;

}
