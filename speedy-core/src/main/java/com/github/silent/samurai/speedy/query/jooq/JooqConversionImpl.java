package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.mappings.ConversionFactory;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.Speedy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

public class JooqConversionImpl extends ConversionFactory<OffsetDateTime, Time,
        Timestamp, Date, BigDecimal, BigDecimal, Double, BigInteger, String, String, String, String, UUID> {


    @Override
    public OffsetDateTime toZonedTimestamp(SpeedyZonedDateTime value) {
        return OffsetDateTime.of(value.asZonedDateTime().toLocalDateTime(), value.asZonedDateTime().getOffset());
    }

    @Override
    public SpeedyZonedDateTime fromZonedTimestamp(OffsetDateTime offsetDateTime) {
        return Speedy.from(offsetDateTime.toZonedDateTime());
    }

    @Override
    public Time toTime(SpeedyTime speedyTime) {
        return Time.valueOf(speedyTime.asTime());
    }

    @Override
    public SpeedyTime fromTime(Time time) {
        return Speedy.from(time.toLocalTime());
    }

    @Override
    public Timestamp toTimeStamp(SpeedyDateTime speedyDateTime) {
        return Timestamp.valueOf(speedyDateTime.asDateTime());
    }

    @Override
    public SpeedyDateTime fromTimeStamp(Timestamp timestamp) {
        return Speedy.from(timestamp.toLocalDateTime());
    }

    @Override
    public Date toDate(SpeedyDate speedyDate) {
        return Date.valueOf(speedyDate.asDate());
    }

    @Override
    public SpeedyDate fromDate(Date date) {
        return Speedy.from(date.toLocalDate());
    }

    @Override
    public BigDecimal toNumeric(SpeedyDouble speedyDouble) {
        return BigDecimal.valueOf(speedyDouble.asDouble());
    }

    @Override
    public SpeedyDouble fromNumeric(BigDecimal numeric) {
        return Speedy.from(numeric.doubleValue());
    }

    @Override
    public BigDecimal toDecimal(SpeedyDouble speedyDouble) {
        return BigDecimal.valueOf(speedyDouble.asDouble());
    }

    @Override
    public SpeedyDouble fromDecimal(BigDecimal decimal) {
        return Speedy.from(decimal.doubleValue());
    }

    @Override
    public Double toReal(SpeedyDouble speedyDouble) {
        return speedyDouble.asDouble();
    }

    @Override
    public SpeedyDouble fromReal(Double real) {
        return Speedy.from(real);
    }

    @Override
    public BigInteger toBigInt(SpeedyInt speedyInt) {
        return BigInteger.valueOf(speedyInt.asLong());
    }

    @Override
    public SpeedyInt fromBigInt(BigInteger bigInt) {
        return Speedy.from(bigInt.longValue());
    }

    @Override
    public String toText(SpeedyText speedyText) {
        return speedyText.asText();
    }

    @Override
    public SpeedyText fromText(String text) {
        return Speedy.from(text);
    }

    @Override
    public String toClob(SpeedyText speedyText) {
        return speedyText.asText();
    }

    @Override
    public SpeedyText fromClob(String clob) {
        return Speedy.from(clob);
    }

    @Override
    public String toVarchar(SpeedyText speedyText) {
        return speedyText.asText();
    }

    @Override
    public SpeedyText fromVarchar(String varchar) {
        return Speedy.from(varchar);
    }

    @Override
    public String toChar(SpeedyText speedyText) {
        return speedyText.asText();
    }

    @Override
    public SpeedyText fromChar(String char_v) {
        return Speedy.from(char_v);
    }

    @Override
    public UUID toUuid(SpeedyText speedyText) {
        return UUID.fromString(speedyText.asText());
    }

    @Override
    public SpeedyText fromUuid(UUID uuid) {
        return Speedy.from(uuid.toString());
    }
}
