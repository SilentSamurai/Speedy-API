package com.github.silent.samurai;

import com.github.silent.samurai.controllers.SpeedyApiController;
import com.github.silent.samurai.docs.OpenApiGenerator;
import com.github.silent.samurai.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.interfaces.ISpeedyOpenApiConfiguration;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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
    @ConditionalOnBean(ISpeedyOpenApiConfiguration.class)
    public OpenApiCustomiser customerGlobalHeaderOpenApiCustomizer(SpeedyFactory speedyFactory) {
        OpenApiGenerator openApiGenerator = new OpenApiGenerator(speedyFactory.getMetaModelProcessor());
        return openApiGenerator::generate;
    }


}
