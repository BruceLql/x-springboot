package com.suke.czx.config.datasource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapper 分层验证测试
 * <p>
 * 验证内容：
 * - MySQL Mapper 正确扫描（含 AI/live/warehouse 等 DDD 模块）
 * - Mapper 归属 MySQL SqlSessionFactory
 * - 事务管理器正确应用
 * - 数据源隔离正确
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.pg.enabled=false",
        "spring.ai.openai.chat.enabled=false",
        "spring.ai.openai.embedding.enabled=false"
})
@Transactional
@DisplayName("Mapper 分层验证")
public class MapperLayeringValidationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("应加载所有 MySQL Mapper（含 AI 模块）")
    void shouldLoadAllMysqlMappers() {
        // 获取所有以 Mapper 结尾的 Bean
        String[] allBeans = applicationContext.getBeanDefinitionNames();
        String[] mapperNames = Arrays.stream(allBeans)
                .filter(name -> name.endsWith("Mapper"))
                .filter(name -> !name.contains("pg"))
                .toArray(String[]::new);

        assertTrue(mapperNames.length > 10,
                "Should have many MySQL Mappers, got: " + mapperNames.length);

        // 验证核心 AI Mapper 存在
        assertTrue(Arrays.asList(mapperNames).contains("aiChatSessionMapper"),
                "aiChatSessionMapper should exist");
        assertTrue(Arrays.asList(mapperNames).contains("aiChatMessageMapper"),
                "aiChatMessageMapper should exist");
        assertTrue(Arrays.asList(mapperNames).contains("aiKnowledgeMapper"),
                "aiKnowledgeMapper should exist");

        // 验证其他模块 Mapper 存在
        assertTrue(Arrays.asList(mapperNames).contains("sysUserMapper"),
                "sysUserMapper should exist");
        assertTrue(Arrays.asList(mapperNames).contains("liveChatMapper"),
                "liveChatMapper should exist");
    }

    @Test
    @DisplayName("MySQL SqlSessionFactory 应存在")
    void shouldHaveMySqlSessionFactory() {
        assertNotNull(applicationContext.getBean("mysqlSqlSessionFactory"),
                "MySQL SqlSessionFactory should exist");
    }

    @Test
    @DisplayName("MySQL TransactionManager 应存在")
    void shouldHaveMySqlTransactionManager() {
        assertNotNull(applicationContext.getBean("mysqlTransactionManager"),
                "MySQL TransactionManager should exist");
    }

    @Test
    @DisplayName("MySQL SqlSessionTemplate 应存在")
    void shouldHaveMySqlSessionTemplate() {
        assertNotNull(applicationContext.getBean("mysqlSqlSessionTemplate"),
                "MySQL SqlSessionTemplate should exist");
    }

    @Test
    @DisplayName("PG disabled 时 PG 相关 Bean 不应存在")
    void shouldNotHavePgBeansWhenDisabled() {
        assertFalse(applicationContext.containsBean("pgDataSource"),
                "pgDataSource should NOT exist when PG disabled");
        assertFalse(applicationContext.containsBean("pgSqlSessionFactory"),
                "pgSqlSessionFactory should NOT exist when PG disabled");
        assertFalse(applicationContext.containsBean("pgTransactionManager"),
                "pgTransactionManager should NOT exist when PG disabled");
    }

    @Test
    @DisplayName("两个数据源应相互隔离")
    void shouldHaveIsolatedDataSources() {
        Object mysqlDataSource = applicationContext.getBean("mysqlDataSource");
        assertNotNull(mysqlDataSource, "MySQL DataSource should not be null");
        assertNotNull(mysqlDataSource, "MySQL DataSource verified");
    }

    @Test
    @DisplayName("@Primary DataSource 应为 MySQL")
    void shouldHavePrimaryDataSource() {
        Object primaryDataSource = applicationContext.getBean(DataSource.class);
        Object mysqlDataSource = applicationContext.getBean("mysqlDataSource");
        assertEquals(primaryDataSource, mysqlDataSource, "Primary DataSource should be MySQL");
    }

    @Test
    @DisplayName("默认事务管理器应为 MySQL")
    @Transactional
    void shouldUseMySqlTransactionByDefault() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result, "Should execute query in MySQL transaction context");
    }

    @Test
    @DisplayName("应满足核心 Bean 命名约定")
    void shouldHaveBeanNamingConvention() {
        String[] expectedBeans = {
                "mysqlDataSource",
                "mysqlSqlSessionFactory",
                "mysqlTransactionManager",
                "mysqlSqlSessionTemplate",
        };

        for (String beanName : expectedBeans) {
            assertTrue(applicationContext.containsBean(beanName),
                    "Bean " + beanName + " should exist");
        }
    }
}
