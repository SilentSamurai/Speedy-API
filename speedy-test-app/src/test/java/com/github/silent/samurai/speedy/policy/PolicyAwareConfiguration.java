package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.config.SpeedyConfig;
import com.github.silent.samurai.speedy.conversion.ext.SpeedyTypeModule;
import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.ISpeedyRegistry;
import com.github.silent.samurai.speedy.interfaces.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModelProcessor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/// Test-only configuration that delegates every production concern to {@link SpeedyConfig} and
/// obtains the authorization context attached to the current MockMvc request.
record PolicyAwareConfiguration(SpeedyConfig delegate) implements ISpeedyConfiguration {

    @Override
    public MetaModelProcessor metaModelProcessor() {
        return delegate.metaModelProcessor();
    }

    @Override
    public void register(ISpeedyRegistry registry) {
        delegate.register(registry);
    }

    @Override
    public DataSource dataSourcePerReq() {
        return delegate.dataSourcePerReq();
    }

    @Override
    public SpeedyDialect getDialect() {
        return delegate.getDialect();
    }

    @Override
    public SpeedyBackend queryBackend(DataSource dataSource, SpeedyDialect dialect) {
        return delegate.queryBackend(dataSource, dialect);
    }

    @Override
    public List<SpeedyTypeModule> typeModules() {
        return delegate.typeModules();
    }

    @Override
    public Optional<SpeedyAuthContext> authContextPerReq() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        Object authContext = attributes.getRequest().getAttribute(PolicyTestConfiguration.AUTH_CONTEXT_ATTRIBUTE);
        return authContext instanceof SpeedyAuthContext context
                ? Optional.of(context)
                : Optional.empty();
    }
}
