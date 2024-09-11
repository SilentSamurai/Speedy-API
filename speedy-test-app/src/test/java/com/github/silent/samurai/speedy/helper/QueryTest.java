package com.github.silent.samurai.speedy.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static com.github.silent.samurai.speedy.helper.SpdyQBuilder.*;

public class QueryTest {

    @Test
    void test() throws JsonProcessingException {
        JsonNode build = new SpdyQBuilder()
                .$from("Resource")
                .$where("id", $eq("1"))
                .$where("bv", $eq("2"))
                .$where("rt", $eq("3"))
                .$and(
                        $whereCond("id", $eq("1"))
                )
                .$or(
                        $whereCond("id", $eq("1"))
                )
                .$orderByAsc("id")
                .$orderByDesc("cost")
                .$pageNo(0)
                .$pageSize(10).build();

        System.out.println(build);
    }
}
