package com.github.silent.samurai.speedy.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "companies", indexes = {
        @Index(name = "companies_phone_key", columnList = "phone", unique = true)
})
@Entity
public class Company extends AbstractBaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", nullable = false, length = 1024)
    private String address;

    @Email
    @NotEmpty(message = "Email cannot be empty")
    @Column(name = "email")
    private String email;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "details_top", length = 1024)
    private String detailsTop;

    @Column(name = "extra", length = 1024)
    private String extra;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Column(name = "invoice_no")
    private Integer invoiceNo;

    @Column(name = "default_generator")
    private Integer defaultGenerator;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}