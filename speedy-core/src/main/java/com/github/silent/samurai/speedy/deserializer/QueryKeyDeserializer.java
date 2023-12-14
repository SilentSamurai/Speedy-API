package com.github.silent.samurai.speedy.deserializer;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.query.QueryHelper;

import java.util.Optional;

public class QueryKeyDeserializer {

    private final EntityMetadata entityMetadata;
    private final SpeedyQuery speedyQuery;
    private final QueryHelper queryHelper;

    public QueryKeyDeserializer(SpeedyQuery speedyQuery) {
        this.speedyQuery = speedyQuery;
        this.entityMetadata = speedyQuery.getFrom();
        this.queryHelper = new QueryHelper(speedyQuery);
    }

    public SpeedyEntityKey deserialize() throws Exception {
        if (!queryHelper.isIdentifiersPresent()) {
            throw new BadRequestException("identifiers field not found");
        }
        return this.getCompositeKey();
    }

    private SpeedyEntityKey getCompositeKey() throws Exception {
        SpeedyEntityKey speedyEntityKey = new SpeedyEntityKey(entityMetadata);
        for (KeyFieldMetadata keyFieldMetadata : entityMetadata.getKeyFields()) {
            Optional<SpeedyValue> filterValue = this.queryHelper.getFilterValue(keyFieldMetadata);
            filterValue.ifPresent(speedyValue -> speedyEntityKey.put(keyFieldMetadata, speedyValue));
        }
        return speedyEntityKey;
    }

}
