package com.github.silent.samurai.speedy.repositories;


import com.github.silent.samurai.speedy.entity.Category;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface CategoryRepository extends CrudRepository<Category, String> {

    Optional<Category> findByName(String name);

    @Query("select c from Category c order by c.name asc")
    List<Category> findAllSorted();
}
