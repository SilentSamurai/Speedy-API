package com.github.silent.samurai.speedy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.silent.samurai.speedy.SpdyQBuilder.*;

public class QueryTest {

    @Test
    void test() throws JsonProcessingException {
        JsonNode build = new SpdyQBuilder()
                .$from("Resource")
                .$whereCondition("id", $eq("1"))
                .$whereCondition("bv", $eq("2"))
                .$whereCondition("rt", $eq("3"))
                .$and(
                        $condition("id", $eq("1"))
                )
                .$or(
                        $condition("id", $eq("1"))
                )
                .$orderByAsc("id")
                .$orderByDesc("cost")
                .$pageNo(0)
                .$pageSize(10).build();

        System.out.println(build);
    }

    @Test
    void test2() throws JsonProcessingException {
        JsonNode build = new SpdyQBuilder()
                .$from("Resource")
                .$where(
                        $and(
                                List.of(
                                        $condition("id", $eq("1")),
                                        $condition("id", $eq("1"))
                                )
                        )
                )
                .$whereCondition("id", $eq("1"))
                .$whereCondition("bv", $eq("2"))
                .$whereCondition("rt", $eq("3"))
                .$and(
                        $condition("id", $eq("1"))
                )
                .$or(
                        $condition("id", $eq("1"))
                )
                .$where(
                        $or(
                                List.of(
                                        $condition("id", $eq("1")),
                                        $condition("id", $eq("1"))
                                )
                        )
                )
                .$orderByAsc("id")
                .$orderByDesc("cost")
                .$pageNo(0)
                .$pageSize(10)
                .prettyPrint()
                .build();

        System.out.println(build);
    }
}
