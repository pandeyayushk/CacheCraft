package com.ayush;

import java.time.Duration;

/**
 * Demonstrates LRU eviction, TTL expiration, and cache statistics.
 */
public class CacheCraftDemo {

    public static void main(String[] args) throws InterruptedException {
        LRUCache<String, String> cache = new LRUCache<>(3);

        System.out.println("Adding three entries to a cache with capacity 3.");
        cache.put("A", "Apple");
        cache.put("B", "Banana");
        cache.put("C", "Cherry");

        System.out.println("Accessing A makes it the most recently used entry.");
        System.out.println("get(A) -> " + cache.get("A"));

        System.out.println("Adding D evicts the least recently used entry, B.");
        cache.put("D", "Date");
        System.out.println("get(B) -> " + cache.get("B"));

        System.out.println("Removing D to make room for the TTL demonstration.");
        cache.remove("D");
        System.out.println("Adding a temporary entry with a 100 ms TTL.");
        cache.put("temporary", "Expires soon", Duration.ofMillis(100));
        Thread.sleep(150);
        System.out.println("get(temporary) after expiration -> " + cache.get("temporary"));

        System.out.println("\nCache statistics:");
        printStatistics(cache);

        System.out.println("\nResetting statistics.");
        cache.resetStatistics();
        printStatistics(cache);
    }

    /**
     * Prints the cache's hit, miss, and eviction counters.
     *
     * @param cache cache whose statistics are printed
     */
    private static void printStatistics(LRUCache<?, ?> cache) {
        System.out.println("Hits: " + cache.getHits());
        System.out.println("Misses: " + cache.getMisses());
        System.out.println("Evictions: " + cache.getEvictions());
    }
}
