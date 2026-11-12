package co.eci.c15.salas.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableJpaRepositories(basePackages = "co.eci.c15.salas.infrastructure.persistence")
@EntityScan(basePackages = "co.eci.c15.salas.infrastructure.persistence")
public class SalasConfig {

    /** Programa la cuenta regresiva del arranque de las partidas. */
    @Bean(name = "salasScheduler")
    public ThreadPoolTaskScheduler salasScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("salas-");
        return scheduler;
    }
}