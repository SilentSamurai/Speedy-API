package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.docs.OpenApiGenerator;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "speedy.api.docs")
public class SpeedyApiDocumentationAutoConfiguration {

    @Bean
    public OpenApiCustomizer customerGlobalHeaderOpenApiCustomizer(SpeedyFactory speedyFactory) {
        OpenApiGenerator openApiGenerator = new OpenApiGenerator(speedyFactory);
        return openApiGenerator::generate;
    }


}
