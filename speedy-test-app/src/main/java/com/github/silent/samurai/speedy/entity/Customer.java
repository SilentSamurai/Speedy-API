package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.github.silent.samurai.speedy.annotations.SpeedyIgnore;
import com.github.silent.samurai.speedy.enums.IgnoreType;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;


/**
 *
 */
@Data
@Table(name = "customers", indexes = {
        @Index(name = "customers_alt_phone_no_key", columnList = "alt_phone_no", unique = true),
        @Index(name = "customers_phone_no_key", columnList = "phone_no", unique = true)
})
@Entity
public class Customer extends AbstractBaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", length = 1024)
    private String address;

    @Email
    @Column(name = "email")
    private String email;

    @Size(min = 10, max = 15)
    @Pattern(regexp = "(^$|[0-9]{10})")
    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @Size(min = 10, max = 15)
    @Pattern(regexp = "(^$|[0-9]{10})")
    @Column(name = "alt_phone_no", nullable = true, length = 15)
    private String altPhoneNo;

    @Column(name = "created_at")
    private Instant createdAt;

    @SpeedyIgnore(IgnoreType.WRITE)
    @Column(name = "created_by")
    private String createdBy;

    @JsonManagedReference
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Invoice> invoices;


}