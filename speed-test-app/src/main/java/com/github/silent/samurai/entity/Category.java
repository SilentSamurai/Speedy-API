package com.github.silent.samurai.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Table(name = "categories", indexes = {
        @Index(name = "categories_name_key", columnList = "name", unique = true)
})
@Entity
public class Category extends AbstractBaseEntity {
    @Column(name = "name", nullable = false, length = 250)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}