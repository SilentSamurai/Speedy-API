package com.github.silent.samurai.speedy.utils;

import org.junit.jupiter.api.Test;

import static com.github.silent.samurai.speedy.utils.CommonUtil.convertToEnum;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonUtilTest {

    @Test
    void convertToEnumTest() {

        enum Status {
            ACTIVE, INACTIVE, PENDING
        }

        var stringNode = Speedy.from("ACTIVE");
        var intNode = Speedy.from(1L);

        Status s1 = convertToEnum(Status.class, stringNode); // ACTIVE
        Status s2 = convertToEnum(Status.class, intNode);    // INACTIVE

        assertEquals(Status.ACTIVE, s1);
        assertEquals(Status.INACTIVE, s2);

    }
}