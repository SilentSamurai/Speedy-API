package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyBulk;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Table(name = "categories", indexes = {
        @Index(name = "categories_name_key", columnList = "name", unique = true)
})
@Entity
@SpeedyBulk
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    protected String id;

    // unique is declared on the column as well as in the table-level index above: Hibernate emits an
    // inline UNIQUE for the former on every dialect, but its SQLite dialect drops `@Index` entirely,
    // which left duplicate names accepted there.
    @Column(name = "NAME", nullable = false, length = 250, unique = true)
    private String name;

    @OneToMany(mappedBy = "category")
    private Set<Product> products;

}