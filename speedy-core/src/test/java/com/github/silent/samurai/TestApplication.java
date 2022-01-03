package com.github.silent.samurai;

import com.github.silent.samurai.metamodel.JpaMetaModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import javax.persistence.EntityManagerFactory;

@SpringBootApplication
public class TestApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Bean
    public JpaMetaModel jpaMetaModel() {
        return new JpaMetaModel();
    }

    @Bean
    public SpeedyFactory speedyFactory(EntityManagerFactory entityManagerFactory, JpaMetaModel jpaMetaModel) {
        return new SpeedyFactory(entityManagerFactory, jpaMetaModel);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TestApplication.class);
    }
}
