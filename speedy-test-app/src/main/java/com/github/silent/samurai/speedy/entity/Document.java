package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/// Minimal owner-tagged entity dedicated to exercising row-level ABAC "only my own rows" policies,
/// where {@link #owner} is compared against the calling principal's id.
@Getter
@Setter
@Table(name = "documents")
@Entity
public class Document extends AbstractBaseEntity {

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "owner", nullable = false, length = 250)
    private String owner;

    /// Unsized text. `@Column.length()` reports 255 whether or not it is stated, so a `@Lob` column
    /// is the case where that number means nothing and must not be enforced as a width.
    ///
    /// The JDBC type is pinned rather than left to `@Lob` alone, because `@Lob` on a String means
    /// CLOB, and two dialects turn that into a column this field cannot use: Postgres maps CLOB to
    /// `oid` — a large-object handle, which rejects a text value outright — and MySQL sizes it from
    /// `@Column.length()`, so the unstated 255 yields `tinytext`. `LONG32VARCHAR` is the "long
    /// string" type instead: `text` on Postgres and `longtext` on MySQL regardless of any declared
    /// length, and the same `clob` the other three dialects already produced.
    @Lob
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "content")
    private String content;

    /// Unsized text again, declared *without* `@Lob`. `@JdbcTypeCode` alone makes the column a long
    /// string — the same `text`/`longtext`/`clob` {@link #content} gets — and `@Column.length()` still
    /// answers 255, so a width check that recognises only `@Lob` rejects what this column holds.
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "summary")
    private String summary;

    /// A width the metamodel cannot read. `columnDefinition` replaces the generated DDL outright, so
    /// JPA never renders `length()` and its 255 describes nothing — the column is `varchar(2000)`.
    /// Whatever the column really holds is the database's to enforce, not core's to guess.
    @Column(name = "abstract_text", columnDefinition = "varchar(2000)")
    private String abstractText;

}
