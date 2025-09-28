package com.github.silent.samurai.speedy.models;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DynamicEnum {

    private final Variant[] variants;
    private final Map<String, Variant> byName;
    private final Map<Integer, Variant> byCode;

    public DynamicEnum(Variant[] variants) {
        this.variants = variants;
        this.byName = Arrays.stream(variants)
                .collect(Collectors.toUnmodifiableMap(Variant::name, v -> v));
        this.byCode = Arrays.stream(variants)
                .collect(Collectors.toUnmodifiableMap(Variant::code, v -> v));
    }

    public static DynamicEnum of(Class<? extends Enum<?>> enumClass) {
        Enum<?>[] enums = enumClass.getEnumConstants();
        Variant[] variants = new Variant[enums.length];
        for (int i = 0; i < enums.length; i++) {
            variants[i] = new Variant(enums[i].name(), enums[i].ordinal());
        }
        return new DynamicEnum(variants);
    }

    public Optional<Variant> fromName(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public Optional<Variant> fromCode(int code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public List<Variant> values() {
        return List.of(variants);
    }

    public record Variant(String name, int code) {
    }
}
