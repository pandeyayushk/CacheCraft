package com.ayush;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Objects;

public class LRUCache <K,V>{

    private final int maximumCapacity;

    private final LinkedHashMap<K,Entry<V>> cache;
    /**
     * Initializing maximum cache limit and the LinkedHashMap to store cache entries
     * @param maximumCapacity
     */
    public LRUCache(int maximumCapacity) {
        if(maximumCapacity<=0){
            throw new IllegalArgumentException("Maximum Capacity must be positive value");
        }
        this.maximumCapacity = maximumCapacity;
        this.cache=new LinkedHashMap<>(this.maximumCapacity,0.75f,true);
    }

    /**
     * Adds or updates an entry.
     * Evicts the least recently used entry if necessary.
     */
    public void put(K key,V value){
        put(key, value, Duration.ZERO);
    }

    /**
     * Adds or updates an entry with a time-to-live duration.
     * Evicts the least recently used entry if necessary.
     *
     * @param key key associated with the value
     * @param value value to store
     * @param ttl time-to-live duration; zero means no expiration
     */
    public void put(K key, V value, Duration ttl){
        Objects.requireNonNull(key,"Key should not be null.");
        Objects.requireNonNull(value,"Value should not be null");
        Objects.requireNonNull(ttl,"Time-to-live must not be null.");
        Entry<V> newEntry = new Entry<>(value, ttl);
        removeExpiredEntries();
        if(!cache.containsKey(key)&&cache.size()==maximumCapacity){
            evictLeastRecentlyUsed();
        }
        cache.put(key,newEntry);
    }

    /**
     * Retrieves an entry and updates its access order.
     *
     * @return value associated with the key, or null if absent
     */
    public V get(K key){
        Objects.requireNonNull(key,"Key should not be null.");
        Entry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    /**
     * Removes a specific entry by key.
     *
     * @return removed value, or null if absent
     */
    public V remove(K key){
        Objects.requireNonNull(key,"Key should not be null.");
        Entry<V> removedEntry = cache.remove(key);
        return removedEntry == null ? null : removedEntry.getValue();
    }

    /**
     * Evicts the least recently used entry.
     */
    private void evictLeastRecentlyUsed() {
        if (cache.isEmpty()) {
            return;
        }

        K leastRecentlyUsedKey = cache.keySet().iterator().next();
        cache.remove(leastRecentlyUsedKey);
    }

    /**
     * Removes all entries whose time-to-live has elapsed.
     */
    private void removeExpiredEntries() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public int size(){
        removeExpiredEntries();
        return cache.size();
    }

    public void clear(){
        cache.clear();
    }

    public boolean containsKey(K key) {
        Objects.requireNonNull(key,"Key should not be null.");
        for (var cacheEntry : cache.entrySet()) {
            if (cacheEntry.getKey().equals(key)) {
                if (cacheEntry.getValue().isExpired()) {
                    cache.remove(key);
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
