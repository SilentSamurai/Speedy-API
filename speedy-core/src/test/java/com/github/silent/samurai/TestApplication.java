package com.github.silent.samurai;

import com.github.silent.samurai.metamodel.JpaMetaModel;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@SpringBootApplication
public class TestApplication {

    @Bean
    public DataSource speedyFactory() {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url("jdbc:h2:mem:testdb");
        dataSourceBuilder.username("sa");
        dataSourceBuilder.password("password");
        dataSourceBuilder.driverClassName("org.h2.Driver");
        return dataSourceBuilder.build();
    }

    @Bean
    public JpaMetaModel jpaMetaModel() {
        return new JpaMetaModel();
    }

    @Bean
    public SpeedyFactory speedyFactory(EntityManagerFactory entityManagerFactory, JpaMetaModel jpaMetaModel) {
        return new SpeedyFactory(entityManagerFactory, jpaMetaModel);
    }
}
