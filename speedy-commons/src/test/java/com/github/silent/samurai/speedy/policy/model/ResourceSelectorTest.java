package com.github.silent.samurai.speedy.policy.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceSelectorTest {

    @Test
    void fieldSpecific_matchesOnlyThatField() {
        ResourceSelector selector = ResourceSelector.parse("Employee.salary");

        assertTrue(selector.matchesEntity("Employee"));
        assertTrue(selector.matches("Employee", "salary"));
        assertFalse(selector.matches("Employee", "name"));
        assertFalse(selector.matchesEntity("Department"));
    }

    @Test
    void wildcard_matchesEveryFieldOfTheEntity() {
        ResourceSelector selector = ResourceSelector.parse("Employee.*");

        assertTrue(selector.matches("Employee", "salary"));
        assertTrue(selector.matches("Employee", "name"));
        assertFalse(selector.matches("Department", "name"));
    }

    @Test
    void entitySubject_matchesEveryField() {
        ResourceSelector selector = ResourceSelector.parse("Employee");

        assertTrue(selector.matches("Employee", "salary"));
        assertTrue(selector.matches("Employee", "name"));
        assertFalse(selector.matches("Department", "name"));
    }

    @Test
    void blankTokens_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> ResourceSelector.parse(""));
        assertThrows(IllegalArgumentException.class, () -> ResourceSelector.parse(null));
    }
}
