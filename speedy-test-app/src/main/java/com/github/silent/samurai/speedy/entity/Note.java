package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyBulk;
import com.github.silent.samurai.speedy.annotations.SpeedyETag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/// Minimal single-key entity dedicated to exercising `@SpeedyETag` (default RANDOM strategy):
/// ETag emission on GET, `If-None-Match` -> 304, `If-Match` -> 412/200 on PATCH/PUT/DELETE, and
/// the 400 rejections (multi-item batch, or an entity with no version field — see {@link Category}
/// for that negative case). Bulk is enabled so a 2-item batch without If-Match succeeds,
/// isolating the multi-item-plus-If-Match 400 from the separate bulk-disabled 400.
@Getter
@Setter
@Table(name = "notes")
@Entity
@SpeedyBulk(true)
public class Note extends AbstractBaseEntity {

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @SpeedyETag
    @Column(name = "row_version", length = 64)
    private String rowVersion;

}
