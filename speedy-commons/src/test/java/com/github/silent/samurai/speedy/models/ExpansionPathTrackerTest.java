package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("ExpansionPathTracker Tests")
class ExpansionPathTrackerTest {

    private EntityMetadata inventory;
    private EntityMetadata product;
    private EntityMetadata category;
    private EntityMetadata supplier;
    private EntityMetadata address;

    @BeforeEach
    void setup() {
        inventory = Mockito.mock(EntityMetadata.class);
        product = Mockito.mock(EntityMetadata.class);
        category = Mockito.mock(EntityMetadata.class);
        supplier = Mockito.mock(EntityMetadata.class);
        address = Mockito.mock(EntityMetadata.class);

        when(inventory.getName()).thenReturn("Inventory");
        when(product.getName()).thenReturn("Product");
        when(category.getName()).thenReturn("Category");
        when(supplier.getName()).thenReturn("Supplier");
        when(address.getName()).thenReturn("Address");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create tracker with valid expansions")
        void shouldCreateTrackerWithValidExpansions() {
            Set<String> expansions = Set.of("Product", "Product.Category");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);
            
            assertNotNull(tracker);
            assertEquals(2, tracker.getRequestedExpansions().size());
            assertTrue(tracker.getRequestedExpansions().contains("Product"));
            assertTrue(tracker.getRequestedExpansions().contains("Product.Category"));
        }

        @Test
        @DisplayName("Should throw exception when expansions is null")
        void shouldThrowExceptionWhenExpansionsIsNull() {
            assertThrows(IllegalArgumentException.class, () -> {
                new ExpansionPathTracker(null);
            });
        }

        @Test
        @DisplayName("Should handle empty expansions set")
        void shouldHandleEmptyExpansionsSet() {
            Set<String> expansions = Set.of();
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);
            
            assertNotNull(tracker);
            assertTrue(tracker.getRequestedExpansions().isEmpty());
        }
    }

    @Nested
    @DisplayName("Path Management Tests")
    class PathManagementTests {

        @Test
        @DisplayName("Should push and pop entities correctly")
        void shouldPushAndPopEntitiesCorrectly() {
            Set<String> expansions = Set.of("Product", "Product.Category");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            // Initially empty
            assertTrue(tracker.isEmpty());
            assertEquals(0, tracker.getPathDepth());
            assertNull(tracker.getCurrentEntity());

            // Push first entity
            tracker.pushEntity(inventory);
            assertFalse(tracker.isEmpty());
            assertEquals(1, tracker.getPathDepth());
            assertEquals(inventory, tracker.getCurrentEntity());

            // Push second entity
            tracker.pushEntity(product);
            assertFalse(tracker.isEmpty());
            assertEquals(2, tracker.getPathDepth());
            assertEquals(product, tracker.getCurrentEntity());

            // Pop entities (LIFO order)
            EntityMetadata popped = tracker.popEntity();
            assertEquals(product, popped);
            assertEquals(1, tracker.getPathDepth());
            assertEquals(inventory, tracker.getCurrentEntity());

            popped = tracker.popEntity();
            assertEquals(inventory, popped);
            assertTrue(tracker.isEmpty());
            assertEquals(0, tracker.getPathDepth());
            assertNull(tracker.getCurrentEntity());
        }

        @Test
        @DisplayName("Should throw exception when pushing null entity")
        void shouldThrowExceptionWhenPushingNullEntity() {
            Set<String> expansions = Set.of("Product");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            assertThrows(IllegalArgumentException.class, () -> {
                tracker.pushEntity(null);
            });
        }

        @Test
        @DisplayName("Should return null when popping from empty path")
        void shouldReturnNullWhenPoppingFromEmptyPath() {
            Set<String> expansions = Set.of("Product");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            assertTrue(tracker.isEmpty());
            EntityMetadata popped = tracker.popEntity();
            assertNull(popped);
        }

        @Test
        @DisplayName("Should get current path as defensive copy")
        void shouldGetCurrentPathAsDefensiveCopy() {
            Set<String> expansions = Set.of("Product");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            tracker.pushEntity(product);

            List<EntityMetadata> path = tracker.getCurrentPath();
            assertEquals(2, path.size());
            assertEquals(inventory, path.get(0));
            assertEquals(product, path.get(1));

            // Modifying the returned list should not affect the tracker
            path.clear();
            assertEquals(2, tracker.getPathDepth());
            assertEquals(2, tracker.getCurrentPath().size());
        }
    }

    @Nested
    @DisplayName("Dot Path Generation Tests")
    class DotPathGenerationTests {

        @Test
        @DisplayName("Should generate correct dot path for single entity")
        void shouldGenerateCorrectDotPathForSingleEntity() {
            Set<String> expansions = Set.of("Product");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            String path = tracker.getCurrentDotPath(product);
            assertEquals("Product", path);
        }

        @Test
        @DisplayName("Should generate correct dot path for multiple entities")
        void shouldGenerateCorrectDotPathForMultipleEntities() {
            Set<String> expansions = Set.of("Product.Category");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            tracker.pushEntity(product);
            String path = tracker.getCurrentDotPath(category);
            assertEquals("Product.Category", path);
        }

        @Test
        @DisplayName("Should generate correct dot path for deep nesting")
        void shouldGenerateCorrectDotPathForDeepNesting() {
            Set<String> expansions = Set.of("Product.Category.Supplier");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            tracker.pushEntity(product);
            tracker.pushEntity(category);
            String path = tracker.getCurrentDotPath(supplier);
            assertEquals("Product.Category.Supplier", path);
        }

        @Test
        @DisplayName("Should return empty string when path is empty")
        void shouldReturnEmptyStringWhenPathIsEmpty() {
            Set<String> expansions = Set.of("Product");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            String path = tracker.getCurrentDotPath(product);
            assertEquals("", path);
        }

        @Test
        @DisplayName("Should throw exception when association is null")
        void shouldThrowExceptionWhenAssociationIsNull() {
            Set<String> expansions = Set.of("Product");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            assertThrows(IllegalArgumentException.class, () -> {
                tracker.getCurrentDotPath(null);
            });
        }
    }

    @Nested
    @DisplayName("Expansion Decision Tests")
    class ExpansionDecisionTests {

        @Test
        @DisplayName("Should expand when exact dot notation path matches")
        void shouldExpandWhenExactDotNotationPathMatches() {
            Set<String> expansions = Set.of("Product.Category");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            tracker.pushEntity(product);
            assertTrue(tracker.shouldExpand(category));
        }

        @Test
        @DisplayName("Should expand when entity name alone matches (backward compatibility)")
        void shouldExpandWhenEntityNameAloneMatches() {
            Set<String> expansions = Set.of("Category");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            tracker.pushEntity(product);
            assertFalse(tracker.shouldExpand(category));
        }

        @Test
        @DisplayName("Should not expand when neither path nor entity name matches")
        void shouldNotExpandWhenNeitherPathNorEntityNameMatches() {
            Set<String> expansions = Set.of("Product.Supplier");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            tracker.pushEntity(product);
            assertFalse(tracker.shouldExpand(category));
        }

        @Test
        @DisplayName("Should expand from root level when entity name matches")
        void shouldExpandFromRootLevelWhenEntityNameMatches() {
            Set<String> expansions = Set.of("Product");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            assertTrue(tracker.shouldExpand(product));
        }

        @Test
        @DisplayName("Should prioritize dot notation over entity name")
        void shouldPrioritizeDotNotationOverEntityName() {
            // This test verifies that dot notation is checked first
            Set<String> expansions = Set.of("Product.Category", "Category");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            tracker.pushEntity(product);
            assertTrue(tracker.shouldExpand(category));
        }

        @Test
        @DisplayName("Should throw exception when association is null")
        void shouldThrowExceptionWhenAssociationIsNull() {
            Set<String> expansions = Set.of("Product");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            assertThrows(IllegalArgumentException.class, () -> {
                tracker.shouldExpand(null);
            });
        }

        @Test
        @DisplayName("Should handle complex nested expansion scenarios")
        void shouldHandleComplexNestedExpansionScenarios() {
            Set<String> expansions = Set.of(
                "Product.Category",
                "Product.Category.Supplier",
                "Product.Category.Supplier.Address"
            );
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            // Test each level
            tracker.pushEntity(inventory);
            tracker.pushEntity(product);
            assertTrue(tracker.shouldExpand(category));

            tracker.pushEntity(category);
            assertTrue(tracker.shouldExpand(supplier));

            tracker.pushEntity(supplier);
            assertTrue(tracker.shouldExpand(address));

            // Test that we can't expand something not in the path
            assertFalse(tracker.shouldExpand(product));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete expansion workflow")
        void shouldHandleCompleteExpansionWorkflow() {
            Set<String> expansions = Set.of("Product.Category", "Product.Supplier");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            // Start with inventory
            tracker.pushEntity(inventory);
            assertEquals(1, tracker.getPathDepth());
            assertEquals("Inventory", tracker.getCurrentEntity().getName());

            // Check if we should expand the product
            assertFalse(tracker.shouldExpand(product));
            String productPath = tracker.getCurrentDotPath(product);
            assertEquals("Product", productPath);

            // Push product and check category expansion
            tracker.pushEntity(product);
            assertEquals(2, tracker.getPathDepth());
            assertTrue(tracker.shouldExpand(category));
            assertTrue(tracker.shouldExpand(supplier));

            String categoryPath = tracker.getCurrentDotPath(category);
            assertEquals("Product.Category", categoryPath);

            String supplierPath = tracker.getCurrentDotPath(supplier);
            assertEquals("Product.Supplier", supplierPath);

            // Pop product and verify state
            EntityMetadata popped = tracker.popEntity();
            assertEquals(product, popped);
            assertEquals(1, tracker.getPathDepth());
            assertEquals(inventory, tracker.getCurrentEntity());
        }

        @Test
        @DisplayName("Should handle multiple expansion paths correctly")
        void shouldHandleMultipleExpansionPathsCorrectly() {
            Set<String> expansions = Set.of(
                "Product",
                "Product.Category",
                "Product.Category.Supplier",
                "Supplier.Address"
            );
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            // Test Product expansion
            tracker.pushEntity(inventory);
            assertTrue(tracker.shouldExpand(product));

            // Test Product.Category expansion
            tracker.pushEntity(product);
            assertTrue(tracker.shouldExpand(category));

            // Test Product.Category.Supplier expansion
            tracker.pushEntity(category);
            assertTrue(tracker.shouldExpand(supplier));

            // Test Supplier.Address expansion (from a different path)
            tracker.popEntity(); // remove category
            tracker.popEntity(); // remove product
            tracker.pushEntity(supplier);
            assertTrue(tracker.shouldExpand(address));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty expansion set")
        void shouldHandleEmptyExpansionSet() {
            Set<String> expansions = Set.of();
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            tracker.pushEntity(inventory);
            tracker.pushEntity(product);
            assertFalse(tracker.shouldExpand(category));
        }

        @Test
        @DisplayName("Should handle empty path with entity name expansion")
        void shouldHandleEmptyPathWithEntityNameExpansion() {
            Set<String> expansions = Set.of("Product");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            assertFalse(tracker.shouldExpand(product));
            assertEquals("", tracker.getCurrentDotPath(product));
        }

        @Test
        @DisplayName("Should handle deep nesting with many entities")
        void shouldHandleDeepNestingWithManyEntities() {
            Set<String> expansions = Set.of("B.C.D.E.F");
            ExpansionPathTracker tracker = new ExpansionPathTracker(expansions);

            EntityMetadata a = Mockito.mock(EntityMetadata.class);
            EntityMetadata b = Mockito.mock(EntityMetadata.class);
            EntityMetadata c = Mockito.mock(EntityMetadata.class);
            EntityMetadata d = Mockito.mock(EntityMetadata.class);
            EntityMetadata e = Mockito.mock(EntityMetadata.class);
            EntityMetadata f = Mockito.mock(EntityMetadata.class);

            when(a.getName()).thenReturn("A");
            when(b.getName()).thenReturn("B");
            when(c.getName()).thenReturn("C");
            when(d.getName()).thenReturn("D");
            when(e.getName()).thenReturn("E");
            when(f.getName()).thenReturn("F");

            tracker.pushEntity(a);
            tracker.pushEntity(b);
            tracker.pushEntity(c);
            tracker.pushEntity(d);
            tracker.pushEntity(e);

            assertTrue(tracker.shouldExpand(f));
            assertEquals("B.C.D.E.F", tracker.getCurrentDotPath(f));
        }
    }
}