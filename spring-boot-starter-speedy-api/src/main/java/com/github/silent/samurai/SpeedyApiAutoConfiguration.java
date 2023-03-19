package com.github.silent.samurai;

import com.github.silent.samurai.controllers.SpeedyApiController;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@Configuration
@ConfigurationProperties(prefix = "speedy.api")
@ComponentScan(basePackageClasses = SpeedyApiController.class)
public class SpeedyApiAutoConfiguration {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    MetaModelProcessor metaModelProcessor;


    @Bean
    public SpeedyFactory speedyFactory() {
        return new SpeedyFactory(entityManagerFactory, metaModelProcessor);
    }


}
