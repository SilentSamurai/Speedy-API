package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Getter
@Setter
@Table(name = "PRODUCT_VIEW")
@Entity
public class VirtualEntity {

    protected static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    protected String id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", length = 1024)
    private String description;

}
