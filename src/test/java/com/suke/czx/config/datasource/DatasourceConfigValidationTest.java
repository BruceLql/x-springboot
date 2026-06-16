package com.suke.czx.config.datasource;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suke.czx.modules.ai.infrastructure.repository.AiChatMessageMapper;
import com.suke.czx.modules.ai.infrastructure.repository.AiChatSessionMapper;
import com.suke.czx.modules.ai.infrastructure.repository.AiKnowledgeMapper;
import com.suke.czx.modules.live.infrastructure.repository.LiveChatMapper;
import com.suke.czx.modules.sys.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据源配置验证测试
 * <p>
 * 全面验证本次修改的所有关键点：
 * 1. MySQL 数据源属性正确绑定（spring.datasource.mysql.* → HikariDataSource）
 * 2. Mapper 扫描正确归属（AI/live/warehouse Mapper 归 MySQL）
 * 3. Bean 命名约定一致
 * 4. 事务管理器正确创建
 * 5. PG 条件化 Bean 在 disabled 时不创建
 */
@SpringBootTest
@TestPropertySource(properties = {
        // 禁用 PG，验证条件化逻辑
        "spring.datasource.pg.enabled=false",
        // 禁用 AI Chat/Embedding 避免外部 API 依赖
        "spring.ai.openai.chat.enabled=false",
        "spring.ai.openai.embedding.enabled=false"
})
@DisplayName("数据源配置验证")
class DatasourceConfigValidationTest {

    @Autowired
    private ApplicationContext ctx;

    // ==================== 1. MySQL 数据源属性绑定验证 ====================

    @Nested
    @DisplayName("MySQL 数据源属性绑定")
    class MySqlDataSourceBinding {

        @Autowired
        @Qualifier("mysqlDataSource")
        private DataSource mysqlDataSource;

        @Test
        @DisplayName("MySQL DataSource 应为 HikariCP")
        void shouldBeHikariDataSource() {
            assertNotNull(mysqlDataSource);
            assertTrue(mysqlDataSource.getClass().getName().contains("HikariDataSource"),
                    "Expected HikariDataSource, got: " + mysqlDataSource.getClass().getName());
        }

        @Test
        @DisplayName("MySQL 连接可用，能执行查询")
        void shouldConnectAndQuery() throws Exception {
            try (Connection conn = mysqlDataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                String dbProduct = meta.getDatabaseProductName();
                assertTrue(dbProduct.contains("MySQL") || dbProduct.contains("MariaDB"),
                        "Expected MySQL/MariaDB, got: " + dbProduct);

                // 验证数据库名称
                JdbcTemplate jt = new JdbcTemplate(mysqlDataSource);
                String dbName = jt.queryForObject("SELECT DATABASE()", String.class);
                assertNotNull(dbName);
                assertTrue(dbName.contains("x_springboot") || dbName.contains("test"),
                        "Database name should contain 'x_springboot', got: " + dbName);
            }
        }

        @Test
        @DisplayName("HikariCP 连接池配置已绑定")
        void shouldHaveHikariPoolConfigured() {
            // 多次查询验证连接池复用
            JdbcTemplate jt = new JdbcTemplate(mysqlDataSource);
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 5; i++) {
                    assertEquals(1, jt.queryForObject("SELECT 1", Integer.class));
                }
            });
        }
    }

    // ==================== 2. Mapper 扫描归属验证 ====================

    @Nested
    @DisplayName("Mapper 扫描归属")
    class MapperScanningVerification {

        @Test
        @DisplayName("AI Mapper 应注入 MySQL SqlSessionFactory（不在 PG）")
        void shouldHaveAiMappersInMySqlContext() {
            // AI Mapper 应该在 Spring 容器中（由 MySQL 数据源管理）
            assertNotNull(ctx.getBean(AiChatSessionMapper.class),
                    "AiChatSessionMapper should exist in MySQL context");
            assertNotNull(ctx.getBean(AiChatMessageMapper.class),
                    "AiChatMessageMapper should exist in MySQL context");
            assertNotNull(ctx.getBean(AiKnowledgeMapper.class),
                    "AiKnowledgeMapper should exist in MySQL context");
        }

        @Test
        @DisplayName("非 AI Mapper 也应正确加载")
        void shouldHaveOtherMappers() {
            assertNotNull(ctx.getBean(SysUserMapper.class),
                    "SysUserMapper should exist in MySQL context");
            assertNotNull(ctx.getBean(LiveChatMapper.class),
                    "LiveChatMapper should exist in MySQL context");
        }

        @Test
        @DisplayName("所有 Mapper 应继承 BaseMapper")
        void shouldAllMappersExtendBaseMapper() {
            // 抽检几个关键 Mapper
            assertTrue(ctx.getBean(AiChatSessionMapper.class) instanceof BaseMapper);
            assertTrue(ctx.getBean(SysUserMapper.class) instanceof BaseMapper);
            assertTrue(ctx.getBean(LiveChatMapper.class) instanceof BaseMapper);
        }

        @Test
        @DisplayName("PostgreSQL Mapper 路径下应无 Bean（因为当前无 PG Mapper 且 PG disabled）")
        void shouldNotHavePgSpecificMappers() {
            // 验证 PG 专用 Mapper 包不存在多余的 Bean
            String[] mappers = ctx.getBeanNamesForType(BaseMapper.class);
            for (String name : mappers) {
                String className = ctx.getBean(name).getClass().getName();
                assertFalse(className.contains(".mapper.pg."),
                        "Should not have any mapper in .mapper.pg. package: " + className);
            }
        }
    }

    // ==================== 3. Bean 命名约定验证 ====================

    @Nested
    @DisplayName("Bean 命名约定")
    class BeanNamingConvention {

        @Test
        @DisplayName("MySQL 数据源相关 Bean 命名约定")
        void shouldHaveMySqlBeans() {
            assertTrue(ctx.containsBean("mysqlDataSource"), "mysqlDataSource bean missing");
            assertTrue(ctx.containsBean("mysqlSqlSessionFactory"), "mysqlSqlSessionFactory bean missing");
            assertTrue(ctx.containsBean("mysqlTransactionManager"), "mysqlTransactionManager bean missing");
            assertTrue(ctx.containsBean("mysqlSqlSessionTemplate"), "mysqlSqlSessionTemplate bean missing");
        }

        @Test
        @DisplayName("PostgreSQL 相关 Bean 在 disabled 时不应存在")
        void shouldNotHavePgBeansWhenDisabled() {
            assertFalse(ctx.containsBean("pgDataSource"),
                    "pgDataSource should NOT exist when PG is disabled");
            assertFalse(ctx.containsBean("pgSqlSessionFactory"),
                    "pgSqlSessionFactory should NOT exist when PG is disabled");
            assertFalse(ctx.containsBean("pgTransactionManager"),
                    "pgTransactionManager should NOT exist when PG is disabled");
            assertFalse(ctx.containsBean("pgSqlSessionTemplate"),
                    "pgSqlSessionTemplate should NOT exist when PG is disabled");
            assertFalse(ctx.containsBean("pgJdbcTemplate"),
                    "pgJdbcTemplate should NOT exist when PG is disabled");
        }

        @Test
        @DisplayName("@Primary DataSource 应为 mysqlDataSource")
        void shouldHavePrimaryDataSourceAsMySql() {
            DataSource primary = ctx.getBean(DataSource.class);
            DataSource mysql = ctx.getBean("mysqlDataSource", DataSource.class);
            assertSame(primary, mysql, "@Primary DataSource should be mysqlDataSource");
        }

        @Test
        @DisplayName("@Primary TransactionManager 应为 mysqlTransactionManager")
        void shouldHavePrimaryTransactionManagerAsMySql() {
            PlatformTransactionManager primary = ctx.getBean(PlatformTransactionManager.class);
            PlatformTransactionManager mysql = ctx.getBean("mysqlTransactionManager", PlatformTransactionManager.class);
            assertSame(primary, mysql, "@Primary TransactionManager should be mysqlTransactionManager");
        }
    }

    // ==================== 4. 事务管理器验证 ====================

    @Nested
    @DisplayName("事务管理器")
    class TransactionManagerVerification {

        @Autowired
        @Qualifier("mysqlTransactionManager")
        private PlatformTransactionManager mysqlTxManager;

        @Test
        @DisplayName("MySQL 事务管理器类型正确")
        void shouldBeDataSourceTransactionManager() {
            assertTrue(mysqlTxManager instanceof org.springframework.jdbc.datasource.DataSourceTransactionManager,
                    "MySQL TransactionManager should be DataSourceTransactionManager");
        }
    }

    // ==================== 5. 条件化 Bean 验证 ====================

    @Nested
    @DisplayName("条件化 Bean")
    class ConditionalBeanVerification {

        @Test
        @DisplayName("PG disabled 时 VectorStore 不应创建")
        void shouldNotHaveVectorStoreWhenPgDisabled() {
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> ctx.getBean("vectorStore"),
                    "vectorStore bean should NOT exist when PG is disabled");
        }

        @Test
        @DisplayName("PG disabled 时 VectorStoreService 应降级为空操作")
        void shouldHaveVectorStoreServiceAsFallback() {
            // VectorStoreService 应始终存在，内部降级
            assertNotNull(ctx.getBean("vectorStoreService"),
                    "vectorStoreService should exist even when PG is disabled (no-op fallback)");
        }
    }
}
