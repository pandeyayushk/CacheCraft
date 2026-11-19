package com.ayush;

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
        put(key, value, 0);
    }

    /**
     * Adds or updates an entry with a time-to-live duration in nanoseconds.
     * Evicts the least recently used entry if necessary.
     *
     * @param key key associated with the value
     * @param value value to store
     * @param ttl time-to-live duration in nanoseconds; zero means no expiration
     */
    public void put(K key, V value, long ttl){
        Objects.requireNonNull(key,"Key should not be null.");
        Objects.requireNonNull(value,"Value should not be null");
        if(!cache.containsKey(key)&&cache.size()==maximumCapacity){
            evictLeastRecentlyUsed();
        }
        cache.put(key,new Entry<>(value, ttl));
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

    public int size(){
        return cache.size();
    }

    public void clear(){
        cache.clear();
    }

    public boolean containsKey(K key) {
        Objects.requireNonNull(key,"Key should not be null.");
        return cache.containsKey(key);
    }
}
