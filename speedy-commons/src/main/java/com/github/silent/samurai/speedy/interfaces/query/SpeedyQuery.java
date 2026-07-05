package com.github.silent.samurai.speedy.interfaces.query;

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

}
