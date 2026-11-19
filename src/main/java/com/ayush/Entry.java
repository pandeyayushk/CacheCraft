package com.ayush;

/**
 * Stores a cache value and its expiration time.
 *
 * @param <V> type of the value stored in the entry
 */
public class Entry<V> {

    private final V value;
    private final long expirationTime;

    /**
     * Creates an entry with a value and a time-to-live duration in nanoseconds.
     *
     * @param value value stored in the entry
     * @param ttl time-to-live duration in nanoseconds; zero means no expiration
     * @throws IllegalArgumentException if the time-to-live is negative
     */
    public Entry(V value, long ttl) {
        if (ttl < 0) {
            throw new IllegalArgumentException("Time-to-live must not be negative.");
        }
        this.value = value;
        this.expirationTime = ttl == 0 ? 0 : System.nanoTime() + ttl;
    }

    /**
     * Retrieves the value stored in the entry.
     *
     * @return value stored in the entry
     */
    public V getValue() {
        return value;
    }

    /**
     * Checks whether this entry has expired.
     *
     * @return true if the entry has expired, otherwise false
     */
    public boolean isExpired() {
        return expirationTime != 0 && System.nanoTime() - expirationTime >= 0;
    }
}
