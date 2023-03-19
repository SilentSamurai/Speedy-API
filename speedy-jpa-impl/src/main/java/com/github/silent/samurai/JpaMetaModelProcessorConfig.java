package com.github.silent.samurai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@Configuration
public class JpaMetaModelProcessorConfig {

    @Bean
    JpaMetaModelProcessor getBean(EntityManagerFactory entityManagerFactor) {
        return new JpaMetaModelProcessor(entityManagerFactor);
    }
}
