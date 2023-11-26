package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;

import java.util.List;

public interface SpeedyQuery {

    EntityMetadata getFrom();

    Condition getWhere();

    List<String> getGroupBy();

    Condition getHaving();

    List<OrderBy> getOrderBy();

    PageInfo getPageInfo();

}
