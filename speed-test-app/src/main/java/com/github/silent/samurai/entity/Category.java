package com.github.silent.samurai.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "categories", indexes = {
        @Index(name = "categories_name_key", columnList = "name", unique = true)
})
@Entity
public class Category extends AbstractBaseEntity {
    @Column(name = "name", nullable = false, length = 250)
    private String name;

}