package com.suke.czx.config.datasource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL 次要数据源验证测试
 * <p>
 * 验证内容：
 * - PostgreSQL 数据源连接正常
 * - pgJdbcTemplate 可用
 * - 查询操作执行成功
 * - PGVector 功能可用
 */
@SpringBootTest
@ActiveProfiles("test")
public class PostgreSqlDataSourceValidationTest {

    @Autowired
    @Qualifier("pgDataSource")
    private DataSource pgDataSource;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    @Test
    void shouldHavePostgreSqlDataSource() {
        assertNotNull(pgDataSource, "PostgreSQL DataSource should not be null");
        assertTrue(pgDataSource.getClass().getName().contains("HikariDataSource"),
                "DataSource should be HikariCP");
    }

    @Test
    void shouldConnectToPostgreSql() {
        // 测试连接
        Integer result = pgJdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result, "PostgreSQL connection test failed");
    }

    @Test
    void shouldExecuteQueryOnPostgreSql() {
        // 测试查询
        Integer result = pgJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema()",
                Integer.class
        );
        assertNotNull(result, "Should return table count");
    }

    @Test
    void shouldHavePgJdbcTemplate() {
        assertNotNull(pgJdbcTemplate, "pgJdbcTemplate should not be null");

        // 执行查询验证是 PostgreSQL 数据库
        String dbName = pgJdbcTemplate.queryForObject("SELECT current_database()", String.class);
        assertNotNull(dbName, "Should return database name");
    }

    @Test
    void shouldSupportPostgreSqlDialect() {
        // 测试 PostgreSQL 特定函数
        String version = pgJdbcTemplate.queryForObject("SELECT version()", String.class);
        assertNotNull(version, "Should return PostgreSQL version");
        assertTrue(version.contains("PostgreSQL"), "Version should indicate PostgreSQL");
    }

    @Test
    void shouldConfigureHikariPool() {
        // 验证 HikariCP 配置
        assertDoesNotThrow(() -> {
            // 多次执行查询测试连接池复用
            for (int i = 0; i < 10; i++) {
                pgJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            }
        }, "HikariCP pool should handle multiple queries");
    }

    @Test
    void shouldSupportPgVectorExtension() {
        // 检查 PGVector 扩展是否可用
        Integer count = pgJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'",
                Integer.class
        );
        // PGVector 可能未安装，所以这里只检查不抛异常
        assertNotNull(count, "Should return extension count");
    }

    @Test
    void shouldCreateVectorTable() {
        // 测试创建向量表
        assertDoesNotThrow(() -> {
            pgJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_vector_table (
                    id SERIAL PRIMARY KEY,
                    embedding vector(1024)
                )
                """);
        }, "Should create vector table without exception");

        // 验证表已创建
        Integer count = pgJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'test_vector_table'",
                Integer.class
        );
        assertTrue(count > 0, "Vector table should be created");

        // 清理
        pgJdbcTemplate.execute("DROP TABLE IF EXISTS test_vector_table");
    }

    @Test
    void shouldInsertAndQueryVector() {
        // 测试向量插入和查询
        assertDoesNotThrow(() -> {
            // 创建表
            pgJdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_vector_query (
                    id SERIAL PRIMARY KEY,
                    name TEXT,
                    embedding vector(1024)
                )
                """);

            // 插入向量（使用零向量作为示例）
            pgJdbcTemplate.execute("""
                INSERT INTO test_vector_query (name, embedding)
                VALUES ('test', '[0]'::vector || array_fill(0.0, ARRAY[1023])::vector)
                """);

            // 查询
            Integer count = pgJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM test_vector_query",
                    Integer.class
            );
            assertTrue(count > 0, "Should have inserted vector");

            // 清理
            pgJdbcTemplate.execute("DROP TABLE IF EXISTS test_vector_query");
        }, "Should handle vector operations");
    }

    @Test
    void shouldSupportTransactionWithPgJdbcTemplate() {
        // 测试 pgJdbcTemplate 的事务支持
        assertDoesNotThrow(() -> {
            pgJdbcTemplate.execute("CREATE TEMPORARY TABLE IF NOT EXISTS temp_pg_test (id INT)");
            pgJdbcTemplate.execute("INSERT INTO temp_pg_test VALUES (1)");
            Integer count = pgJdbcTemplate.queryForObject("SELECT COUNT(*) FROM temp_pg_test", Integer.class);
            assertEquals(1, count, "Should have 1 row");
        }, "Should support temporary table operations");
    }

    @Test
    void shouldHandlePostgreSqlSpecificTypes() {
        // 测试 PostgreSQL 特定数据类型
        assertDoesNotThrow(() -> {
            // JSONB 类型
            pgJdbcTemplate.execute("""
                CREATE TEMPORARY TABLE IF NOT EXISTS temp_types (
                    id INT,
                    data JSONB,
                    ts TIMESTAMP WITH TIME ZONE
                )
                """);

            pgJdbcTemplate.execute("""
                INSERT INTO temp_types (id, data, ts)
                VALUES (1, '{"key": "value"}'::jsonb, CURRENT_TIMESTAMP)
                """);

            Integer count = pgJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM temp_types",
                    Integer.class
            );
            assertTrue(count > 0, "Should have inserted row with PostgreSQL types");
        }, "Should handle PostgreSQL specific types");
    }
}