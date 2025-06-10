package com.github.silent.samurai.speedy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;
import org.junit.jupiter.api.Test;

import static com.github.silent.samurai.speedy.api.client.SpeedyQuery.*;

public class QueryTest {

    @Test
    void test() throws JsonProcessingException {
        JsonNode build = SpeedyQuery.builder()
                .from("Resource")
                .where(
                        condition("id", eq("1")),
                        condition("bv", eq("2")),
                        condition("rt", eq("3")),
                        and(
                                condition("id", eq("1"))
                        ),
                        or(
                                condition("id", eq("1"))
                        )
                )
                .orderByAsc("id")
                .orderByDesc("cost")
                .pageNo(0)
                .pageSize(10)
                .prettyPrint()
                .build();

        System.out.println(build);
    }

    @Test
    void test2() throws JsonProcessingException {
        JsonNode build = SpeedyQuery.builder()
                .from("Resource")
                .where(
                        and(
                                condition("id", eq("1")),
                                condition("id", eq("1"))
                        ),
                        condition("id", eq("1")),
                        condition("bv", eq("2")),
                        condition("rt", eq("3")),
                        or(
                                condition("id", eq("1"))
                        )
                )
                .where(
                        or(
                                condition("id", eq("1")),
                                condition("id", eq("1"))
                        )
                )
                .orderByAsc("id")
                .orderByDesc("cost")
                .pageNo(0)
                .pageSize(10)
                .prettyPrint()
                .build();

        System.out.println(build);
    }
}
