package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.enums.EtagStrategy;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.models.*;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.HexFormat;

/// Generates the fresh value Speedy stamps into a {@code @SpeedyETag} field on every
/// create/update/replace, per the field's declared {@link EtagStrategy}. No DB read is involved —
/// each strategy computes its next value independently of what was previously stored.
public final class EtagManager {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private EtagManager() {
    }

    public static SpeedyValue freshValue(FieldMetadata etagField) {
        return switch (etagField.getEtagStrategy().get()) {
            case RANDOM -> new SpeedyText(randomHash());
            case TIMESTAMP -> freshTimestamp(etagField);
        };
    }

    private static String randomHash() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static SpeedyValue freshTimestamp(FieldMetadata etagField) {
        return switch (etagField.getValueType()) {
            case DATE_TIME -> new SpeedyDateTime(LocalDateTime.now());
            case ZONED_DATE_TIME -> new SpeedyZonedDateTime(ZonedDateTime.now());
            case DATE -> new SpeedyDate(LocalDate.now());
            case TIME -> new SpeedyTime(LocalTime.now());
            // Unreachable in a correctly configured app: MetaModelVerifier rejects a TIMESTAMP
            // strategy on a non-temporal column at startup.
            default -> throw new IllegalStateException(
                    "@SpeedyETag(TIMESTAMP) on '" + etagField.getOutputPropertyName()
                            + "' requires a temporal column, found " + etagField.getValueType());
        };
    }
}
