package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import com.sun.jdi.BooleanValue;
import lombok.SneakyThrows;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


public class SelectVerifier {

    private static final Logger log = LoggerFactory.getLogger(SelectVerifier.class);
    Select select;
    List<Expression> whereCondition = new ArrayList<>();

    public SelectVerifier(Statement stmt) {
        this.select = (Select) stmt;
    }

    public static SelectVerifier parse(String sql) throws Exception {
        Statement stmt = CCJSqlParserUtil.parse(sql);
        assertInstanceOf(Select.class, stmt);
        SelectVerifier selectVerifier = new SelectVerifier(stmt);
        selectVerifier.captureWhereCondition();
        return selectVerifier;
    }

    public List<Expression> captureWhereCondition() throws JsonProcessingException {
        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
        Expression whereExpression = selectBody.getWhere();
        Map<String, Object> exprs = new HashMap<>();
        if (whereExpression != null) {
            extractBinaryExpressions(whereExpression);
        }
        return whereCondition;
    }

    // ((a & b) & (c & d) || (e & f))
    private void extractBinaryExpressions(Expression expression) throws JsonProcessingException {
        if (expression instanceof Parenthesis parenthesis) {
            // Recursively handle AND conditions
            extractBinaryExpressions(parenthesis.getExpression());
        }  else if (expression instanceof AndExpression andExpression) {
            extractBinaryExpressions(andExpression.getLeftExpression());
            extractBinaryExpressions(andExpression.getRightExpression());
        } else if (expression instanceof OrExpression orExpression) {
            extractBinaryExpressions(orExpression.getLeftExpression());
            extractBinaryExpressions(orExpression.getRightExpression());
        }else if (expression instanceof InExpression inExpression) {
            whereCondition.add(inExpression);
        } else if (expression instanceof IsNullExpression isNullExpression) {
            whereCondition.add(isNullExpression);
        } else if (expression instanceof BinaryExpression binaryExpression) {
            whereCondition.add(binaryExpression);
        }
    }


    public void assertJoinExists() {
        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
        List<Join> joins = selectBody.getJoins();
        assertFalse(joins.isEmpty());
    }

    public void assertJoinDoNotExists() {
        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
        List<Join> joins = selectBody.getJoins();
        if (joins == null) {
            assertNull(joins);
        } else {
            assertEquals(0, joins.size());
        }
    }


    public Pair<String, String> join(String selfField, String otherField) {
        return Pair.of(selfField, otherField);
    }

    @SafeVarargs
    public final void assertJoins(Pair<String, String>... paid) {
        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
        List<Join> joins = selectBody.getJoins();
        assertFalse(joins.isEmpty());
        assertEquals(joins.size(), paid.length);
        for (Pair<String, String> pair : paid) {
            assertJoin(pair.getFirst(), pair.getSecond());
        }
    }

    public void assertJoin(String selfField, String joinField) {
        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
        List<Join> joins = selectBody.getJoins();

        assertNotNull(joins, "No JOIN clauses found in the query.");

        boolean joinConditionExists = false;

        for (Join join : joins) {
            for (Expression expression : join.getOnExpressions()) {
                if (expression instanceof BinaryExpression binaryExpr) {
                    Column leftExpression = (Column) binaryExpr.getLeftExpression();
                    Column rightExpression = (Column) binaryExpr.getRightExpression();

                    if (matchesColumn(leftExpression, selfField) && matchesColumn(rightExpression, joinField) || matchesColumn(leftExpression, joinField) && matchesColumn(rightExpression, selfField)) {
                        joinConditionExists = true;
                        break;
                    }
                }
            }
        }
        assertTrue(joinConditionExists, "No join found in the query.");
    }


    public void assertTable(String table) {
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(select);
        Set<String> tables = tableList.stream().map(name -> name.replace("\"", "")).collect(Collectors.toSet());
        assertTrue(tables.contains(table), "Expected table: " + table + " not found in SQL." + tables);
    }

    public void assertFrom(String tableName) {
        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
        Table table = (Table) selectBody.getFromItem();
        boolean b = matchesString(tableName, removeQuotes(table.getName()));
        assertTrue(b, "Expected table name does not match the actual table name.");
    }

    public void assertWhereExists() {
        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
        assertNotNull(selectBody.getWhere(), "WHERE clause is missing in SQL.");
    }

    public void assertWhereCondition(String field, String operator, String value, ColumnType type) throws Exception {
        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
        assertNotNull(selectBody.getWhere(), "WHERE clause is missing in SQL.");
        boolean isMatch = false;
        for (Expression expression : whereCondition) {
            if (matchesBinaryCondition(expression, field, operator, value, type)) {
                isMatch = true;
                break;
            }
        }
        assertTrue(isMatch, "condition not matched.");
    }

    private boolean matchInExpression(Expression expression, String field, List<String> values, ColumnType type) {
        assertInstanceOf(InExpression.class, expression);
        InExpression inExpression = (InExpression) expression;
        Column column = (Column) inExpression.getLeftExpression();
        assertInstanceOf(ExpressionList.class, inExpression.getRightItemsList());
        ExpressionList expressionList = (ExpressionList) inExpression.getRightItemsList();
        boolean allValuesMatch = expressionList.getExpressions().stream().allMatch(
                expr -> values.stream().anyMatch(value -> matchesValue(expr, value, type))
        );
        return matchesColumn(column, field) && allValuesMatch;
    }

    private boolean matchNotInExpression(Expression expression, String field, List<String> values, ColumnType type) {
        assertInstanceOf(InExpression.class, expression);
        InExpression inExpression = (InExpression) expression;

        Column column = (Column) inExpression.getLeftExpression();
        assertInstanceOf(ExpressionList.class, inExpression.getRightItemsList());
        ExpressionList expressionList = (ExpressionList) inExpression.getRightItemsList();
        boolean allValuesMatch = expressionList.getExpressions().stream().allMatch(
                expr -> values.stream().anyMatch(value -> matchesValue(expr, value, type))
        );
        return matchesColumn(column, field) && allValuesMatch && inExpression.isNot();
    }

    private boolean matchesBinaryCondition(Expression expression, String field, String operator, String value, ColumnType type) throws Exception {
        assertInstanceOf(BinaryExpression.class, expression);
        BinaryExpression binaryExpression = (BinaryExpression) expression;
        Column column = (Column) binaryExpression.getLeftExpression();
        return matchesColumn(column, field)
                && matchOperator(binaryExpression, operator)
                && matchesValue(binaryExpression.getRightExpression(), value, type);
    }

    boolean matchesColumn(Column column, String field) {
        String table = "*";
        String schema = "*";
        if (field.contains(".")) {
            String[] split = field.split("\\.");
            if (split.length == 2) {
                table = split[0];
                field = split[1];
            }
            if (split.length == 3) {
                schema = split[0];
                table = split[1];
                field = split[2];
            }
        }
        return matchesString(schema, removeQuotes(column.getTable().getSchemaName()))
                && matchesString(table, removeQuotes(column.getTable().getName()))
                && matchesString(field, removeQuotes(column.getColumnName()));
    }

    private boolean matchesValue(Expression expression, String expectedValue, ColumnType type) {
        if (expression == null) return false;
        return switch (type) {
            case CHAR, VARCHAR, TEXT, UUID -> expression instanceof StringValue stringValue
                    && matchesString(expectedValue, removeQuotes(stringValue.getValue()));
            case INTEGER, SMALLINT, BIGINT -> expression instanceof LongValue longValue
                    && longValue.getValue() == Long.parseLong(expectedValue);
            case DECIMAL, NUMERIC, FLOAT, REAL, DOUBLE -> {
                if (expression instanceof LongValue longValue) {
                    yield longValue.getValue() == Long.parseLong(expectedValue);
                } else if (expression instanceof DoubleValue doubleValue) {
                    yield doubleValue.getValue() == Double.parseDouble(expectedValue);
                } else {
                    yield false;
                }
            }
            case BOOLEAN -> expression instanceof BooleanValue booleanValue &&
                    booleanValue.booleanValue() == Boolean.parseBoolean(expectedValue);
            case DATE -> expression instanceof StringValue stringValue
                    && isValidDate(stringValue.getValue(), expectedValue, "yyyy-MM-dd");
            case TIME -> expression instanceof StringValue stringValue
                    && isValidDate(stringValue.getValue(), expectedValue, "HH:mm:ss");
            case TIMESTAMP, TIMESTAMP_WITH_ZONE -> expression instanceof StringValue stringValue
                    && isValidDate(stringValue.getValue(), expectedValue, "yyyy-MM-dd HH:mm:ss");
            case BLOB, CLOB -> throw new UnsupportedOperationException("BLOB and CLOB matching is not supported");
            default -> throw new UnsupportedOperationException("Unhandled column type: " + type);
        };
    }

    private boolean matchOperator(BinaryExpression expr, String operator) {
        return switch (operator) {
            case "=" -> expr instanceof EqualsTo;
            case "!=" -> expr instanceof NotEqualsTo;
            case ">" -> expr instanceof GreaterThan;
            case ">=" -> expr instanceof GreaterThanEquals;
            case "<" -> expr instanceof MinorThan;
            case "<=" -> expr instanceof MinorThanEquals;
            case "LIKE" -> expr instanceof LikeExpression;
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    private String removeQuotes(String str) {
        if (str == null) return "";
        return str.replace("\"", "");
    }

    @SneakyThrows
    private boolean isValidDate(String sqlDate, String expectedDate, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setLenient(false);
        Date date = sdf.parse(sqlDate);
        return matchesString(expectedDate, date.toString());
    }

    private boolean matchesString(String pattern, String value) {
        String regex = pattern.replace("?", ".").replace("*", ".*");
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(value).matches();
    }


    public void assertInCondition(String field, List<String> values, ColumnType columnType) {
        PlainSelect selectBody = (PlainSelect) select.getSelectBody();
        assertNotNull(selectBody.getWhere(), "WHERE clause is missing in SQL.");
        boolean isMatch = false;
        for (Expression expression : whereCondition) {
            if (matchInExpression(expression, field, values, columnType)) {
                isMatch = true;
                break;
            }
        }
        assertTrue(isMatch, "condition not matched.");
    }

    public void assertNotInCondition(String field, List<String> values, ColumnType columnType) {
        assertWhereExists();
        boolean isMatch = false;
        for (Expression expression : whereCondition) {
            if (matchNotInExpression(expression, field, values, columnType)) {
                isMatch = true;
                break;
            }
        }
        assertTrue(isMatch, "condition not matched.");
    }

    public void assertIsNull(String field) {
        boolean isMatch = false;
        for (Expression expression : whereCondition) {
            assertInstanceOf(IsNullExpression.class, expression);
            IsNullExpression isNullExpression = (IsNullExpression) expression;
            Column column = (Column) isNullExpression.getLeftExpression();
            isMatch = matchesColumn(column, field);
            if (isMatch) {
                break;
            }
        }
        assertTrue(isMatch, "condition not matched.");
    }

    public void assertIsNotNull(String field) {
        boolean isMatch = false;
        for (Expression expression : whereCondition) {
            assertInstanceOf(IsNullExpression.class, expression);
            IsNullExpression isNullExpression = (IsNullExpression) expression;
            Column column = (Column) isNullExpression.getLeftExpression();
            isMatch = matchesColumn(column, field) && isNullExpression.isNot();
            if (isMatch) {
                break;
            }
        }
        assertTrue(isMatch, "condition not matched.");
    }

}
