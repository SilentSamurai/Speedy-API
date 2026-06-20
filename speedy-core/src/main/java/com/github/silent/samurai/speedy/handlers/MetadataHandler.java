package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.models.SpeedyMetadataResponse;
import com.github.silent.samurai.speedy.context.SpeedyContext;

public class MetadataHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        ISpeedyConfiguration config = context.get(ISpeedyConfiguration.class);
        if (!config.isMetadataEndpointEnabled()) {
            throw new NotFoundException("Metadata endpoint is disabled");
        }

        MetaModel metaModel = context.get(MetaModel.class);
        context.put(SpeedyResponse.class,
                SpeedyMetadataResponse.builder()
                        .metaModel(metaModel)
                        .status(200)
                        .build());
    }
}
