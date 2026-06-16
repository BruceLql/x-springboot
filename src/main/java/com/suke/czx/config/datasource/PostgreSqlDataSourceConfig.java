package com.suke.czx.config.datasource;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * PostgreSQL 次要数据源配置
 * <p>
 * 仅在 spring.datasource.pg.enabled=true 时激活。
 * 启用后提供 pgDataSource / pgSqlSessionFactory / pgTransactionManager / pgJdbcTemplate。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.datasource.pg", name = "enabled", havingValue = "true")
@MapperScan(
        basePackages = {
                "com.suke.czx.modules.*.mapper.pg"
        },
        sqlSessionFactoryRef = "pgSqlSessionFactory"
)
public class PostgreSqlDataSourceConfig {

    @Bean(name = "pgDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.pg")
    public DataSource pgDataSource() {
        log.info("Initializing PostgreSQL secondary datasource...");
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "pgSqlSessionFactory")
    public SqlSessionFactory pgSqlSessionFactory(
            @Qualifier("pgDataSource") DataSource pgDataSource) throws Exception {
        log.info("Creating PostgreSQL SqlSessionFactory...");

        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(pgDataSource);

        // Mapper XML 位置（PostgreSQL 专用）
        try {
            factory.setMapperLocations(
                    new PathMatchingResourcePatternResolver().getResources("classpath:/mapper/pg/**/*Mapper.xml")
            );
        } catch (Exception e) {
            // 如果 pg 目录不存在，不设置 Mapper 位置
            log.warn("PG Mapper directory not found, proceeding without XML mappers");
        }

        factory.setTypeAliasesPackage("com.suke.czx.modules.*.entity.pg");

        com.baomidou.mybatisplus.core.MybatisConfiguration configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setCacheEnabled(false);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultEnumTypeHandler(com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler.class);
        factory.setConfiguration(configuration);

        com.baomidou.mybatisplus.core.config.GlobalConfig globalConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig();
        com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig dbConfig = new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig();
        dbConfig.setIdType(com.baomidou.mybatisplus.annotation.IdType.AUTO);
        dbConfig.setLogicDeleteField("deleted");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        factory.setGlobalConfig(globalConfig);

        return factory.getObject();
    }

    @Bean(name = "pgSqlSessionTemplate")
    public SqlSessionTemplate pgSqlSessionTemplate(
            @Qualifier("pgSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "pgTransactionManager")
    public DataSourceTransactionManager pgTransactionManager(
            @Qualifier("pgDataSource") DataSource pgDataSource) {
        log.info("Creating PostgreSQL transaction manager...");
        return new DataSourceTransactionManager(pgDataSource);
    }

    @Bean(name = "pgJdbcTemplate")
    public org.springframework.jdbc.core.JdbcTemplate pgJdbcTemplate(
            @Qualifier("pgDataSource") DataSource pgDataSource) {
        return new org.springframework.jdbc.core.JdbcTemplate(pgDataSource);
    }
}