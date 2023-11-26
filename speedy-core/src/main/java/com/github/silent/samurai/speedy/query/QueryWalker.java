package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;

public class QueryWalker {
    private final EntityMetadata entityMetadata;
    private final SpeedyQuery speedyQuery;

    public QueryWalker(EntityMetadata entityMetadata, SpeedyQuery speedyQuery) {
        this.entityMetadata = entityMetadata;
        this.speedyQuery = speedyQuery;
    }

    public void walk() {

    }

}
