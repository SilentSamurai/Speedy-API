package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;

import java.util.List;
import java.util.Set;

public interface SpeedyQuery {

    EntityMetadata getFrom();

    BooleanCondition getWhere();

    List<String> getGroupBy();

    Condition getHaving();

    List<OrderBy> getOrderByList();

    PageInfo getPageInfo();

    List<String> getExpand();

    Set<String> getSelect();

}
