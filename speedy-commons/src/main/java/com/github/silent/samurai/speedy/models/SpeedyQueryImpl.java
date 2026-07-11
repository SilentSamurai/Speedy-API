package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.OrderBy;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import com.github.silent.samurai.speedy.models.orderby.OrderByImpl;
import com.github.silent.samurai.speedy.parser.ConditionFactory;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class SpeedyQueryImpl implements SpeedyQuery {

    protected final ConditionFactory conditionFactory;
    private final EntityMetadata from;
    private SpeedyRequestType type;
    private List<Aggregation> aggregation;
    private BooleanCondition where = new BooleanConditionImpl(ConditionOperator.AND);
    private List<String> groupBy;
    private BooleanCondition having;
    private List<OrderBy> orderByList = new LinkedList<>();
    private PageInfoImpl pageInfo = new PageInfoImpl();
    private Set<String> expand = new LinkedHashSet<>();
    private Set<String> select = new LinkedHashSet<>();
    private boolean countRequest = false;
    private String responseFormat;
    private int maxPageSize = 1000;

    public SpeedyQueryImpl(EntityMetadata from) {
        this.from = from;
        this.conditionFactory = new ConditionFactory(from);
    }

    public OrderBy orderByDesc(String field) throws SpeedyHttpException {
        QueryField queryField = this.conditionFactory.createQueryField(field);
        OrderByImpl desc = OrderByImpl.desc(queryField);
        orderByList.add(desc);
        return desc;
    }

    public OrderBy orderByAsc(String field) throws SpeedyHttpException {
        QueryField queryField = this.conditionFactory.createQueryField(field);
        OrderByImpl asc = OrderByImpl.asc(queryField);
        orderByList.add(asc);
        return asc;
    }

    public void addPageNo(int pageNo) {
        pageInfo.setPageNo(pageNo);
    }

    public void addPageSize(int pageSize) throws BadRequestException {
        if (pageSize <= 0) {
            throw new BadRequestException("Page size must be > 0, got " + pageSize);
        }
        if (pageSize > maxPageSize) {
            throw new BadRequestException("Page size " + pageSize + " exceeds maximum allowed " + maxPageSize);
        }
        pageInfo.setPageSize(pageSize);
    }

    public void addExpand(String expand) {
        this.expand.add(expand);
    }

    public void addSelect(String select) {
        this.select.add(select);
    }

    public void addFormat(String format) {
        this.responseFormat = format;
    }


}
