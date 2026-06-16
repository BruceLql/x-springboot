package com.suke.czx.config.datasource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 主数据源验证测试
 * <p>
 * 验证内容：
 * - MySQL 数据源连接正常
 * - JdbcTemplate 可用
 * - 事务管理器工作正常
 * - 查询操作执行成功
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.pg.enabled=false",
        "spring.ai.openai.chat.enabled=false",
        "spring.ai.openai.embedding.enabled=false"
})
public class MysqlDataSourceValidationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldHaveMySqlDataSource() {
        assertNotNull(dataSource, "MySQL DataSource should not be null");
        assertTrue(dataSource.getClass().getName().contains("HikariDataSource"),
                "DataSource should be HikariCP");
    }

    @Test
    void shouldConnectToMySql() {
        // 测试连接
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result, "MySQL connection test failed");
    }

    @Test
    void shouldExecuteQueryOnMySql() {
        // 测试查询（确保 MySQL 已启动）
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                Integer.class
        );
        assertNotNull(result, "Should return table count");
        assertTrue(result > 0, "Database should have tables");
    }

    @Test
    void shouldHaveDefaultJdbcTemplate() {
        // 验证默认的 JdbcTemplate 是 MySQL 的
        assertNotNull(jdbcTemplate, "Default JdbcTemplate should not be null");

        // 执行查询验证是 MySQL 数据库
        String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        assertNotNull(dbName, "Should return database name");
        assertTrue(dbName.contains("x_springboot") || dbName.contains("test"),
                "Database name should contain 'x_springboot' or 'test'");
    }

    @Test
    @Transactional
    void shouldRollbackTransaction() {
        // 测试事务回滚
        // 创建临时表进行测试
        jdbcTemplate.execute("CREATE TEMPORARY TABLE IF NOT EXISTS temp_test (id INT)");

        jdbcTemplate.execute("INSERT INTO temp_test VALUES (1)");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM temp_test", Integer.class);
        assertEquals(1, count, "Should have 1 row before rollback");

        // 由于 @Transactional，测试完成后会自动回滚
    }

    @Test
    void shouldSupportMySqlDialect() {
        // 测试 MySQL 特定函数
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        assertNotNull(version, "Should return MySQL version");
        assertFalse(version.isEmpty(), "Version should not be empty");
    }

    @Test
    void shouldConfigureHikariPool() {
        // 验证 HikariCP 配置
        assertDoesNotThrow(() -> {
            // 多次执行查询测试连接池复用
            for (int i = 0; i < 10; i++) {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            }
        }, "HikariCP pool should handle multiple queries");
    }
}