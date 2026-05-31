package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyType;
import com.github.silent.samurai.speedy.enums.ColumnType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "type_override_table")
@Entity
public class TypeOverrideEntity extends AbstractBaseEntity {

    @SpeedyType(ColumnType.TEXT)
    @Column(name = "text_field")
    private String textField;

    @SpeedyType(ColumnType.BIGINT)
    @Column(name = "big_int_field")
    private Integer bigIntField;

    @SpeedyType(ColumnType.FLOAT)
    @Column(name = "float_field")
    private Double floatField;

}
