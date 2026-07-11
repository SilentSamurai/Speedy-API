package com.github.silent.samurai.speedy.interfaces.request;

/// A write-request body that carries one or more independently processed items.
///
/// This is deliberately separate from {@link SpeedyBody}: query and other request bodies do not
/// have a meaningful item count. Implementations use it to expose whether a request is a bulk
/// write without coupling the bulk gate to a concrete create, update, or delete body type.
public interface BulkRequestBody extends SpeedyBody {

    /// The number of items carried by this request body.
    int itemCount();

    /// Whether this body represents a bulk writing rather than a single-item writing.
    default boolean isBulk() {
        return itemCount() > 1;
    }
}
