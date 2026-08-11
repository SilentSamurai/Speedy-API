package com.github.silent.samurai.speedy.metadata;

/// Unresolved form of {@link com.github.silent.samurai.speedy.interfaces.metadata.AssociationColumn}:
/// one foreign-key column named by strings, before the target entity's fields exist as metadata.
/// {@link MetaModelBuilder} resolves {@code targetFieldName} against the association's target entity
/// once every entity is built.
///
/// A null {@code localDbColumnName} means "the owning field's own column" — the single-column case,
/// where the association *is* the foreign-key column.
public record AssociationColumnRef(String localDbColumnName, String targetFieldName) {
}
