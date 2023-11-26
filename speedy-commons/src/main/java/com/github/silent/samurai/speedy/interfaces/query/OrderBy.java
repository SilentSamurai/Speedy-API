package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;

public interface OrderBy {

    FieldMetadata getFieldMetadata();

    OrderByOperator getOperator();


}
