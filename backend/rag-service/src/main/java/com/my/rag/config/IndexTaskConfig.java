package com.my.rag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class IndexTaskConfig {

    @Bean
    public TaskExecutor indexTaskExecutor(RagProperties ragProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int workerThreads = Math.max(1, ragProperties.getIndex().getWorkerThreads());
        executor.setCorePoolSize(workerThreads);
        executor.setMaxPoolSize(workerThreads);
        executor.setQueueCapacity(Math.max(1, ragProperties.getIndex().getQueueCapacity()));
        executor.setThreadNamePrefix("rag-index-");
        executor.initialize();
        return executor;
    }
}
