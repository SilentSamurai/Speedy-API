package com.github.silent.samurai.speedy.repositories;


import com.github.silent.samurai.speedy.entity.Procurement;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public interface ProcurementRepository extends CrudRepository<Procurement, String> {
}
