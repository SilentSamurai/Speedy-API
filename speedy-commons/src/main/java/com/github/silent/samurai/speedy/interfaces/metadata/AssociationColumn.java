package com.github.silent.samurai.speedy.interfaces.metadata;

/// One column of the foreign key an association is mapped through: the column on the *owning*
/// entity's table, paired with the key field of the *target* entity it references.
///
/// A to-one association owns one {@code AssociationColumn} per column of the target's primary key —
/// one for the common single-column case, several when the target has a composite key (JPA's
/// {@code @JoinColumns}). {@link FieldMetadata#getAssociationColumns()} returns them ordered by the
/// target entity's key-field order, so the pairs line up positionally across reads, writes, joins
/// and generated documentation.
public record AssociationColumn(String localDbColumnName, FieldMetadata targetKeyField) {
}
