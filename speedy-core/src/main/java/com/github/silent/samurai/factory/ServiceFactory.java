package com.github.silent.samurai.factory;

import com.github.silent.samurai.interfaces.RequestContext;

import java.util.HashMap;
import java.util.Map;

public class ServiceFactory<T, C> {

    @FunctionalInterface
    static interface ServiceSupplier<T, C> {
        T get(C context);
    }

    private Map<String, ServiceSupplier<T, C>> serializerMap = new HashMap<>();

    public T createService(String format, C context) {
        ServiceSupplier<T, C> supplier = serializerMap.get(format);
        return supplier.get(context);
    }

    public void addService(String format, ServiceSupplier<T, C> serializer) {
        serializerMap.put(format, serializer);
    }
}
