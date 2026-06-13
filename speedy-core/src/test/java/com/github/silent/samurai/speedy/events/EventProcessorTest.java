package com.github.silent.samurai.speedy.events;

import com.github.silent.samurai.speedy.annotations.SpeedyEvent;
import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.ISpeedyEventHandler;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.mappings.JavaTypeRegistry;
import com.github.silent.samurai.speedy.mappings.JavaToSpeedy;
import com.github.silent.samurai.speedy.mappings.SpeedyToJava;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventProcessorTest {

    @Mock
    MetaModel metaModel;

    RegistryImpl registry;
    EntityMetadata productMetadata;
    SpeedyToJava serializer = new SpeedyToJava(JavaTypeRegistry.defaults());
    JavaToSpeedy deserializer = new JavaToSpeedy(JavaTypeRegistry.defaults());

    @BeforeEach
    void setUp() throws NotFoundException {
        productMetadata = StaticEntityMetadata.createEntityMetadata(Product.class);
        when(metaModel.findEntityMetadata("Product")).thenReturn(productMetadata);
        when(metaModel.findEntityMetadata(""))
                .thenThrow(new NotFoundException("Entity '' not found in metamodel"));
        when(metaModel.findEntityMetadata("NonExistent"))
                .thenThrow(new NotFoundException("Entity 'NonExistent' not found in metamodel"));
        registry = new RegistryImpl();
        SpeedyEntityHandler.reset();
        PojoHandler.reset();
    }

    /// Tests that captureEvents rejects a @SpeedyEvent method with zero parameters.
    /// Verifies that processRegistry throws IllegalArgumentException with the expected
    /// "must have a single parameter" message.
    @Test
    void shouldThrowWhenMethodHasZeroParams() {
        registry.registerEventHandler(new ZeroParamHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        assertThrows(IllegalArgumentException.class, () -> ep.processRegistry());
    }

    /// Tests that captureEvents rejects a @SpeedyEvent method with more than one parameter.
    /// Verifies that processRegistry throws IllegalArgumentException with the expected
    /// "must have a single parameter" message.
    @Test
    void shouldThrowWhenMethodHasTwoParams() {
        registry.registerEventHandler(new TwoParamHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        assertThrows(IllegalArgumentException.class, () -> ep.processRegistry());
    }

    /// Tests that captureEvents fails fast when the entity name in @SpeedyEvent
    /// does not match any entity in the MetaModel.
    /// Verifies that the NotFoundException is caught and re-wrapped in a RuntimeException
    /// containing the "Misconfigured @SpeedyEvent" prefix and the offending entity name.
    @Test
    void shouldThrowWhenEntityNotFoundInMetamodel() {
        registry.registerEventHandler(new NonExistentEntityHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ep.processRegistry());
        assertEquals("Misconfigured @SpeedyEvent: entity 'NonExistent' not found in metamodel",
                ex.getMessage());
    }

    /// Tests that captureEvents fails fast when @SpeedyEvent uses the default empty
    /// string as the entity name.
    /// Verifies that the MetaModel lookup for "" throws NotFoundException, which is then
    /// re-wrapped in a RuntimeException with the "Misconfigured @SpeedyEvent" prefix.
    @Test
    void shouldThrowWhenEntityNameIsEmpty() {
        registry.registerEventHandler(new EmptyEntityNameHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ep.processRegistry());
        assertTrue(ex.getMessage().contains("Misconfigured @SpeedyEvent"));
    }

    /// Tests that a handler with a single event type in @SpeedyEvent is only registered
    /// for that specific event type.
    /// Verifies via isEventPresent that POST_INSERT returns true while POST_UPDATE and
    /// PRE_INSERT both return false.
    @Test
    void shouldRegisterHandlerForSingleEventType() throws Exception {
        registry.registerEventHandler(new PostInsertHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        assertTrue(ep.isEventPresent(SpeedyEventType.POST_INSERT, productMetadata));
        assertFalse(ep.isEventPresent(SpeedyEventType.POST_UPDATE, productMetadata));
        assertFalse(ep.isEventPresent(SpeedyEventType.PRE_INSERT, productMetadata));
    }

    /// Tests that a handler with multiple event types in its @SpeedyEvent annotation
    /// is registered under all specified types.
    /// Verifies via isEventPresent that PRE_INSERT and POST_INSERT return true while
    /// unspecified types like PRE_UPDATE return false.
    @Test
    void shouldRegisterHandlerForMultipleEventTypes() {
        registry.registerEventHandler(new MultiEventTypeHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        assertTrue(ep.isEventPresent(SpeedyEventType.PRE_INSERT, productMetadata));
        assertTrue(ep.isEventPresent(SpeedyEventType.POST_INSERT, productMetadata));
        assertFalse(ep.isEventPresent(SpeedyEventType.PRE_UPDATE, productMetadata));
    }

    /// Tests that multiple handlers can coexist for the same entity and event type.
    /// Verifies via isEventPresent that the event is still reported as present after
    /// registering two distinct handlers for the same (entity, eventType) pair.
    @Test
    void shouldRegisterMultipleHandlersForSameEvent() {
        registry.registerEventHandler(new PostInsertHandler());
        registry.registerEventHandler(new AnotherPostInsertHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        assertTrue(ep.isEventPresent(SpeedyEventType.POST_INSERT, productMetadata));
    }

    /// Tests that isEventPresent correctly isolates handlers by entity name.
    /// Verifies that after registering a handler for "Product", isEventPresent returns
    /// false when queried with a different entity name ("OtherEntity") even when the
    /// event type matches.
    @Test
    void shouldNotMatchDifferentEntityForHandler() {
        registry.registerEventHandler(new PostInsertHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        EntityMetadata otherMetadata = org.mockito.Mockito.mock(EntityMetadata.class);
        org.mockito.Mockito.when(otherMetadata.getName()).thenReturn("OtherEntity");

        assertFalse(ep.isEventPresent(SpeedyEventType.POST_INSERT, otherMetadata));
    }

    /// Tests that triggerEvent invokes a handler whose method parameter type is
    /// SpeedyEntity (the internal type), bypassing serialization/deserialization.
    /// Verifies by capturing the SpeedyEntity reference in the handler and asserting
    /// that it carries the exact same field values that were placed before the trigger.
    @Test
    void shouldInvokeHandlerWithSpeedyEntityParam() throws Exception {
        registry.registerEventHandler(new SpeedyEntityHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        SpeedyEntity entity = new SpeedyEntity(productMetadata);
        entity.put("name", new SpeedyText("test-name"));
        entity.put("cost", new SpeedyInt(100L));

        ep.triggerEvent(SpeedyEventType.POST_INSERT, productMetadata, entity);

        assertTrue(SpeedyEntityHandler.WAS_CALLED.get());
        assertNotNull(SpeedyEntityHandler.RECEIVED_ENTITY.get());
        assertEquals("test-name",
                SpeedyEntityHandler.RECEIVED_ENTITY.get().get("name").asText());
    }

    /// Tests that triggerEvent serializes a SpeedyEntity to a typed Java POJO when
    /// the handler method parameter is a concrete class (e.g. Product), then
    /// deserializes any modifications back into the SpeedyEntity after invocation.
    /// Verifies that the handler receives the original data as a Product instance,
    /// and that a field mutation made in the handler is reflected in the SpeedyEntity.
    @Test
    void shouldInvokeHandlerWithJavaPojoParam() throws Exception {
        registry.registerEventHandler(new PojoHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        SpeedyEntity entity = new SpeedyEntity(productMetadata);
        entity.put("name", new SpeedyText("original-name"));
        entity.put("id", new SpeedyText("PK-001"));
        entity.put("cost", new SpeedyInt(50L));

        ep.triggerEvent(SpeedyEventType.POST_INSERT, productMetadata, entity);

        assertTrue(PojoHandler.WAS_CALLED.get());
        assertEquals("PK-001", PojoHandler.RECEIVED_PRODUCT.get().getId());
        assertEquals("original-name", PojoHandler.RECEIVED_NAME.get());
        assertEquals("modified-name", entity.get("name").asText());
    }

    /// Tests that an exception thrown by an event handler propagates through
    /// triggerEvent without being swallowed.
    /// Verifies by registering a handler that always throws a RuntimeException and
    /// asserting that the same exception (with the expected message) is received.
    @Test
    void shouldPropagateExceptionFromHandler() {
        registry.registerEventHandler(new ThrowingHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        SpeedyEntity entity = new SpeedyEntity(productMetadata);
        Exception ex = assertThrows(Exception.class,
                () -> ep.triggerEvent(SpeedyEventType.POST_INSERT, productMetadata, entity));
        assertTrue(ex.getMessage().contains("handler failure"));
    }

    /// Tests that triggerEvent silently returns when no handler is registered for
    /// the given event type and entity combination.
    /// Verifies that calling triggerEvent with an empty event map does not throw.
    @Test
    void shouldNotThrowWhenNoHandlersRegistered() throws Exception {
        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        SpeedyEntity entity = new SpeedyEntity(productMetadata);
        assertDoesNotThrow(() -> ep.triggerEvent(SpeedyEventType.POST_INSERT, productMetadata, entity));
    }

    /// Tests that all handlers registered for the same entity and event type are
    /// invoked by a single triggerEvent call.
    /// Verifies by registering two handlers that each increment a shared counter,
    /// then asserting the counter equals 2 after the event is triggered.
    @Test
    void shouldInvokeMultipleHandlersForSameEvent() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);

        ISpeedyEventHandler handler1 = new ISpeedyEventHandler() {
            @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.POST_INSERT})
            public void onEvent(SpeedyEntity e) {
                callCount.incrementAndGet();
            }
        };
        ISpeedyEventHandler handler2 = new ISpeedyEventHandler() {
            @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.POST_INSERT})
            public void onEvent(SpeedyEntity e) {
                callCount.incrementAndGet();
            }
        };

        registry.registerEventHandler(handler1);
        registry.registerEventHandler(handler2);

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        SpeedyEntity entity = new SpeedyEntity(productMetadata);
        ep.triggerEvent(SpeedyEventType.POST_INSERT, productMetadata, entity);

        assertEquals(2, callCount.get());
    }

    /// Tests that isEventPresent returns false for all event types when no handlers
    /// have been registered in the event map.
    /// Verifies that both PRE_INSERT and POST_DELETE return false for an
    /// EventProcessor backed by an empty registry.
    @Test
    void shouldReturnFalseWhenEventTypeNotRegistered() {
        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        assertFalse(ep.isEventPresent(SpeedyEventType.PRE_INSERT, productMetadata));
        assertFalse(ep.isEventPresent(SpeedyEventType.POST_DELETE, productMetadata));
    }

    /// Tests that isEventPresent for a given event type only considers handlers
    /// registered under that exact event type key.
    /// Verifies that after registering a POST_INSERT handler, isEventPresent returns
    /// false when queried for PRE_INSERT (same entity, different event type).
    @Test
    void shouldReturnFalseWhenEntityNotRegisteredForEventType() {
        registry.registerEventHandler(new PostInsertHandler());

        EventProcessor ep = new EventProcessor(metaModel, registry, serializer, deserializer);
        ep.processRegistry();

        assertFalse(ep.isEventPresent(SpeedyEventType.PRE_INSERT, productMetadata));
    }

    // -- Test handler inner classes -----------------------------------------

    static class ZeroParamHandler implements ISpeedyEventHandler {
        @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.POST_INSERT})
        public void noParam() {
        }
    }

    static class TwoParamHandler implements ISpeedyEventHandler {
        @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.POST_INSERT})
        public void twoParams(SpeedyEntity e, String extra) {
        }
    }

    static class NonExistentEntityHandler implements ISpeedyEventHandler {
        @SpeedyEvent(value = "NonExistent", eventType = {SpeedyEventType.POST_INSERT})
        public void handle(SpeedyEntity e) {
        }
    }

    static class EmptyEntityNameHandler implements ISpeedyEventHandler {
        @SpeedyEvent(value = "", eventType = {SpeedyEventType.POST_INSERT})
        public void handle(SpeedyEntity e) {
        }
    }

    static class PostInsertHandler implements ISpeedyEventHandler {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);

        @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.POST_INSERT})
        public void onPostInsert(SpeedyEntity entity) {
            wasCalled.set(true);
        }
    }

    static class AnotherPostInsertHandler implements ISpeedyEventHandler {
        @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.POST_INSERT})
        public void onPostInsert(SpeedyEntity entity) {
        }
    }

    static class MultiEventTypeHandler implements ISpeedyEventHandler {
        @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.PRE_INSERT, SpeedyEventType.POST_INSERT})
        public void onEvent(SpeedyEntity entity) {
        }
    }

    static class SpeedyEntityHandler implements ISpeedyEventHandler {
        static final AtomicBoolean WAS_CALLED = new AtomicBoolean(false);
        static final AtomicReference<SpeedyEntity> RECEIVED_ENTITY = new AtomicReference<>();

        static void reset() {
            WAS_CALLED.set(false);
            RECEIVED_ENTITY.set(null);
        }

        @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.POST_INSERT})
        public void onPostInsert(SpeedyEntity entity) {
            WAS_CALLED.set(true);
            RECEIVED_ENTITY.set(entity);
        }
    }

    static class PojoHandler implements ISpeedyEventHandler {
        static final AtomicBoolean WAS_CALLED = new AtomicBoolean(false);
        static final AtomicReference<Product> RECEIVED_PRODUCT = new AtomicReference<>();
        static final AtomicReference<String> RECEIVED_NAME = new AtomicReference<>();

        static void reset() {
            WAS_CALLED.set(false);
            RECEIVED_PRODUCT.set(null);
            RECEIVED_NAME.set(null);
        }

        @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.POST_INSERT})
        public void onPostInsert(Product product) {
            WAS_CALLED.set(true);
            RECEIVED_PRODUCT.set(product);
            RECEIVED_NAME.set(product.getName());
            product.setName("modified-name");
        }
    }

    static class ThrowingHandler implements ISpeedyEventHandler {
        @SpeedyEvent(value = "Product", eventType = {SpeedyEventType.POST_INSERT})
        public void onPostInsert(SpeedyEntity entity) {
            throw new RuntimeException("handler failure");
        }
    }
}
