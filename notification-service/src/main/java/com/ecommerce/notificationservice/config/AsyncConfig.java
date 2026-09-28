package com.ecommerce.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool used by channel adapters so that dispatching to a slow external provider
 * (SMTP, SMS gateway, push service) does not block the RabbitMQ listener threads.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "notificationDispatchExecutor")
    Executor notificationDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notif-dispatch-");
        executor.initialize();
        return executor;
    }
}
