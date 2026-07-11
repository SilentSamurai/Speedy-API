package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.DeletedFilter;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;

import java.util.List;
import java.util.Set;

public interface SpeedyQuery extends SpeedyBody {

    String getResponseFormat();

    EntityMetadata getFrom();

    BooleanCondition getWhere();

    List<String> getGroupBy();

    Condition getHaving();

    List<OrderBy> getOrderByList();

    PageInfo getPageInfo();

    Set<String> getExpand();

    Set<String> getSelect();

    boolean isCountRequest();

    /// How soft-deleted rows are treated by this read request (default {@link DeletedFilter#EXCLUDE}).
    DeletedFilter getDeleted();

    /// Restricts this query with an additional top-level {@code AND} condition, wrapping any
    /// existing {@code $where} so it is ANDed (not merged into an {@code OR}). Used by the engine
    /// to enforce soft-delete visibility ({@code deletedAt IS NULL}) regardless of the caller's filter.
    void restrictWith(Condition condition);

}
