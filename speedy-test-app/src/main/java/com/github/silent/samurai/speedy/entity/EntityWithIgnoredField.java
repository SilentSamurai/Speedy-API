package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "entity_with_ignored_field")
@Entity
public class EntityWithIgnoredField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    protected String id;

    @Column(name = "VISIBLE_FIELD", nullable = false, length = 250)
    private String visibleField;

    @SpeedyIgnore
    @Column(name = "HIDDEN_FIELD", length = 250)
    private String hiddenField;

    @SpeedyIgnore
    @Column(name = "INT_FIELD")
    private Integer intField;

    @ManyToOne
    @JoinColumn(name = "IGNORED_ENTITY_ID")
    private IgnoredEntity associationToIgnored;
}
