package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.config.SpeedyConfig;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class PolicyTestConfiguration {

    static final String AUTH_CONTEXT_ATTRIBUTE = PolicyTestConfiguration.class.getName() + ".authContext";

    @Bean
    @Primary
    ISpeedyConfiguration policyAwareConfiguration(SpeedyConfig delegate) {
        return new PolicyAwareConfiguration(delegate);
    }
}
