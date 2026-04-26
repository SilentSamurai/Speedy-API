# QueryVisitor Architecture — Implementation-Agnostic Query Construction

## Problem

`JooqQueryBuilder` fuses two concerns: walking the `SpeedyQuery` AST and emitting jOOQ DSL. This makes it impossible to support alternative backends (native SQL, MongoDB, R2DBC) without duplicating the walking logic. The jOOQ implementation also lives in `speedy-core`, creating a hard dependency.

## Solution

Separate the AST traversal from the backend-specific code generation using a Visitor pattern.

```
SpeedyQuery (existing AST in speedy-commons)
    │
    ▼
QueryWalker (new — walks the AST, calls visitor methods)
    │
    ├── JooqSelectVisitor     → produces jOOQ SelectQuery
    ├── JooqInsertVisitor     → produces jOOQ InsertQuery
    ├── JooqUpdateVisitor     → produces jOOQ UpdateQuery
    ├── JooqDeleteVisitor     → produces jOOQ DeleteQuery
    ├── NativeSqlVisitor      → produces SQL string + bind params
    ├── MongoQueryVisitor     → produces Bson filter
    └── ExplainVisitor        → produces human-readable query plan
```

## Existing AST (no changes needed)

These interfaces in `speedy-commons` already form a clean, implementation-agnostic query model:

- `SpeedyQuery` — root: from, where, orderBy, pageInfo, expand, select
- `BooleanCondition` — AND/OR combinator with sub-conditions
- `BinaryCondition` — field + operator + expression
- `QueryField` — field reference (normal or associated)
- `Expression` → `Literal` (value) or `Identifier` (field reference)
- `OrderBy` — field + ASC/DESC
- `PageInfo` — pageNo + pageSize

---

## New Interfaces (speedy-commons)

### QueryVisitor

```java
package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;

/**
 * Visitor for constructing a backend-specific query from a SpeedyQuery AST.
 * Each backend (jOOQ, native SQL, MongoDB, etc.) implements this interface.
 *
 * @param <T> the type of the built query (e.g., jOOQ SelectQuery, SQL string, Bson)
 */
public interface QueryVisitor<T> {

    // --- Source ---

    void visitFrom(EntityMetadata entity) throws SpeedyHttpException;

    // --- Conditions ---

    void visitEq(QueryField field, Expression expression) throws SpeedyHttpException;

    void visitNeq(QueryField field, Expression expression) throws SpeedyHttpException;

    void visitLt(QueryField field, Expression expression) throws SpeedyHttpException;

    void visitGt(QueryField field, Expression expression) throws SpeedyHttpException;

    void visitLte(QueryField field, Expression expression) throws SpeedyHttpException;

    void visitGte(QueryField field, Expression expression) throws SpeedyHttpException;

    void visitIn(QueryField field, Expression expression) throws SpeedyHttpException;

    void visitNotIn(QueryField field, Expression expression) throws SpeedyHttpException;

    void visitPattern(QueryField field, Expression expression) throws SpeedyHttpException;

    // --- Boolean combinators ---

    void beginAnd() throws SpeedyHttpException;

    void endAnd() throws SpeedyHttpException;

    void beginOr() throws SpeedyHttpException;

    void endOr() throws SpeedyHttpException;

    // --- Ordering ---

    void visitOrderBy(FieldMetadata field, OrderByOperator direction) throws SpeedyHttpException;

    // --- Pagination ---

    void visitPage(int offset, int limit) throws SpeedyHttpException;

    // --- Joins (for associated field conditions) ---

    void visitJoin(FieldMetadata fkField, FieldMetadata targetField) throws SpeedyHttpException;

    // --- Terminal ---

    T build() throws SpeedyHttpException;
}
```

### MutationVisitor

```java
package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;

/**
 * Visitor for constructing backend-specific mutation operations.
 *
 * @param <T> the type of the built mutation (e.g., jOOQ InsertQuery, SQL string)
 */
public interface MutationVisitor<T> {

    void visitEntity(EntityMetadata entity) throws SpeedyHttpException;

    void visitField(FieldMetadata field, SpeedyValue value) throws SpeedyHttpException;

    void visitAssociationField(FieldMetadata fkField, FieldMetadata associatedField, SpeedyValue value) throws SpeedyHttpException;

    void visitKeyCondition(FieldMetadata keyField, SpeedyValue value) throws SpeedyHttpException;

    T build() throws SpeedyHttpException;
}
```

---

## QueryWalker (speedy-commons)

A single, reusable walker that traverses any `SpeedyQuery` and drives a visitor:

```java
package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.query.*;

public class QueryWalker {

    /**
     * Walk a SpeedyQuery AST and drive the visitor to produce a result.
     */
    public static <T> T walk(SpeedyQuery query, QueryVisitor<T> visitor) throws SpeedyHttpException {
        visitor.visitFrom(query.getFrom());

        if (query.getWhere() != null) {
            walkCondition(query.getWhere(), visitor);
        }

        for (OrderBy ob : query.getOrderByList()) {
            visitor.visitOrderBy(ob.getFieldMetadata(), ob.getOperator());
        }

        PageInfo page = query.getPageInfo();
        int offset = page.getPageNo() * page.getPageSize();
        visitor.visitPage(offset, page.getPageSize());

        return visitor.build();
    }

    private static <T> void walkCondition(Condition condition, QueryVisitor<T> visitor) throws SpeedyHttpException {
        if (condition instanceof BooleanCondition bc) {
            boolean isAnd = bc.getOperator() == ConditionOperator.AND;

            if (isAnd) visitor.beginAnd();
            else visitor.beginOr();

            for (Condition sub : bc.getConditions()) {
                walkCondition(sub, visitor);
            }

            if (isAnd) visitor.endAnd();
            else visitor.endOr();

        } else if (condition instanceof BinaryCondition bin) {
            // Resolve joins for associated fields
            QueryField field = bin.getField();
            if (field.isAssociated()) {
                visitor.visitJoin(field.getFieldMetadata(), field.getAssociatedFieldMetadata());
            }

            switch (bin.getOperator()) {
                case EQ -> visitor.visitEq(field, bin.getExpression());
                case NEQ -> visitor.visitNeq(field, bin.getExpression());
                case LT -> visitor.visitLt(field, bin.getExpression());
                case GT -> visitor.visitGt(field, bin.getExpression());
                case LTE -> visitor.visitLte(field, bin.getExpression());
                case GTE -> visitor.visitGte(field, bin.getExpression());
                case IN -> visitor.visitIn(field, bin.getExpression());
                case NOT_IN -> visitor.visitNotIn(field, bin.getExpression());
                case PATTERN_MATCHING -> visitor.visitPattern(field, bin.getExpression());
                default -> throw new IllegalStateException("Unexpected operator: " + bin.getOperator());
            }
        }
    }
}
```

### MutationWalker

```java
package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.MutationVisitor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;

public class MutationWalker {

    public static <T> T walkInsert(SpeedyEntity entity, MutationVisitor<T> visitor) throws SpeedyHttpException {
        EntityMetadata metadata = entity.getMetadata();
        visitor.visitEntity(metadata);

        for (FieldMetadata field : metadata.getAllFields()) {
            if (!entity.has(field) || entity.get(field).isEmpty() || entity.get(field).isNull()) {
                continue;
            }
            SpeedyValue value = entity.get(field);
            if (field.isAssociation()) {
                SpeedyEntity assoc = value.asObject();
                FieldMetadata assocField = field.getAssociatedFieldMetadata();
                if (assoc.has(assocField) && !assoc.get(assocField).isNull()) {
                    visitor.visitAssociationField(field, assocField, assoc.get(assocField));
                }
            } else {
                visitor.visitField(field, value);
            }
        }

        return visitor.build();
    }

    public static <T> T walkUpdate(SpeedyEntityKey pk, SpeedyEntity entity, MutationVisitor<T> visitor) throws SpeedyHttpException {
        EntityMetadata metadata = entity.getMetadata();
        visitor.visitEntity(metadata);

        // Fields to update
        for (FieldMetadata field : metadata.getAllNonKeyFields()) {
            if (!entity.has(field) || entity.get(field).isEmpty() || entity.get(field).isNull()) {
                continue;
            }
            SpeedyValue value = entity.get(field);
            if (field.isAssociation()) {
                SpeedyEntity assoc = value.asObject();
                FieldMetadata assocField = field.getAssociatedFieldMetadata();
                if (assoc.has(assocField) && !assoc.get(assocField).isNull()) {
                    visitor.visitAssociationField(field, assocField, assoc.get(assocField));
                }
            } else {
                visitor.visitField(field, value);
            }
        }

        // PK conditions
        for (KeyFieldMetadata keyField : metadata.getKeyFields()) {
            visitor.visitKeyCondition(keyField, pk.get(keyField));
        }

        return visitor.build();
    }

    public static <T> T walkDelete(SpeedyEntityKey pk, MutationVisitor<T> visitor) throws SpeedyHttpException {
        EntityMetadata metadata = pk.getMetadata();
        visitor.visitEntity(metadata);

        for (KeyFieldMetadata keyField : metadata.getKeyFields()) {
            visitor.visitKeyCondition(keyField, pk.get(keyField));
        }

        return visitor.build();
    }
}
```

---

## JooqSelectVisitor (speedy-jooq-impl)

Port of the existing `JooqQueryBuilder` emit logic, without the walking:

```java
package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.*;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.*;

public class JooqSelectVisitor implements QueryVisitor<SelectQuery<Record>> {

    private final DSLContext dslContext;
    private final SQLDialect dialect;
    private final Converter converter;
    private SelectQuery<Record> query;

    // Stack for building nested AND/OR conditions
    private final Deque<List<org.jooq.Condition>> conditionStack = new ArrayDeque<>();

    // Join tracking (same as existing JooqQueryBuilder)
    private final Map<String, FieldMetadata> joins = new HashMap<>();
    private final Map<String, String> joinAlias = new HashMap<>();

    public JooqSelectVisitor(DSLContext dslContext, Converter converter) {
        this.dslContext = dslContext;
        this.dialect = dslContext.dialect();
        this.converter = converter;
        // Initialize with a root condition list
        conditionStack.push(new ArrayList<>());
    }

    // --- Source ---

    @Override
    public void visitFrom(EntityMetadata entity) {
        this.query = dslContext.selectQuery(JooqUtil.getTable(entity, dialect));
    }

    // --- Conditions ---

    @Override
    public void visitEq(QueryField field, Expression expr) throws SpeedyHttpException {
        Field<Object> col = resolveColumn(field);
        if (expr instanceof Literal l && l.value().isNull()) {
            pushCondition(col.isNull());
        } else if (expr instanceof Identifier id) {
            pushCondition(col.eq(resolveColumn(id.field())));
        } else {
            pushCondition(col.eq(DSL.value(resolveValue(field, expr))));
        }
    }

    @Override
    public void visitNeq(QueryField field, Expression expr) throws SpeedyHttpException {
        Field<Object> col = resolveColumn(field);
        if (expr instanceof Literal l && l.value().isNull()) {
            pushCondition(col.isNotNull());
        } else if (expr instanceof Identifier id) {
            pushCondition(col.ne(resolveColumn(id.field())));
        } else {
            pushCondition(col.ne(DSL.value(resolveValue(field, expr))));
        }
    }

    @Override
    public void visitLt(QueryField field, Expression expr) throws SpeedyHttpException {
        pushComparison(field, expr, "lt");
    }

    @Override
    public void visitGt(QueryField field, Expression expr) throws SpeedyHttpException {
        pushComparison(field, expr, "gt");
    }

    @Override
    public void visitLte(QueryField field, Expression expr) throws SpeedyHttpException {
        pushComparison(field, expr, "lte");
    }

    @Override
    public void visitGte(QueryField field, Expression expr) throws SpeedyHttpException {
        pushComparison(field, expr, "gte");
    }

    @Override
    public void visitIn(QueryField field, Expression expr) throws SpeedyHttpException {
        Field<Object> col = resolveColumn(field);
        if (expr instanceof Literal l && l.value().isCollection()) {
            Collection<Param<Object>> params = new ArrayList<>();
            for (SpeedyValue sv : l.value().asCollection()) {
                params.add(DSL.value(toJooqType(field, sv)));
            }
            pushCondition(col.in(params));
        } else {
            pushCondition(col.in(DSL.value(resolveValue(field, expr))));
        }
    }

    @Override
    public void visitNotIn(QueryField field, Expression expr) throws SpeedyHttpException {
        Field<Object> col = resolveColumn(field);
        if (expr instanceof Literal l && l.value().isCollection()) {
            Collection<Param<Object>> params = new ArrayList<>();
            for (SpeedyValue sv : l.value().asCollection()) {
                params.add(DSL.value(toJooqType(field, sv)));
            }
            pushCondition(col.notIn(params));
        } else {
            pushCondition(col.notIn(List.of(DSL.value(resolveValue(field, expr)))));
        }
    }

    @Override
    public void visitPattern(QueryField field, Expression expr) throws SpeedyHttpException {
        Field<Object> col = resolveColumn(field);
        SpeedyValue val = ((Literal) expr).value();
        String rawValue = val.asText()
                .replaceAll("%", "\\\\%")
                .replaceAll("\\*", "%");
        pushCondition(col.like(DSL.value(rawValue)).escape('\\'));
    }

    // --- Boolean combinators ---

    @Override
    public void beginAnd() {
        conditionStack.push(new ArrayList<>());
    }

    @Override
    public void endAnd() {
        List<org.jooq.Condition> conditions = conditionStack.pop();
        org.jooq.Condition combined = conditions.stream()
                .reduce(DSL.noCondition(), org.jooq.Condition::and);
        conditionStack.peek().add(combined);
    }

    @Override
    public void beginOr() {
        conditionStack.push(new ArrayList<>());
    }

    @Override
    public void endOr() {
        List<org.jooq.Condition> conditions = conditionStack.pop();
        org.jooq.Condition combined = conditions.stream()
                .reduce(DSL.noCondition(), org.jooq.Condition::or);
        conditionStack.peek().add(combined);
    }

    // --- Ordering ---

    @Override
    public void visitOrderBy(FieldMetadata field, OrderByOperator direction) {
        Field<Object> col = JooqUtil.getColumn(field, dialect);
        if (direction == OrderByOperator.ASC) {
            query.addOrderBy(col.asc());
        } else {
            query.addOrderBy(col.desc());
        }
    }

    // --- Pagination ---

    @Override
    public void visitPage(int offset, int limit) {
        query.addLimit(offset, limit);
    }

    // --- Joins ---

    @Override
    public void visitJoin(FieldMetadata fkField, FieldMetadata targetField) {
        String key = fkField.getAssociationMetadata().getDbTableName() + "." + fkField.getDbColumnName();
        if (!joins.containsKey(key)) {
            joins.put(key, fkField);
            String alias = fkField.getAssociationMetadata().getName() + "_" + (joinAlias.size() + 1);
            joinAlias.put(key, alias);
        }
    }

    // --- Terminal ---

    @Override
    public SelectQuery<Record> build() {
        // Apply accumulated joins
        for (var entry : joins.entrySet()) {
            FieldMetadata fkField = entry.getValue();
            String alias = joinAlias.get(entry.getKey());
            Table<?> table = JooqUtil.getTable(fkField.getAssociationMetadata(), dialect).as(alias);
            Field<?> fromCol = JooqUtil.getColumn(fkField, dialect);
            Field<?> toCol = JooqUtil.getColumnWithTableAlias(alias, fkField.getAssociatedFieldMetadata(), dialect);
            query.addJoin(table, fromCol.eq(toCol));
        }

        // Apply root conditions
        List<org.jooq.Condition> rootConditions = conditionStack.pop();
        if (!rootConditions.isEmpty()) {
            org.jooq.Condition where = rootConditions.stream()
                    .reduce(DSL.noCondition(), org.jooq.Condition::and);
            query.addConditions(where);
        }

        return query;
    }

    // --- Helpers ---

    private Field<Object> resolveColumn(QueryField field) {
        if (field.isAssociated()) {
            String key = field.getAssociatedFieldMetadata().getEntityMetadata().getDbTableName()
                    + "." + field.getFieldMetadata().getDbColumnName();
            String alias = joinAlias.get(key);
            if (alias != null) {
                return JooqUtil.getColumnWithTableAlias(alias, field.getAssociatedFieldMetadata(), dialect);
            }
        }
        return JooqUtil.getColumn(field.getFieldMetadata(), dialect);
    }

    private Object resolveValue(QueryField field, Expression expr) throws SpeedyHttpException {
        SpeedyValue val = ((Literal) expr).value();
        return toJooqType(field, val);
    }

    private Object toJooqType(QueryField field, SpeedyValue val) {
        FieldMetadata meta = field.isAssociated()
                ? field.getAssociatedFieldMetadata()
                : field.getFieldMetadata();
        return converter.toColumnType(val, meta);
    }

    private void pushCondition(org.jooq.Condition condition) {
        conditionStack.peek().add(condition);
    }

    private void pushComparison(QueryField field, Expression expr, String op) throws SpeedyHttpException {
        Field<Object> col = resolveColumn(field);
        if (expr instanceof Identifier id) {
            Field<Object> other = resolveColumn(id.field());
            switch (op) {
                case "lt" -> pushCondition(col.lt(other));
                case "gt" -> pushCondition(col.gt(other));
                case "lte" -> pushCondition(col.le(other));
                case "gte" -> pushCondition(col.ge(other));
            }
        } else {
            Object val = resolveValue(field, expr);
            switch (op) {
                case "lt" -> pushCondition(col.lt(DSL.value(val)));
                case "gt" -> pushCondition(col.gt(DSL.value(val)));
                case "lte" -> pushCondition(col.le(DSL.value(val)));
                case "gte" -> pushCondition(col.ge(DSL.value(val)));
            }
        }
    }
}
```

---

## Usage — Before vs After

### Before (current)

```java
// In JooqQueryProcessorImpl.executeMany()
JooqQueryBuilder qb = new JooqQueryBuilder(speedyQuery, dslContext, converter);
Result<? extends Record> result = qb.executeQuery();
```

### After

```java
// In JooqQueryProcessorImpl.executeMany()
SelectQuery<Record> query = QueryWalker.walk(
    speedyQuery,
    new JooqSelectVisitor(dslContext, converter)
);
Result<Record> result = query.fetch();
```

### Hypothetical MongoDB backend

```java
// In MongoQueryProcessorImpl.executeMany()
Bson filter = QueryWalker.walk(
    speedyQuery,
    new MongoSelectVisitor(collection)
);
List<Document> result = collection.find(filter).into(new ArrayList<>());
```

---

## Module Restructure

```
speedy-commons
    ├── interfaces/query/QueryVisitor.java      (new)
    ├── interfaces/query/MutationVisitor.java    (new)
    ├── query/QueryWalker.java                   (new)
    └── query/MutationWalker.java                (new)

speedy-jooq-impl (new module, extracted from speedy-core)
    ├── JooqSelectVisitor.java
    ├── JooqInsertVisitor.java
    ├── JooqUpdateVisitor.java
    ├── JooqDeleteVisitor.java
    ├── JooqConversionImpl.java                  (moved from speedy-core)
    ├── JooqSqlToSpeedy.java                     (moved from speedy-core)
    ├── JooqUtil.java                            (moved from speedy-core)
    └── JooqQueryProcessorImpl.java              (moved, refactored to use visitors)

speedy-core
    └── No more jOOQ dependency — uses QueryProcessor interface only
```

---

## Migration Steps

1. Add `QueryVisitor`, `MutationVisitor`, `QueryWalker`, `MutationWalker` to `speedy-commons`
2. Create `speedy-jooq-impl` module with dependency on `speedy-commons` and jOOQ
3. Implement `JooqSelectVisitor` by extracting emit logic from `JooqQueryBuilder`
4. Implement `JooqInsertVisitor`, `JooqUpdateVisitor`, `JooqDeleteVisitor` from existing `SpeedyInsertQuery`, `SpeedyUpdateQuery`, `SpeedyDeleteQuery`
5. Refactor `JooqQueryProcessorImpl` to use `QueryWalker.walk()` + visitors
6. Move all jOOQ classes from `speedy-core` to `speedy-jooq-impl`
7. Remove jOOQ dependency from `speedy-core`
8. Update `CreateQueryProcessorHandler` to use a factory/SPI to instantiate the `QueryProcessor`
9. Update parent POM to include `speedy-jooq-impl` module

Each step is independently testable. The existing test suite in `speedy-test-app` validates correctness throughout.
