package com.github.silent.samurai.speedy.repositories;

import com.github.silent.samurai.speedy.entity.PriceHistory;
import com.github.silent.samurai.speedy.entity.PriceHistoryId;
import org.springframework.data.repository.CrudRepository;

/// Repository for {@link PriceHistory}, used by tests to tear down rows in the shared H2 context.
public interface PriceHistoryRepository extends CrudRepository<PriceHistory, PriceHistoryId> {
}
