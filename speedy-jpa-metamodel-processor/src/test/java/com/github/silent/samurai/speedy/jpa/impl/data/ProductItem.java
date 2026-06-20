package com.github.silent.samurai.speedy.jpa.impl.data;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductItem {

    @Id
    String id;
    String name;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ProductItem{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
