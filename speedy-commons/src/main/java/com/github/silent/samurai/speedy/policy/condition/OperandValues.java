package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyDouble;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.github.silent.samurai.speedy.models.SpeedyText;

/// Wraps a plain Java value (as typically found in a principal's claims map or a parsed policy
/// document) into a {@link SpeedyValue}. Deliberately small and dependency-free — this is not a
/// general Java&lt;-&gt;Speedy converter (that lives in speedy-core), just enough for the simple
/// scalar types condition operands carry.
final class OperandValues {

    private OperandValues() {
    }

    static SpeedyValue wrap(Object raw) {
        if (raw == null) {
            return SpeedyNull.SPEEDY_NULL;
        }
        if (raw instanceof SpeedyValue speedyValue) {
            return speedyValue;
        }
        if (raw instanceof String s) {
            return new SpeedyText(s);
        }
        if (raw instanceof Boolean b) {
            return new SpeedyBoolean(b);
        }
        if (raw instanceof Long l) {
            return new SpeedyInt(l);
        }
        if (raw instanceof Integer i) {
            return new SpeedyInt(i.longValue());
        }
        if (raw instanceof Double d) {
            return new SpeedyDouble(d);
        }
        if (raw instanceof Float f) {
            return new SpeedyDouble(f.doubleValue());
        }
        return new SpeedyText(String.valueOf(raw));
    }
}
