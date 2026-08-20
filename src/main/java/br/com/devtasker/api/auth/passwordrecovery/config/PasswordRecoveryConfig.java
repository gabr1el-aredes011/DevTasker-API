package br.com.devtasker.api.auth.passwordrecovery.config;

import java.time.Clock;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PasswordRecoveryProperties.class)
public class PasswordRecoveryConfig {

    @Bean
    Clock passwordRecoveryClock() {
        return Clock.systemUTC();
    }

    @Bean
    Executor passwordRecoveryEmailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("password-recovery-mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.AbortPolicy()
        );
        executor.initialize();
        return executor;
    }
}
