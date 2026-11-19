package com.ayush;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LRUCache <K,V>{

    private final int maximumCapacity;

    private final LinkedHashMap<K,Entry<V>> cache;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
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
        writeLock.lock();
        try{
            Entry<V> newEntry = new Entry<>(value, ttl);
            removeExpiredEntries();
            if(!cache.containsKey(key)&&cache.size()==maximumCapacity){
                evictLeastRecentlyUsed();
            }
            cache.put(key,newEntry);
        }finally {
            writeLock.unlock();
        }

    }

    /**
     * Retrieves an entry and updates its access order.
     *
     * @return value associated with the key, or null if absent
     */
    public V get(K key){
        Objects.requireNonNull(key,"Key should not be null.");
        writeLock.lock();
        try {
            Entry<V> entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return entry.getValue();
        }finally {
            writeLock.unlock();
        }
    }

    /**
     * Removes a specific entry by key.
     *
     * Returns the stored value even if the entry has expired.
     *
     * @return removed value, or null if the key is absent
     */
    public V remove(K key){
        Objects.requireNonNull(key,"Key should not be null.");
        writeLock.lock();
        try {
            Entry<V> removedEntry = cache.remove(key);
            return removedEntry == null ? null : removedEntry.getValue();
        }finally {
            writeLock.unlock();
        }            
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
        writeLock.lock();
        try {
            removeExpiredEntries();
            return cache.size();
        } finally {
            writeLock.unlock();
        }
    }

    public void clear(){
        writeLock.lock();
        try {
            cache.clear();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Checks whether the cache contains a non-expired entry for the key.
     * Removes the entry if it has expired without updating its access order.
     *
     * @param key key to check
     * @return true if a non-expired entry exists, otherwise false
     */
    public boolean containsKey(K key) {
        Objects.requireNonNull(key,"Key should not be null.");
        writeLock.lock();
        try {
            if (!cache.containsKey(key)) {
                return false;
            }
            Iterator<Map.Entry<K, Entry<V>>> entries = cache.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<K, Entry<V>> cacheEntry = entries.next();
                if (cacheEntry.getKey().equals(key)) {
                    if (cacheEntry.getValue().isExpired()) {
                        entries.remove();
                        return false;
                    }
                    return true;
                }
            }
            return false;
        } finally {
            writeLock.unlock();
        }
    }
}
