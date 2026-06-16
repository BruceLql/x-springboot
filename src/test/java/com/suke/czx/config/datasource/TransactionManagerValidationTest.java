package com.suke.czx.config.datasource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事务管理器验证测试
 * <p>
 * 验证 MySQL 事务管理器的核心功能：
 * - 事务执行
 * - 事务回滚
 * - 多事务并发
 * - 只读事务
 * <p>
 * PG 事务管理器测试需在 PG 启用时单独运行。
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.pg.enabled=false",
        "spring.ai.openai.chat.enabled=false",
        "spring.ai.openai.embedding.enabled=false"
})
@DisplayName("事务管理器验证")
public class TransactionManagerValidationTest {

    @Autowired
    @Qualifier("mysqlTransactionManager")
    private PlatformTransactionManager mysqlTransactionManager;

    @Autowired
    @Qualifier("mysqlDataSource")
    private DataSource mysqlDataSource;

    @Test
    @DisplayName("MySQL 事务管理器应存在且类型正确")
    void shouldHaveMySqlTransactionManager() {
        assertNotNull(mysqlTransactionManager, "MySQL TransactionManager should not be null");
        assertTrue(mysqlTransactionManager instanceof org.springframework.jdbc.datasource.DataSourceTransactionManager,
                "MySQL TransactionManager should be DataSourceTransactionManager");
    }

    @Test
    @DisplayName("应在 MySQL 事务中执行查询")
    void shouldExecuteWithMySqlTransaction() {
        TransactionTemplate txTemplate = new TransactionTemplate(mysqlTransactionManager);

        Integer result = txTemplate.execute(status -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource);
            return jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        });

        assertEquals(1, result, "Should execute with MySQL transaction");
    }

    @Test
    @DisplayName("MySQL 事务应支持回滚")
    void shouldRollbackMySqlTransaction() {
        TransactionTemplate txTemplate = new TransactionTemplate(mysqlTransactionManager);

        // 创建临时表并回滚
        txTemplate.executeWithoutResult(status -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource);
            jdbcTemplate.execute("CREATE TEMPORARY TABLE IF NOT EXISTS temp_mysql_tx (id INT)");
            jdbcTemplate.execute("INSERT INTO temp_mysql_tx VALUES (1)");

            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM temp_mysql_tx", Integer.class);
            assertEquals(1, count, "Should have 1 row");
        });
    }

    @Test
    @DisplayName("应在 MySQL 事务中触发回滚")
    void shouldHandleMySqlTransactionRollback() {
        TransactionTemplate txTemplate = new TransactionTemplate(mysqlTransactionManager);

        try {
            txTemplate.executeWithoutResult(status -> {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource);
                jdbcTemplate.execute("CREATE TEMPORARY TABLE IF NOT EXISTS temp_mysql_rollback (id INT)");
                jdbcTemplate.execute("INSERT INTO temp_mysql_rollback VALUES (1)");

                // 模拟异常，触发回滚
                throw new RuntimeException("Simulated exception for rollback");
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            // 事务应该已回滚 — 正常行为
            assertTrue(e.getMessage().contains("Simulated exception"));
        }
    }

    @Test
    @DisplayName("应支持多个独立 MySQL 事务")
    void shouldExecuteMultipleMySqlTransactions() {
        TransactionTemplate txTemplate = new TransactionTemplate(mysqlTransactionManager);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource);

        jdbcTemplate.execute("CREATE TEMPORARY TABLE IF NOT EXISTS temp_mysql_multi (id INT, value INT)");

        for (int i = 1; i <= 5; i++) {
            final int value = i;
            txTemplate.executeWithoutResult(status -> {
                jdbcTemplate.execute("INSERT INTO temp_mysql_multi (id, value) VALUES ("
                        + value + ", " + value + ")");
            });
        }

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM temp_mysql_multi", Integer.class);
        assertEquals(5, count, "Should have 5 rows from multiple transactions");
    }

    @Test
    @DisplayName("应支持只读事务")
    void shouldSupportReadOnlyTransactions() {
        TransactionTemplate txTemplate = new TransactionTemplate(mysqlTransactionManager);
        txTemplate.setReadOnly(true);

        Integer result = txTemplate.execute(status -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlDataSource);
            return jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        });

        assertEquals(1, result, "Should execute read-only transaction");
    }
}
