package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyType;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.types.Email;
import com.github.silent.samurai.speedy.types.EmailConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "custom_type_entity")
@Entity
public class CustomTypeEntity extends AbstractBaseEntity {

    @SpeedyType(ColumnType.VARCHAR)
    @Convert(converter = EmailConverter.class)
    @Column(name = "email", nullable = false, length = 320)
    private Email email;

}
