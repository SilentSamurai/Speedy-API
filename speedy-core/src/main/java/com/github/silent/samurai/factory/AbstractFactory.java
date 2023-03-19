package com.github.silent.samurai.factory;

import com.github.silent.samurai.interfaces.IResponseSerializer;
import com.github.silent.samurai.interfaces.ResponseReturningRequestContext;
import com.github.silent.samurai.serializers.json.JSONSerializer;
import lombok.Getter;

@Getter
public class AbstractFactory {

    private ServiceFactory<IResponseSerializer, ResponseReturningRequestContext> serializerFactory = new ServiceFactory<>();


    static {
        getInstance().getSerializerFactory().addService("JSON", JSONSerializer::new);
    }


    private static AbstractFactory instance;

    public static AbstractFactory getInstance() {
        if (instance == null) {
            instance = new AbstractFactory();
        }
        return instance;
    }


}
