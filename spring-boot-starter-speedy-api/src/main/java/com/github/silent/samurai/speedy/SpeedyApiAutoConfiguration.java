package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.controllers.SpeedyApiController;
import com.github.silent.samurai.speedy.docs.SpeedyOpenApiCustomizer;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Setter
@Getter
@AutoConfiguration
@Configuration
@ConfigurationProperties(prefix = "speedy.api")
@ComponentScan(basePackageClasses = SpeedyApiController.class)
public class SpeedyApiAutoConfiguration {

    private DataSize maxRequestBodySize = DataSize.ofMegabytes(1);
    private int maxQueryStringLength = 2048;
    private int maxFilterCount = 100;

    @Bean
    @ConditionalOnBean(ISpeedyConfiguration.class)
    public SpeedyFactory speedyFactory(ISpeedyConfiguration speedyConfiguration) throws SpeedyHttpException {
        return new SpeedyFactory(speedyConfiguration, maxRequestBodySize.toBytes());
    }

    @Bean
    @ConditionalOnBean(SpeedyFactory.class)
    public SpeedyOpenApiCustomizer speedyOpenApiCustomizer(SpeedyFactory speedyFactory) {
        return new SpeedyOpenApiCustomizer(speedyFactory);
    }

}
