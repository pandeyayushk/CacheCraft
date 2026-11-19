package com.ayush;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LRUCacheTest {
    private LRUCache<String, Integer> cache;

    @BeforeEach
    void setUp() {
        cache = new LRUCache<>(2);
    }

    @Test
    void putAndGet() {
        cache.put("one", 1);

        assertEquals(1, cache.get("one"));
    }

    @Test
    void getMissingKey() {
        assertNull(cache.get("missing"));
    }

    @Test
    void expiredEntryIsRemovedByContainsKey() {
        cache.put("one", 1, Duration.ofNanos(1));

        assertFalse(cache.containsKey("one"));
        assertEquals(0, cache.size());
    }

    @Test
    void sizeCountsOnlyNonExpiredEntriesAndRemovesExpiredOnes() {
        cache.put("one", 1, Duration.ofNanos(1));
        cache.put("two", 2);

        assertEquals(1, cache.size());
        assertFalse(cache.containsKey("one"));
        assertTrue(cache.containsKey("two"));
    }

    @Test
    void putRemovesExpiredEntriesBeforeLruEviction() {
        cache.put("one", 1, Duration.ofNanos(1));
        cache.put("two", 2);

        cache.put("three", 3);

        assertFalse(cache.containsKey("one"));
        assertTrue(cache.containsKey("two"));
        assertTrue(cache.containsKey("three"));
    }

    @Test
    void ttlMustNotBeNullOrNegative() {
        assertThrows(NullPointerException.class, () -> cache.put("one", 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> cache.put("one", 1, Duration.ofNanos(-1)));
    }

    @Test
    void zeroTtlDoesNotExpire() {
        cache.put("one", 1, Duration.ZERO);

        assertTrue(cache.containsKey("one"));
        assertEquals(1, cache.get("one"));
    }

    @Test
    void evictsLeastRecentlyUsed() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);

        assertNull(cache.get("one"));
        assertEquals(2, cache.get("two"));
        assertEquals(3, cache.get("three"));
    }

    @Test
    void accessUpdatesUsageOrder() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.get("one");
        cache.put("three", 3);

        assertEquals(1, cache.get("one"));
        assertNull(cache.get("two"));
        assertEquals(3, cache.get("three"));
    }

    @Test
    void updateExistingKey() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("one", 10);

        assertEquals(2, cache.size());
        assertEquals(10, cache.get("one"));
        assertEquals(2, cache.get("two"));

        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("one", 10);
        cache.put("three", 3);

        assertNull(cache.get("two"));
        assertEquals(10, cache.get("one"));
    }

    @Test
    void removeExistingKey() {
        cache.put("one", 1);

        assertEquals(1, cache.remove("one"));
        assertFalse(cache.containsKey("one"));
    }

    @Test
    void clearCache() {
        cache.put("one", 1);
        cache.put("two", 2);

        cache.clear();

        assertEquals(0, cache.size());
        assertNull(cache.get("one"));
    }

    @Test
    void invalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(0));
        assertThrows(IllegalArgumentException.class,() -> new LRUCache<>(-1));
    }
}
