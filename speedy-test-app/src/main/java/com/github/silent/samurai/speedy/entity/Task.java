package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "tasks")
@Entity
public class Task extends AbstractBaseEntity {

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "difficulty", nullable = false)
    private TaskDifficulty difficulty = TaskDifficulty.MEDIUM;
}
