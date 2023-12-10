package com.github.silent.samurai.speedy.deserializer;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
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
        if (entityMetadata.hasCompositeKey()) {
            return this.getCompositeKey();
        }
        return this.getBasicKey();
    }

    private SpeedyEntityKey getBasicKey() throws Exception {
        Optional<KeyFieldMetadata> primaryKeyFieldMetadata = entityMetadata.getKeyFields().stream().findAny();
        if (primaryKeyFieldMetadata.isPresent()) {
            KeyFieldMetadata keyFieldMetadata = primaryKeyFieldMetadata.get();
            Optional<SpeedyValue> filterValue = this.queryHelper.getFilterValue(keyFieldMetadata);
            if (filterValue.isPresent()) {
                SpeedyEntityKey speedyEntityKey = new SpeedyEntityKey(entityMetadata);
                SpeedyValue speedyValue = filterValue.get();
                speedyEntityKey.put(keyFieldMetadata, speedyValue);
                return speedyEntityKey;
            }
        }
        throw new BadRequestException("primary key field not found");
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
