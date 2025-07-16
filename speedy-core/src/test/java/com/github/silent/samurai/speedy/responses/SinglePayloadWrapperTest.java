package com.github.silent.samurai.speedy.responses;


import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import org.junit.jupiter.api.Test;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class SinglePayloadWrapperTest {


    // Constructor initializes SinglePayloadWrapper with provided SpeedyEntity payload
    @Test
    public void test_constructor_initializes_with_valid_payload() {
        EntityMetadata metadata = mock(EntityMetadata.class);
        SpeedyEntity payload = new SpeedyEntity(metadata);

        SinglePayloadWrapper wrapper = new SinglePayloadWrapper(payload);

        assertNotNull(wrapper);
        assertEquals(payload, wrapper.getPayload());
        assertEquals(0, wrapper.getPageIndex());
        assertEquals(1, wrapper.getPageSize());
        assertEquals(1, wrapper.getTotalPageCount());
    }

    // Constructor handles null SpeedyEntity payload
    @Test
    public void test_constructor_handles_null_payload() {
        SinglePayloadWrapper wrapper = new SinglePayloadWrapper(null);

        assertNotNull(wrapper);
        assertNull(wrapper.getPayload());
        assertEquals(0, wrapper.getPageIndex());
        assertEquals(1, wrapper.getPageSize());
        assertEquals(1, wrapper.getTotalPageCount());
    }

    // Static factory method wrapperInResponse creates new wrapper with payload
    @Test
    public void test_wrapperInResponse_creates_wrapper_with_payload() {
        EntityMetadata metadata = mock(EntityMetadata.class);
        SpeedyEntity payload = new SpeedyEntity(metadata);

        SinglePayloadWrapper wrapper = SinglePayloadWrapper.wrapperInResponse(payload);

        assertNotNull(wrapper);
        assertEquals(payload, wrapper.getPayload());
        assertEquals(0, wrapper.getPageIndex());
        assertEquals(1, wrapper.getPageSize());
        assertEquals(1, wrapper.getTotalPageCount());
    }

    // Setter methods update values for pageIndex, pageSize and totalPageCount
    @Test
    public void test_setter_methods_update_values() {
        EntityMetadata metadata = mock(EntityMetadata.class);
        SpeedyEntity payload = new SpeedyEntity(metadata);

        SinglePayloadWrapper wrapper = new SinglePayloadWrapper(payload);

        wrapper.setPageIndex(5);
        wrapper.setPageSize(10);
        wrapper.setTotalPageCount(15);

        assertEquals(5, wrapper.getPageIndex());
        assertEquals(10, wrapper.getPageSize());
        assertEquals(15, wrapper.getTotalPageCount());
    }
}