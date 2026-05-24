package com.github.silent.samurai.speedy.api.client.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class SpeedyResponse {

    private static final ObjectMapper MAPPER = CommonUtil.json();

    private Integer pageIndex;
    private Integer pageSize;
    private Integer totalPageCount;
    private JsonNode payload;

    /**
     * Deserialize the payload as a list of the given type.
     *
     * @param type the entity class
     * @return list of typed entities, or empty list if payload is null/missing
     */
    public <T> List<T> asList(Class<T> type) {
        if (payload == null || payload.isNull()) {
            return Collections.emptyList();
        }
        if (payload.isArray()) {
            JavaType javaType = MAPPER.getTypeFactory().constructCollectionType(List.class, type);
            try {
                return MAPPER.treeToValue(payload, javaType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize payload as List<" + type.getSimpleName() + ">", e);
            }
        }
        try {
            T value = MAPPER.treeToValue(payload, type);
            return Collections.singletonList(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize payload as " + type.getSimpleName(), e);
        }
    }

    /**
     * Deserialize the payload as a single entity of the given type.
     * If the payload is an array, returns the first element.
     *
     * @param type the entity class
     * @return the typed entity, or null if payload is null/missing
     */
    public <T> T asSingle(Class<T> type) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        JsonNode node = payload.isArray() ? payload.get(0) : payload;
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return MAPPER.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize payload as " + type.getSimpleName(), e);
        }
    }

    /**
     * For count queries, extracts the count as a long.
     *
     * @return the count value
     */
    public long asCount() {
        if (payload == null || payload.isNull()) {
            return 0L;
        }
        return payload.asLong(0L);
    }

}
