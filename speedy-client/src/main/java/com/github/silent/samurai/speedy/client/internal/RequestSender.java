package com.github.silent.samurai.speedy.client.internal;

import com.github.silent.samurai.speedy.client.transport.SpeedyRawResponse;
import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;

import java.io.IOException;

@FunctionalInterface
public interface RequestSender {
    SpeedyRawResponse send(SpeedyRequest request) throws IOException;
}
