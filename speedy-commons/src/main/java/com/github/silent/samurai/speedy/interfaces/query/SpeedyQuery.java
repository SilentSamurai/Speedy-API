package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;

import java.util.List;

public interface SpeedyQuery {

    EntityMetadata getFrom();

    BooleanCondition getWhere();

    List<String> getGroupBy();

    Condition getHaving();

    List<OrderBy> getOrderByList();

    PageInfo getPageInfo();

    List<String> getExpand();

    List<String> getSelect();

}
