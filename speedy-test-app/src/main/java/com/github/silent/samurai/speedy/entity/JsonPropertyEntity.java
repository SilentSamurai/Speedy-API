package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/// # JsonPropertyEntity
///
/// Test entity that exercises the `@JsonProperty` output-name override
/// path in {@link JpaMetaModelProcessorV2#findOutputName}. Each field
/// carries a {@code @JsonProperty} annotation whose {@code value} is
/// used as the REST API output property name, overriding the Java field
/// name.
///
/// ## Fields
/// - {@code custom_name} — maps to column `name` (String)
/// - {@code custom_cost} — maps to column `cost` (Long)
/// - {@code custom_category} — {@code @ManyToOne} to {@link Category},
///   verifies association name override
///
/// ## Expected API Behavior
/// ```json
/// {
///   "id": "<uuid>",
///   "custom_name": "widget",
///   "custom_cost": 150,
///   "custom_category": { "id": "<uuid>" }
/// }
/// ```
/// The Java field names {@code name}, {@code cost}, and {@code category}
/// are never exposed in serialized output.
@Getter
@Setter
@Table(name = "json_property_entity")
@Entity
public class JsonPropertyEntity extends AbstractBaseEntity {

    @JsonProperty("custom_name")
    @Column(name = "name")
    private String name;

    @JsonProperty("custom_cost")
    @Column(name = "cost")
    private Long cost;

    @JsonProperty("custom_category")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;
}
