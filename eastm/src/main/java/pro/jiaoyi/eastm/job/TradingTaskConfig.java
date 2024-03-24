package pro.jiaoyi.eastm.job;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TradingTaskConfig {
    @Bean("tradingTaskExecutor")
    public ThreadPoolTaskExecutor tradingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 配置线程池的属性，例如核心线程数、最大线程数、队列容量等
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mk-");
        executor.initialize();
        return executor;
    }
}
