package com.github.silent.samurai.data;


import lombok.Data;

import javax.persistence.Id;
import javax.persistence.OneToOne;


@Data
public class AssociatedEntityTestClass {

    @Id
    String id;
    String name;
    String category;

    @OneToOne
    AssociationEntity associationEntity;

}
