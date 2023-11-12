package com.github.silent.samurai.speedy.repositories;


import com.github.silent.samurai.speedy.entity.Category;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface CategoryRepository extends CrudRepository<Category, String> {

    Optional<Category> findByName(String name);

}
