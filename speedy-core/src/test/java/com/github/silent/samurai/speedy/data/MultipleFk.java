package com.github.silent.samurai.speedy.data;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class MultipleFk {

    @Id
    String id;
    String name;
    String category;

    @OneToOne
    Product a;

    @OneToOne
    Product b;

}
