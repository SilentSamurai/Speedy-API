package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

@Getter
@Setter
@Table(name = "invoices")
@Entity
public class Invoice extends AbstractBaseEntity {

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "paid", nullable = false)
    private Double paid;

    @Column(name = "discount", nullable = false)
    private Double discount;

    @Column(name = "adjustment", nullable = false)
    private Double adjustment;

    @Column(name = "due_amount", nullable = false)
    private Double dueAmount;

    @Column(name = "notes", length = 1024)
    private String notes;

    @Column(name = "invoice_date", nullable = false)
    private Instant invoiceDate;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "modified_at")
    private Instant modifiedAt;

    @Column(name = "modified_by")
    private String modifiedBy;


}