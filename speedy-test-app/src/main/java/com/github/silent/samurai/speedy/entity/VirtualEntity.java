package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
