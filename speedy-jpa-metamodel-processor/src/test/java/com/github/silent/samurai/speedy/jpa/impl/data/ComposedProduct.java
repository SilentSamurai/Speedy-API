package com.github.silent.samurai.speedy.jpa.impl.data;


import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ComposedProduct {

    @Id
    String id;
    String name;
    String category;

    @OneToOne
    ProductItem productItem;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ComposedProduct{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", category='").append(category).append('\'');
        sb.append(", productItem=").append(productItem);
        sb.append('}');
        return sb.toString();
    }
}
