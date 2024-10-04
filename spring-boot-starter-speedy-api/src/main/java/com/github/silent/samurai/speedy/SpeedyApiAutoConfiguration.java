package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.controllers.SpeedyApiController;
import com.github.silent.samurai.speedy.docs.SpeedyOpenApiCustomizer;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@Configuration
@ConfigurationProperties(prefix = "speedy.api")
@ComponentScan(basePackageClasses = SpeedyApiController.class)
public class SpeedyApiAutoConfiguration {


    @Bean
    @ConditionalOnBean(ISpeedyConfiguration.class)
    public SpeedyFactory speedyFactory(ISpeedyConfiguration speedyConfiguration) {
        return new SpeedyFactory(speedyConfiguration);
    }

    @Bean
    @ConditionalOnBean(SpeedyFactory.class)
    public SpeedyOpenApiCustomizer speedyOpenApiCustomizer(SpeedyFactory speedyFactory) {
        return new SpeedyOpenApiCustomizer(speedyFactory);
    }

}
