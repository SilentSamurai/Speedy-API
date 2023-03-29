package com.github.silent.samurai.data;


import lombok.Data;

import javax.persistence.Id;


@Data
public class EntityTestClass {

    @Id
    String id;
    String name;
    String category;

}
