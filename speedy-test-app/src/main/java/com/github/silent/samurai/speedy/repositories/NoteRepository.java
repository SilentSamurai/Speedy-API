package com.github.silent.samurai.speedy.repositories;

import com.github.silent.samurai.speedy.entity.Note;
import org.springframework.data.repository.CrudRepository;

/// Repository for {@link Note}, used by tests to tear down rows in the shared H2 context.
public interface NoteRepository extends CrudRepository<Note, String> {
}
