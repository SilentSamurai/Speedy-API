package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Fixture for @JoinColumn flags that *disagree* across the columns of one multi-column foreign key:
// the first column below takes JPA's defaults and the second marks itself unique, so reading the
// flags off whichever column happens to be declared first gives the wrong answer.
//
// Only `unique` can differ. Hibernate refuses to map a property whose join columns disagree on
// insertable, updatable or nullable -- "Column mappings for property 'null' mix insertable with
// 'insertable=false'", "... mix nullable with 'not null'" -- so those three can never reach Speedy
// in a disagreeing state, and combining them is defensive only.
@Getter
@Setter
@Table(name = "order_audits")
@Entity
public class OrderAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "note")
    private String note;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "audited_product_id", referencedColumnName = "product_id"),
            @JoinColumn(name = "audited_supplier_id", referencedColumnName = "supplier_id", unique = true)
    })
    private Order auditedOrder;

}
