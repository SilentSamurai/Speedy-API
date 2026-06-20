package com.github.silent.samurai.speedy.conversion.ext;

import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;

public interface SpeedyTypeModule {
    void contribute(ConversionContext ctx);
}
