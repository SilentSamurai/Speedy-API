package com.github.silent.samurai.speedy.helper;

import org.openapitools.client.model.QueryRequest;
import org.openapitools.client.model.QueryRequestWhere;
import org.openapitools.client.model.QueryRequestWhereValue;

import java.util.List;

public class SpdyQ {

    public static QueryRequest qry() {
        return new QueryRequest();
    }

    public static QueryRequest where(String key, QueryRequestWhereValue value) {
        QueryRequestWhere queryRequestWhere = new QueryRequestWhere();
        queryRequestWhere.put(key, value);
        return new QueryRequest().$where(queryRequestWhere);
    }

    public static QueryRequest whereEq(String key, Object value) {
        QueryRequestWhere queryRequestWhere = new QueryRequestWhere();
        queryRequestWhere.put(key, $eq(value));
        return new QueryRequest().$where(queryRequestWhere);
    }

    public static QueryRequestWhere where() {
        QueryRequestWhere queryRequestWhere = new QueryRequestWhere();
        return queryRequestWhere;
    }

    public static QueryRequestWhereValue $eq(Object value) {
        QueryRequestWhereValue condition = new QueryRequestWhereValue();
        condition.$eq(value);
        return condition;
    }

    public static QueryRequestWhereValue $lt(Object value) {
        QueryRequestWhereValue condition = new QueryRequestWhereValue();
        condition.$lt(value);
        return condition;
    }

    public static QueryRequestWhereValue $lte(Object value) {
        QueryRequestWhereValue condition = new QueryRequestWhereValue();
        condition.$lte(value);
        return condition;
    }

    public static QueryRequestWhereValue $gt(Object value) {
        QueryRequestWhereValue condition = new QueryRequestWhereValue();
        condition.$gt(value);
        return condition;
    }

    public static QueryRequestWhereValue $gte(Object value) {
        QueryRequestWhereValue condition = new QueryRequestWhereValue();
        condition.$gte(value);
        return condition;
    }

    public static QueryRequestWhereValue $ne(Object value) {
        QueryRequestWhereValue condition = new QueryRequestWhereValue();
        condition.$ne(value);
        return condition;
    }

    public static QueryRequestWhereValue $in(List<Object> values) {
        QueryRequestWhereValue condition = new QueryRequestWhereValue();
        condition.$in(values);
        return condition;
    }

    public static QueryRequestWhereValue $nin(List<Object> values) {
        QueryRequestWhereValue condition = new QueryRequestWhereValue();
        condition.$nin(values);
        return condition;
    }
}
