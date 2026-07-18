package com.github.silent.samurai.speedy;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.controllers.SpeedyApiController;
import com.github.silent.samurai.speedy.docs.SpeedyOpenApiCustomizer;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModelProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Exercises the @ConditionalOnBean annotations on the real SpeedyApiAutoConfiguration
/// via @SpringBootTest, covering both the positive and negative bean-creation paths.
///
/// ## What we test
/// SpeedyApiAutoConfiguration declares three conditional beans:
///   SpeedyFactory            → @ConditionalOnBean(ISpeedyConfiguration.class)
///   SpeedyApiController       → @ConditionalOnBean(SpeedyFactory.class)
///   SpeedyOpenApiCustomizer  → @ConditionalOnBean(SpeedyFactory.class)
///
/// When ISpeedyConfiguration is present, all three beans are created. When it is absent,
/// none of them is created.
///
/// ## How we test
/// Two @Nested inner classes each boot their own minimal Spring Boot app via
///
/// @SpringBootTest. Both apps load the real SpeedyApiAutoConfiguration through
/// @EnableAutoConfiguration (picked up from AutoConfiguration.imports).
///
/// Positive: the app provides a mock ISpeedyConfiguration (stubbed to return safe
///   defaults for SpeedyFactory constructor). The real @ConditionalOnBean conditions
///   are exercised directly on the real auto-config class — no proxy duplication.
///
/// Negative: the app does NOT provide ISpeedyConfiguration, so none of the conditional
///   Speedy beans are created.
class SpeedyApiAutoConfigurationConditionalTest {

    @Nested
    @SpringBootTest(
            classes = Positive.Configured.class,
            properties = "spring.autoconfigure.exclude="
                    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    )
    class Positive {

        @Autowired
        SpeedyFactory speedyFactory;
        @Autowired
        SpeedyOpenApiCustomizer speedyOpenApiCustomizer;
        @Autowired
        SpeedyApiController speedyApiController;

        @Test
        @DisplayName("Speedy beans are created when ISpeedyConfiguration is present")
        void allConditionalBeansCreated() {
            assertThat(speedyFactory).isNotNull();
            assertThat(speedyOpenApiCustomizer).isNotNull();
            assertThat(speedyApiController).isNotNull();
        }

        @SpringBootConfiguration
        @EnableAutoConfiguration
        static class Configured {
            @Bean
            ISpeedyConfiguration speedyConfiguration() {
                ISpeedyConfiguration config = mock(ISpeedyConfiguration.class);
                MetaModelProcessor proc = mock(MetaModelProcessor.class);
                MetaModel model = mock(MetaModel.class);
                when(model.getAllEntityMetadata()).thenReturn(Collections.emptyList());
                when(proc.getMetaModel()).thenReturn(model);
                when(config.metaModelProcessor()).thenReturn(proc);
                when(config.getDialect()).thenReturn(SpeedyDialect.H2);
                return config;
            }
        }
    }

    @Nested
    @SpringBootTest(
            classes = Negative.Unconfigured.class,
            properties = "spring.autoconfigure.exclude="
                    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    )
    class Negative {

        @Autowired(required = false)
        SpeedyFactory speedyFactory;
        @Autowired(required = false)
        SpeedyOpenApiCustomizer speedyOpenApiCustomizer;
        @Autowired(required = false)
        SpeedyApiController speedyApiController;

        @Test
        @DisplayName("Speedy beans are not created when ISpeedyConfiguration is absent")
        void noConditionalBeansCreated() {
            assertThat(speedyFactory).isNull();
            assertThat(speedyOpenApiCustomizer).isNull();
            assertThat(speedyApiController).isNull();
        }

        @SpringBootConfiguration
        @EnableAutoConfiguration
        static class Unconfigured {
        }
    }
}
