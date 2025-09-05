package com.github.silent.samurai.speedy.utils;

import org.junit.jupiter.api.Test;

import static com.github.silent.samurai.speedy.utils.CommonUtil.convertToEnum;

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

        assert s1 == Status.ACTIVE;
        assert s2 == Status.INACTIVE;

    }
}