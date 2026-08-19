package com.github.silent.samurai.speedy.repositories;


import com.github.silent.samurai.speedy.entity.FkNullEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public interface FkNullEntityRepository extends CrudRepository<FkNullEntity, UUID> {
}
