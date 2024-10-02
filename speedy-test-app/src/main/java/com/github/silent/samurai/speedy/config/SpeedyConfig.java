package com.github.silent.samurai.speedy.config;

import com.github.silent.samurai.speedy.entity.VirtualEntity;
import com.github.silent.samurai.speedy.events.EntityEvents;
import com.github.silent.samurai.speedy.events.VirtualEntityHandler;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.ISpeedyRegistry;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.jpa.impl.processors.JpaMetaModelProcessor;
import com.github.silent.samurai.speedy.validation.SpeedyValidation;
import org.jooq.SQLDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyValidation speedyValidation;

    @Autowired
    EntityEvents entityEvents;

    @Autowired
    VirtualEntityHandler virtualEntityHandler;

    @Autowired
    DataSource dataSource;

    @Override
    public MetaModelProcessor createMetaModelProcessor() {
        return new JpaMetaModelProcessor(this, entityManagerFactory);
    }

    @Override
    public void register(ISpeedyRegistry registry) {
        registry.registerEventHandler(entityEvents)
                .registerVirtualEntityHandler(virtualEntityHandler, VirtualEntity.class)
                .registerValidator(speedyValidation);
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String getDialect() {
        return SQLDialect.H2.getName();
    }
}
