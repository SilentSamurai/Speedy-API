package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "suppliers", indexes = {
        @Index(name = "suppliers_alt_phone_no_key", columnList = "alt_phone_no", unique = true),
        @Index(name = "suppliers_phone_no_key", columnList = "phone_no", unique = true)
})
@Entity
public class Supplier extends AbstractBaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", length = 1024)
    private String address;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @Column(name = "alt_phone_no", nullable = false, length = 15)
    private String altPhoneNo;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by")
    private String createdBy;


}