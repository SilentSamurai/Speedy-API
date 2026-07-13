package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/// Minimal entity carrying one obviously sensitive field ({@link #salary}), dedicated to
/// exercising field-level ABAC where a caller is granted the whole entity except one denied field.
@Getter
@Setter
@Table(name = "employees")
@Entity
public class Employee extends AbstractBaseEntity {

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "salary", nullable = false)
    private Double salary;

}
