package com.github.silent.samurai.speedy.config;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.docs.SpeedyOpenApiCustomizer;
import com.github.silent.samurai.speedy.events.EntityEvents;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.ISpeedyRegistry;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.jpa.impl.processors.JpaMetaModelProcessor;
import com.github.silent.samurai.speedy.jpa.impl.processors.JpaMetaModelProcessorV2;
import com.github.silent.samurai.speedy.validation.SpeedyValidation;
import jakarta.persistence.EntityManagerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final SpeedyValidation speedyValidation;
    private final EntityEvents entityEvents;
    private final DataSource dataSource;
    private final Environment environment;

    public SpeedyConfig(EntityManagerFactory entityManagerFactory, SpeedyValidation speedyValidation, EntityEvents entityEvents, DataSource dataSource, Environment environment) {
        this.entityManagerFactory = entityManagerFactory;
        this.speedyValidation = speedyValidation;
        this.entityEvents = entityEvents;
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Bean
    public OpenApiCustomizer customizer(SpeedyOpenApiCustomizer speedyOpenApiCustomizer) {
        return speedyOpenApiCustomizer::generate;
    }

    @Override
    public MetaModelProcessor metaModelProcessor() {
        return new JpaMetaModelProcessorV2(this, entityManagerFactory);
    }

    @Override
    public void register(ISpeedyRegistry registry) {
        registry.registerEventHandler(entityEvents)
                .registerValidator(speedyValidation);
    }

    @Override
    public DataSource dataSourcePerReq() {
        return dataSource;
    }

    @Override
    public SpeedyDialect getDialect() {
        Set<String> profiles = new HashSet<>(Arrays.asList(environment.getActiveProfiles()));
        if (profiles.contains("postgres")) {
            return SpeedyDialect.POSTGRES;
        } else if (profiles.contains("mysql")) {
            return SpeedyDialect.MYSQL;
        }
        return SpeedyDialect.H2;
    }
}
