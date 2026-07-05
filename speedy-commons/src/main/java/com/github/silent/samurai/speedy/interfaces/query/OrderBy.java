package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;

public interface OrderBy {

    FieldMetadata getFieldMetadata();

    OrderByOperator getOperator();


}
