package io.wanjune.zagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ZAgent AI Agent平台启动类
 *
 * @author zagent
 */
@EnableScheduling
@SpringBootApplication
public class ZAgentApplication {

    /**
     * 应用程序入口方法
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ZAgentApplication.class, args);
    }

}
