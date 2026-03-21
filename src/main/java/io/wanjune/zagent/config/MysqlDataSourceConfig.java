package io.wanjune.zagent.config;

import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * MySQL 主数据源配置, 包含 HikariCP 连接池、MyBatis SqlSessionFactory 和 SqlSessionTemplate
 */
@Configuration
public class MysqlDataSourceConfig {

    /**
     * 创建MySQL主数据源（HikariCP连接池）
     *
     * @param driverClassName  JDBC驱动类名
     * @param url              数据库连接URL
     * @param username         数据库用户名
     * @param password         数据库密码
     * @param maximumPoolSize  最大连接池大小, 默认10
     * @param minimumIdle      最小空闲连接数, 默认5
     * @param idleTimeout      空闲连接超时时间(ms), 默认30000
     * @param connectionTimeout 连接超时时间(ms), 默认30000
     * @param maxLifetime      连接最大存活时间(ms), 默认1800000
     * @return MySQL HikariCP数据源
     */
    @Bean("mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource(
            @Value("${spring.datasource.mysql.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.mysql.url}") String url,
            @Value("${spring.datasource.mysql.username}") String username,
            @Value("${spring.datasource.mysql.password}") String password,
            @Value("${spring.datasource.mysql.hikari.maximum-pool-size:10}") int maximumPoolSize,
            @Value("${spring.datasource.mysql.hikari.minimum-idle:5}") int minimumIdle,
            @Value("${spring.datasource.mysql.hikari.idle-timeout:30000}") long idleTimeout,
            @Value("${spring.datasource.mysql.hikari.connection-timeout:30000}") long connectionTimeout,
            @Value("${spring.datasource.mysql.hikari.max-lifetime:1800000}") long maxLifetime) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setConnectionTimeout(connectionTimeout);
        dataSource.setMaxLifetime(maxLifetime);
        dataSource.setPoolName("MysqlHikariPool");
        return dataSource;
    }

    /**
     * 创建MyBatis SqlSessionFactory, 加载mapper/*.xml映射文件, 开启驼峰命名转换
     *
     * @param mysqlDataSource MySQL主数据源
     * @return SqlSessionFactoryBean实例
     * @throws Exception 创建过程中的异常
     */
    @Bean("sqlSessionFactory")
    public SqlSessionFactoryBean sqlSessionFactory(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(mysqlDataSource);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factory.setMapperLocations(resolver.getResources("classpath:mapper/*.xml"));

        // MyBatis settings
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        return factory;
    }

    /**
     * 创建MyBatis SqlSessionTemplate
     *
     * @param sqlSessionFactory SqlSessionFactoryBean实例
     * @return SqlSessionTemplate实例
     * @throws Exception 创建过程中的异常
     */
    @Bean("sqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactoryBean sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(Objects.requireNonNull(sqlSessionFactory.getObject()));
    }

    @Bean("mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        return new JdbcTemplate(mysqlDataSource);
    }

}
