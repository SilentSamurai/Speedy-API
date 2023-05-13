package com.github.silent.samurai.speedy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class TestApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

//    @Bean
//    public JpaMetaModelProcessor jpaMetaModel(EntityManagerFactory entityManagerFactory) {
//        return new JpaMetaModelProcessor(entityManagerFactory);
//    }
//
//    @Bean
//    public SpeedyFactory speedyFactory(EntityManagerFactory entityManagerFactory, JpaMetaModelProcessor jpaMetaModelProcessor) {
//        return new SpeedyFactory(entityManagerFactory, jpaMetaModelProcessor);
//    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TestApplication.class);
    }
}
