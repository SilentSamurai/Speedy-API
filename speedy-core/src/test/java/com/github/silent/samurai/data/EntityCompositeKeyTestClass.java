package com.github.silent.samurai.data;


import lombok.Data;

import javax.persistence.Id;
import javax.persistence.IdClass;


@Data
@IdClass(PrimaryKeyTestClass.class)
public class EntityCompositeKeyTestClass {

    String category;
    @Id
    private String id;
    @Id
    private String name;

}
