package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Points at Order, whose primary key is composite (@IdClass OrderId: product_id + supplier_id), so
// the association can only be expressed through a multi-column foreign key -- JPA's @JoinColumns,
// one @JoinColumn per key column. Covers the read/write/join/expand paths for that shape; every
// other association in this app targets a single-column key.
@Getter
@Setter
@Table(name = "order_shipments")
@Entity
public class OrderShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "carrier", nullable = false, length = 250)
    private String carrier;

    @ManyToOne(optional = false)
    @JoinColumns({
            @JoinColumn(name = "order_product_id", referencedColumnName = "product_id", nullable = false),
            @JoinColumn(name = "order_supplier_id", referencedColumnName = "supplier_id", nullable = false)
    })
    private Order order;

    // A second, nullable multi-column foreign key to the same target: covers $isnull/$isnotnull and
    // clearing a whole composite key on replace, neither of which the non-null `order` can reach,
    // and forces the two associations onto separate joins despite sharing a table.
    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "return_order_product_id", referencedColumnName = "product_id", nullable = true),
            @JoinColumn(name = "return_order_supplier_id", referencedColumnName = "supplier_id", nullable = true)
    })
    private Order returnOrder;

}
