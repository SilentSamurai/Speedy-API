package com.github.silent.samurai.service;


import com.github.silent.samurai.entity.Category;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface CategoryRepository extends CrudRepository<Category, String> {

    Optional<Category> findByName(String name);

}
