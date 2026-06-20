package com.github.silent.samurai.speedy.config;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.conversion.ext.SpeedyTypeModule;
import com.github.silent.samurai.speedy.types.Email;

public class EmailTypeModule implements SpeedyTypeModule {

    @Override
    public void contribute(ConversionContext ctx) {
        ctx.forType(Email.class)
                .asText(Email::toString, Email::new);
    }
}
