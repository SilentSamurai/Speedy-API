package com.github.silent.samurai.speedy.repositories;

import com.github.silent.samurai.speedy.entity.Product;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, String> {
}
