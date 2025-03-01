package com.github.silent.samurai.speedy.repositories;


import com.github.silent.samurai.speedy.entity.Category;
import com.github.silent.samurai.speedy.entity.PkUuidTest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public interface PkUuidTestRepository extends CrudRepository<PkUuidTest, UUID> {

    Optional<PkUuidTest> findByName(String name);

}
