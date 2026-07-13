package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/// Minimal entity dedicated to exercising row-condition ABAC keyed on lifecycle {@link #status}:
/// a rule can grant UPDATE/DELETE only while {@code status == DRAFT}, so once a row is
/// {@code PUBLISHED} it falls out of that condition and every further write (including reverting
/// the status field itself) is denied by the document's default effect.
@Getter
@Setter
@Table(name = "articles")
@Entity
public class Article extends AbstractBaseEntity {

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ArticleStatus status = ArticleStatus.DRAFT;

}
