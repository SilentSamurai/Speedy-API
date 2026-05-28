package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.controllers.SpeedyApiController;
import com.github.silent.samurai.speedy.docs.SpeedyOpenApiCustomizer;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@AutoConfiguration
@Configuration
@ConfigurationProperties(prefix = "speedy.api")
@ComponentScan(basePackageClasses = SpeedyApiController.class)
public class SpeedyApiAutoConfiguration {

    private DataSize maxRequestBodySize = DataSize.ofMegabytes(1);

    public DataSize getMaxRequestBodySize() {
        return maxRequestBodySize;
    }

    public void setMaxRequestBodySize(DataSize maxRequestBodySize) {
        this.maxRequestBodySize = maxRequestBodySize;
    }

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
