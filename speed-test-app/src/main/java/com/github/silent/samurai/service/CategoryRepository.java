package com.github.silent.samurai.service;


import com.github.silent.samurai.entity.Category;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public interface CategoryRepository extends CrudRepository<Category, String> {

}
