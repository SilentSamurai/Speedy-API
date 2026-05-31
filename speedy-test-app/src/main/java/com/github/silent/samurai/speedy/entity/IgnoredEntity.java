package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SpeedyIgnore
@Table(name = "ignored_entity")
@Entity
public class IgnoredEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    protected String id;

    @Column(name = "NAME", nullable = false, length = 250)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;
}
