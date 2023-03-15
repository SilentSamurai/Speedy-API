package com.github.silent.samurai.entity;

import javax.persistence.*;

@Table(name = "inventory")
@Entity
public class Inventory extends AbstractBaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "cost", nullable = false)
    private Double cost;

    @Column(name = "listing_price", nullable = false)
    private Double listingPrice;

    @Column(name = "sold_price", nullable = false)
    private Double soldPrice;

    @Column(name = "discount", nullable = false)
    private Double discount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "procurement_id", nullable = false)
    private Procurement procurement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Procurement getProcurement() {
        return procurement;
    }

    public void setProcurement(Procurement procurement) {
        this.procurement = procurement;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getSoldPrice() {
        return soldPrice;
    }

    public void setSoldPrice(Double soldPrice) {
        this.soldPrice = soldPrice;
    }

    public Double getListingPrice() {
        return listingPrice;
    }

    public void setListingPrice(Double listingPrice) {
        this.listingPrice = listingPrice;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}