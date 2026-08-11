package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.SpeedyQuery;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Guards the `$expand` N+1 fix at the database boundary: association expansion must cost a fixed
/// number of SQL statements regardless of how many rows the query returns.
///
/// Rather than pinning an exact statement count — which would break whenever unrelated bookkeeping
/// queries change — this compares a one-row page against a many-row page of the same query. Under the
/// per-row expansion this replaced, the wider page issued one extra statement per row.
/// The capturing configuration is imported explicitly: a nested `@TestConfiguration` is only
/// auto-detected when `@SpringBootTest` declares no `classes`.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ExpandQueryCountTest.SqlCapturingConfig.class)
class ExpandQueryCountTest {

    private static final List<String> CAPTURED_SQL = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private MockMvc mvc;

    @Test
    void singleLevelExpandCostsTheSameNumberOfQueriesForOneRowAsForMany() throws Exception {
        int oneRow = selectsIssuedBy(expandQuery(1), 1);
        int manyRows = selectsIssuedBy(expandQuery(100), 2);

        assertEquals(oneRow, manyRows,
                "expanding more rows must not issue more queries; captured: " + CAPTURED_SQL);
    }

    @Test
    void nestedExpandCostsTheSameNumberOfQueriesForOneRowAsForMany() throws Exception {
        int oneRow = selectsIssuedBy(nestedExpandQuery(1), 1);
        int manyRows = selectsIssuedBy(nestedExpandQuery(100), 2);

        assertEquals(oneRow, manyRows,
                "nested expansion must not issue more queries for more rows; captured: " + CAPTURED_SQL);
    }

    private JsonNode expandQuery(int pageSize) {
        return SpeedyQuery.from("Product")
                .expand("Category")
                .pageNo(0)
                .pageSize(pageSize)
                .build();
    }

    private JsonNode nestedExpandQuery(int pageSize) {
        return SpeedyQuery.from("Inventory")
                .expand("Product")
                .expand("Product.Category")
                .pageNo(0)
                .pageSize(pageSize)
                .build();
    }

    /// Runs the query and returns how many SELECTs reached the database, asserting the response
    /// really carried at least {@code minimumRows} rows — otherwise the comparison proves nothing.
    private int selectsIssuedBy(JsonNode query, int minimumRows) throws Exception {
        CAPTURED_SQL.clear();
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .post(SpeedyConstants.URI + "/" + query.get("$from").asText() + "/" + SpeedyEndpoint.QUERY.suffix())
                        .content(CommonUtil.json().writeValueAsString(query))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = CommonUtil.json()
                .readTree(result.getResponse().getContentAsString())
                .get("payload");
        assertTrue(payload.size() >= minimumRows,
                "expected at least " + minimumRows + " rows to make the comparison meaningful, got " + payload.size());

        synchronized (CAPTURED_SQL) {
            int selects = (int) CAPTURED_SQL.stream()
                    .filter(sql -> sql.trim().toLowerCase(Locale.ROOT).startsWith("select"))
                    .count();
            // Without this the comparison could pass by capturing nothing at all.
            assertTrue(selects > 0, "no SQL was captured — the capturing DataSource is not in the wiring");
            return selects;
        }
    }

    @TestConfiguration
    static class SqlCapturingConfig {

        /// Wraps whatever {@link DataSource} the application context supplies, so every statement the
        /// engine prepares is recorded without the production wiring knowing about it.
        @Bean
        static BeanPostProcessor sqlCapturingDataSource() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof DataSource dataSource && !(bean instanceof CapturingDataSource)) {
                        return new CapturingDataSource(dataSource);
                    }
                    return bean;
                }
            };
        }
    }

    private record CapturingDataSource(DataSource delegate) implements DataSource {

        private static Connection capturing(Connection connection) {
            return (Connection) Proxy.newProxyInstance(
                    CapturingDataSource.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("prepare") && args != null
                                && args.length > 0 && args[0] instanceof String sql) {
                            CAPTURED_SQL.add(sql);
                        }
                        try {
                            return method.invoke(connection, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }

        @Override
        public Connection getConnection() throws SQLException {
            return capturing(delegate.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return capturing(delegate.getConnection(username, password));
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return delegate.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            delegate.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            delegate.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return delegate.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }
    }
}
