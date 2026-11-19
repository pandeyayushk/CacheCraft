package com.ayush;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the public hit, miss, and eviction statistics of {@link LRUCache}.
 */
class LRUCacheStatisticsTest {

    private LRUCache<String, Integer> cache;

    @BeforeEach
    void setUp() {
        cache = new LRUCache<>(2);
    }

    @Test
    void successfulGetIncrementsHits() {
        cache.put("one", 1);

        assertEquals(1, cache.get("one"));

        assertEquals(1, cache.getHits(), "A successful get should count as a hit");
        assertEquals(0, cache.getMisses(), "A successful get should not count as a miss");
    }

    @Test
    void getForMissingKeyIncrementsMisses() {
        assertNull(cache.get("missing"));
        assertNull(cache.get("missing"));

        assertEquals(2, cache.getMisses(), "Each unsuccessful get should count as a miss");
        assertEquals(0, cache.getHits());
    }

    @Test
    void getForExpiredEntryIncrementsMissesWithoutIncrementingHits() throws InterruptedException {
        cache.put("one", 1, Duration.ofMillis(25));
        Thread.sleep(75);

        assertNull(cache.get("one"));

        assertEquals(1, cache.getMisses(), "An expired entry should count as a miss");
        assertEquals(0, cache.getHits(), "An expired entry should not count as a hit");
    }

    @Test
    void insertingPastCapacityIncrementsEvictions() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);

        assertEquals(1, cache.getEvictions(),
                "Adding a distinct key at capacity should evict one entry");
    }

    @Test
    void updatingExistingKeyDoesNotIncrementEvictions() {
        cache.put("one", 1);
        cache.put("one", 10);

        assertEquals(10, cache.get("one"));
        assertEquals(0, cache.getEvictions(), "Updating a key should not evict an entry");
    }

    @Test
    void expiredEntryCleanupDoesNotIncrementEvictions() throws InterruptedException {
        cache.put("one", 1, Duration.ofMillis(25));
        Thread.sleep(75);

        assertEquals(0, cache.size(), "Size should remove expired entries");
        assertEquals(0, cache.getEvictions(), "Expiration should not count as eviction");
    }

    @Test
    void resetStatisticsSetsAllCountersToZero() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.get("one");
        cache.get("missing");
        cache.put("three", 3);

        cache.resetStatistics();

        assertEquals(0, cache.getHits());
        assertEquals(0, cache.getMisses());
        assertEquals(0, cache.getEvictions());
    }

    @Test
    void clearPreservesStatistics() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.get("one");
        cache.get("missing");
        cache.put("three", 3);

        cache.clear();

        assertEquals(1, cache.getHits());
        assertEquals(1, cache.getMisses());
        assertEquals(1, cache.getEvictions());
    }

    @Test
    void statisticsGettersReturnCountsForOperationSequence() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.get("one");
        cache.get("missing");
        cache.put("three", 3);
        cache.get("three");

        assertEquals(2, cache.getHits());
        assertEquals(1, cache.getMisses());
        assertEquals(1, cache.getEvictions());
    }
}
