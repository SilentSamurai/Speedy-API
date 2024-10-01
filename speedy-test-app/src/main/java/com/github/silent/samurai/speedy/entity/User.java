package com.github.silent.samurai.speedy.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Table(name = "users", indexes = {
        @Index(name = "users_phone_no_key", columnList = "phone_no", unique = true),
        @Index(name = "users_email_key", columnList = "email", unique = true)
})
@Entity
public class User extends AbstractBaseEntity {
    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @Column(name = "email", nullable = false, length = 250)
    private String email;

    @Column(name = "profile_pic", nullable = false, length = 512)
    private String profilePic;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}