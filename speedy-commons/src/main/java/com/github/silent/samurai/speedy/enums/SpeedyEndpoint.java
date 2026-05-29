package com.github.silent.samurai.speedy.enums;

public enum SpeedyEndpoint {
    QUERY("$query"),
    CREATE("$create"),
    UPDATE("$update"),
    DELETE("$delete"),
    METADATA("$metadata");

    private final String suffix;

    SpeedyEndpoint(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }

    public String path() {
        return "/" + suffix;
    }

    public static SpeedyEndpoint fromSuffix(String suffix) {
        for (SpeedyEndpoint ep : values()) {
            if (ep.suffix.equals(suffix)) {
                return ep;
            }
        }
        return null;
    }
}
