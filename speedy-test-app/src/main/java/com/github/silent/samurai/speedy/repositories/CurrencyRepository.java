package com.github.silent.samurai.speedy.repositories;

import com.github.silent.samurai.speedy.entity.Currency;
import org.springframework.data.repository.CrudRepository;

public interface CurrencyRepository extends CrudRepository<Currency, String> {
}
