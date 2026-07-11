package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.OrderByOperator;

public interface OrderBy {

    QueryField getField();

    OrderByOperator getOperator();


}
