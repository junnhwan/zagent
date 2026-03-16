package io.wanjune.zagent.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PgVector 向量数据库数据源配置, 用于RAG知识库的向量存储
 */
@Configuration
public class PgVectorDataSourceConfig {

    /**
     * 创建PostgreSQL+PgVector数据源（HikariCP连接池）
     *
     * @param driverClassName  JDBC驱动类名
     * @param url              数据库连接URL
     * @param username         数据库用户名
     * @param password         数据库密码
     * @param maximumPoolSize  最大连接池大小, 默认5
     * @param minimumIdle      最小空闲连接数, 默认2
     * @param idleTimeout      空闲连接超时时间(ms), 默认30000
     * @param connectionTimeout 连接超时时间(ms), 默认30000
     * @return PgVector HikariCP数据源
     */
    @Bean("pgVectorDataSource")
    public DataSource pgVectorDataSource(
            @Value("${spring.datasource.pgvector.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.pgvector.url}") String url,
            @Value("${spring.datasource.pgvector.username}") String username,
            @Value("${spring.datasource.pgvector.password}") String password,
            @Value("${spring.datasource.pgvector.hikari.maximum-pool-size:5}") int maximumPoolSize,
            @Value("${spring.datasource.pgvector.hikari.minimum-idle:2}") int minimumIdle,
            @Value("${spring.datasource.pgvector.hikari.idle-timeout:30000}") long idleTimeout,
            @Value("${spring.datasource.pgvector.hikari.connection-timeout:30000}") long connectionTimeout) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setConnectionTimeout(connectionTimeout);
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setAutoCommit(true);
        dataSource.setPoolName("PgVectorHikariPool");
        return dataSource;
    }

    /**
     * 创建PgVector专用的JdbcTemplate
     *
     * @param dataSource PgVector数据源
     * @return PgVector专用JdbcTemplate实例
     */
    @Bean("pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
