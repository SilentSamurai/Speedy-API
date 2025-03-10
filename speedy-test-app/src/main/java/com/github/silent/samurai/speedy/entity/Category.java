package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Table(name = "categories", indexes = {
        @Index(name = "categories_name_key", columnList = "name", unique = true)
})
@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    protected String id;

    @Column(name = "NAME", nullable = false, length = 250)
    private String name;

    @OneToMany(mappedBy = "category")
    private Set<Product> products;

}