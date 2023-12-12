package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.OrderBy;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import com.github.silent.samurai.speedy.models.conditions.EqCondition;
import com.github.silent.samurai.speedy.models.orderby.OrderByImpl;
import com.github.silent.samurai.speedy.parser.ConditionFactory;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class SpeedyQueryImpl implements SpeedyQuery {

    protected final ConditionFactory conditionFactory;
    private final EntityMetadata from;
    private List<Aggregation> aggregation;
    private BooleanCondition where = new BooleanConditionImpl(ConditionOperator.AND);
    private List<String> groupBy;
    private BooleanCondition having;
    private List<OrderBy> orderByList = new LinkedList<>();
    private PageInfoImpl pageInfo = new PageInfoImpl();

    public SpeedyQueryImpl(EntityMetadata from) {
        this.from = from;
        this.conditionFactory = new ConditionFactory(from);
    }

    public OrderBy orderByDesc(String field) throws NotFoundException {
        FieldMetadata fieldMetadata = this.from.field(field);
        OrderByImpl desc = OrderByImpl.desc(fieldMetadata);
        orderByList.add(desc);
        return desc;
    }

    public OrderBy orderByAsc(String field) throws NotFoundException {
        FieldMetadata fieldMetadata = this.from.field(field);
        OrderByImpl asc = OrderByImpl.asc(fieldMetadata);
        orderByList.add(asc);
        return asc;
    }

    public void addPageNo(int pageNo) {
        pageInfo.setPageNo(pageNo);
    }

    public void addPageSize(int pageSize) {
        if (pageSize > 0) {
            pageInfo.setPageSize(pageSize);
        }
    }

    public boolean isOnlyIdentifiersPresent() {
        boolean isAllEqualCondition = getWhere().getConditions()
                .stream()
                .allMatch(EqCondition.class::isInstance);
        if (isAllEqualCondition) {
            Set<String> keywords = getWhere().getConditions().stream()
                    .map(EqCondition.class::cast)
                    .map(condition -> condition.getField().getFieldMetadata().getClassFieldName())
                    .collect(Collectors.toSet());
            return MetadataUtil.hasOnlyPrimaryKeyFields(from, keywords);
        }
        return false;
    }

}