package com.github.silent.samurai.speedy.json;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.conversion.ext.SpeedyTypeModule;
import com.github.silent.samurai.speedy.json.registry.JsonRegistry;

/// Contributes the default {@link JsonRegistry} to the {@link ConversionContext}.
/// Discovered via ServiceLoader so speedy-core never needs a compile-time
/// dependency on speedy-json-io.
public class JsonConversionModule implements SpeedyTypeModule {

    @Override
    public void contribute(ConversionContext ctx) {
        ctx.put(JsonRegistry.defaults());
    }
}
