package com.github.silent.samurai.speedy.deserializer;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.query.SpeedyQueryHelper;

import java.util.Optional;

public class QueryKeyDeserializer {

    private final EntityMetadata entityMetadata;
    private final SpeedyQuery speedyQuery;
    private final SpeedyQueryHelper speedyQueryHelper;

    public QueryKeyDeserializer(SpeedyQuery speedyQuery) {
        this.speedyQuery = speedyQuery;
        this.entityMetadata = speedyQuery.getFrom();
        this.speedyQueryHelper = new SpeedyQueryHelper(speedyQuery);
    }

    public SpeedyEntityKey deserialize() throws Exception {
        if (!speedyQueryHelper.isIdentifiersPresent()) {
            throw new BadRequestException("identifiers field not present in query");
        }
        return this.getCompositeKey();
    }

    private SpeedyEntityKey getCompositeKey() {
        SpeedyEntityKey speedyEntityKey = new SpeedyEntityKey(entityMetadata);
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            Optional<SpeedyValue> filterValue = this.speedyQueryHelper.getFilterValue(keyFieldMetadata);
            filterValue.ifPresent(speedyValue -> speedyEntityKey.put(keyFieldMetadata, speedyValue));
        }
        return speedyEntityKey;
    }

}
