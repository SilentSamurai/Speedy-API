package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.interfaces.query.ConversionProcessor;
import com.github.silent.samurai.speedy.models.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

public class JooqConversionImpl implements ConversionProcessor<OffsetDateTime, Time,
        Timestamp, Date, BigDecimal, BigDecimal, Double, BigInteger, String, String, String, String, UUID> {


    @Override
    public OffsetDateTime toZonedTimestamp(SpeedyZonedDateTime value) {
        return OffsetDateTime.of(value.asZonedDateTime().toLocalDateTime(), value.asZonedDateTime().getOffset());
    }

    @Override
    public Time toTime(SpeedyTime speedyTime) {
        return Time.valueOf(speedyTime.asTime());
    }

    @Override
    public Timestamp toTimeStamp(SpeedyDateTime speedyDateTime) {
        return Timestamp.valueOf(speedyDateTime.asDateTime());
    }

    @Override
    public Date toDate(SpeedyDate speedyDate) {
        return Date.valueOf(speedyDate.asDate());
    }

    @Override
    public BigDecimal toNumeric(SpeedyDouble speedyDouble) {
        return BigDecimal.valueOf(speedyDouble.asDouble());
    }

    @Override
    public BigDecimal toDecimal(SpeedyDouble speedyDouble) {
        return BigDecimal.valueOf(speedyDouble.asDouble());
    }

    @Override
    public Double toReal(SpeedyDouble speedyDouble) {
        return speedyDouble.asDouble();
    }

    @Override
    public BigInteger toBigInt(SpeedyInt speedyInt) {
        return BigInteger.valueOf(speedyInt.asLong());
    }

    @Override
    public String toText(SpeedyText speedyText) {
        return speedyText.asText();
    }

    @Override
    public String toClob(SpeedyText speedyText) {
        return speedyText.asText();
    }

    @Override
    public String toVarchar(SpeedyText speedyText) {
        return speedyText.asText();
    }

    @Override
    public String toChar(SpeedyText speedyText) {
        return speedyText.asText();
    }

    @Override
    public UUID toUuid(SpeedyText speedyText) {
        return UUID.fromString(speedyText.asText());
    }
}
