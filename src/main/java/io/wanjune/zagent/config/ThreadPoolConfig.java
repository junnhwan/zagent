package io.wanjune.zagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置, 用于异步任务执行（并行数据加载、SSE流式推送等）
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 从application.yml加载线程池配置参数
     *
     * @return 线程池配置属性对象
     */
    @Bean("threadPoolExecutor")
    @ConfigurationProperties(prefix = "thread.pool.executor.config")
    public ThreadPoolConfigProperties threadPoolConfigProperties() {
        return new ThreadPoolConfigProperties();
    }

    /**
     * 创建ThreadPoolExecutor, 默认: core=20, max=50, CallerRunsPolicy拒绝策略
     *
     * @param props 线程池配置属性
     * @return ThreadPoolExecutor线程池实例
     */
    @Bean("executorService")
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigProperties props) {
        return new ThreadPoolExecutor(
                props.getCorePoolSize(),
                props.getMaxPoolSize(),
                props.getKeepAliveTime(),
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(props.getBlockQueueSize()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 线程池配置属性类, 从application.yml中读取配置
     * <p>配置前缀: thread.pool.executor.config</p>
     */
    @lombok.Data
    public static class ThreadPoolConfigProperties {
        /** 核心线程数, 默认20 */
        private int corePoolSize = 20;
        /** 最大线程数, 默认50 */
        private int maxPoolSize = 50;
        /** 线程空闲存活时间(ms), 默认5000 */
        private long keepAliveTime = 5000;
        /** 阻塞队列大小, 默认5000 */
        private int blockQueueSize = 5000;
        /** 拒绝策略, 默认CallerRunsPolicy */
        private String policy = "CallerRunsPolicy";
    }

}
