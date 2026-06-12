package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyText;

import java.util.function.Function;

public class TypeBuilder<T> {

    private final ConversionContext ctx;
    private final Class<T> type;

    TypeBuilder(ConversionContext ctx, Class<T> type) {
        this.ctx = ctx;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public TypeBuilder<T> asText(Function<T, String> enc, Function<String, T> dec) {
        if (ctx.has(JavaTypeRegistry.class)) {
            JavaTypeRegistry jtr = ctx.get(JavaTypeRegistry.class);
            jtr.register(type,
                    sv -> dec.apply(sv.asText()),
                    raw -> new SpeedyText(enc.apply((T) raw)));
        }
        if (ctx.has(JsonRegistry.class)) {
            JsonRegistry jr = ctx.get(JsonRegistry.class);
            jr.register(ValueType.TEXT,
                    sv -> sv.asText(),
                    raw -> new SpeedyText(String.valueOf(raw)));
        }
        return this;
    }

    public TypeBuilder<T> onDb(ColumnType col,
                                Function<SpeedyValue, Object> enc,
                                Function<Object, SpeedyValue> dec) {
        if (ctx.has(DbConversionRegistry.class)) {
            ctx.get(DbConversionRegistry.class).register(col, enc, dec);
        }
        return this;
    }

    public TypeBuilder<T> onJson(ValueType vt,
                                  Function<SpeedyValue, Object> enc,
                                  Function<Object, SpeedyValue> dec) {
        if (ctx.has(JsonRegistry.class)) {
            ctx.get(JsonRegistry.class).register(vt, enc, dec);
        }
        return this;
    }

    public TypeBuilder<T> onJava(ValueType vt,
                                  Function<SpeedyValue, Object> enc,
                                  Function<Object, SpeedyValue> dec) {
        if (ctx.has(JavaTypeRegistry.class)) {
            ctx.get(JavaTypeRegistry.class).register(type, vt, enc, dec);
        }
        return this;
    }

}
