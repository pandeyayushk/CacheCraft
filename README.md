# CacheCraft

CacheCraft is a Java 21 in-memory cache that combines least-recently-used (LRU) eviction, optional time-to-live (TTL) expiration, and basic cache statistics. It is built with Maven and includes JUnit 5 tests and a runnable demonstration.

## Key features

- Fixed-capacity storage with LRU eviction.
- Optional per-entry expiration using `java.time.Duration`.
- Thread-safe public cache operations using `ReentrantReadWriteLock`.
- Hit, miss, and capacity-eviction counters.
- Explicit statistics reset; clearing entries does not reset the counters.
- Tests for core behavior, TTL expiration, statistics, and concurrent operations.

## Technology stack

- Java 21
- Maven
- JUnit Jupiter 5.11.4 for tests
- Maven Surefire 3.5.2 for running tests

The cache implementation uses only Java standard library classes at runtime.

## How LRU eviction works

The cache stores entries in a `LinkedHashMap` configured for access order. Reading or updating an existing key makes it the most recently used entry. When a new key is inserted at capacity, the cache removes the least recently used entry first.

Before capacity eviction, `put()` removes expired entries. Therefore, an expired entry freed during this cleanup does not count as a capacity eviction. Updating an existing key replaces its entry without evicting another key.

## How TTL expiration works

Use `put(key, value, Duration)` to set a TTL. `Duration.ZERO` means the entry does not expire. Null and negative durations are rejected. The entry records an expiration time using `System.nanoTime()`.

Expiration is lazy: there is no background cleanup thread. Expired entries are removed when encountered by `get()` or `containsKey()`, and in bulk during `put()` and `size()`. Until one of those operations runs, an expired entry may remain in the map. `remove()` always removes the entry and returns its stored value, even if its TTL has elapsed.

## Thread safety and locking

The cache protects access to its `LinkedHashMap` and mutable statistics with a `ReentrantReadWriteLock`. `get()` takes the write lock because `LinkedHashMap.get()` changes the map's access order. Operations that may modify the map, including `put()`, `remove()`, `clear()`, `size()` expiration cleanup, and `containsKey()` expiration removal, also use the write lock.

The statistics getters only read counters, so they use the read lock. This lets multiple statistics reads proceed together while excluding counter updates and other write-locked operations. Cache operations themselves are serialized, including `get()` calls.

## Cache statistics

| Counter | Meaning |
| --- | --- |
| `hits` | Successful `get()` calls. |
| `misses` | Unsuccessful `get()` calls, including missing and expired keys. |
| `evictions` | Entries removed because a new key was inserted while the cache was at capacity. Expiration cleanup is not an eviction. |

Read counters with `getHits()`, `getMisses()`, and `getEvictions()`. Call `resetStatistics()` to set all three to zero. `clear()` removes cached entries but preserves statistics. Calls to `containsKey()`, `remove()`, and `size()` do not affect hit or miss counters.

## Project structure

```text
CacheCraft/
+-- pom.xml
+-- src/
    +-- main/java/com/ayush/
    |   +-- CacheCraftDemo.java
    |   +-- Entry.java
    |   +-- LRUCache.java
    +-- test/java/com/ayush/
        +-- LRUCacheConcurrencyTest.java
        +-- LRUCacheStatisticsTest.java
        +-- LRUCacheTest.java
```

`Entry<V>` holds a value and its expiration metadata. `CacheCraftDemo.java` demonstrates the cache API.

## Installation and Maven setup

Install a Java 21 JDK and Maven, then open a terminal in the project root. This project does not include a Maven Wrapper, so use the installed `mvn` command.

Compile the project:

```bash
mvn compile
```

Run the complete build and test suite:

```bash
mvn clean test
```

## Usage

The two-argument `put()` stores an entry without expiration. The overload accepting a `Duration` stores an entry with a TTL.

For example, with capacity two, accessing `A` makes it more recently used than `B`. Inserting `C` then evicts `B`:

```java
LRUCache<String, String> cache = new LRUCache<>(2);

cache.put("A", "Apple");
cache.put("B", "Banana");

cache.get("A"); // A becomes most recently used

cache.put("C", "Cherry"); // B is evicted

System.out.println(cache.get("B")); // null
```

The complete example below shows TTL insertion and statistics as well:

```java
import com.ayush.LRUCache;

import java.time.Duration;

public class CacheExample {
    public static void main(String[] args) {
        LRUCache<String, String> cache = new LRUCache<>(100);

        cache.put("user:42", "Ayush");
        cache.put("session:42", "active", Duration.ofMinutes(15));

        String user = cache.get("user:42");
        System.out.println("User: " + user);
        System.out.println("Hits: " + cache.getHits());
        System.out.println("Misses: " + cache.getMisses());
        System.out.println("Evictions: " + cache.getEvictions());

        cache.resetStatistics();
    }
}
```

The cache capacity must be positive. Keys, values, and TTL durations must not be null.

## Run the demo

Compile and run the demo class from the project root:

```bash
mvn -DskipTests compile
java -cp target/classes com.ayush.CacheCraftDemo
```

In an IDE, run `com.ayush.CacheCraftDemo`'s `main()` method.

## Run the complete test suite

```bash
mvn clean test
```

The tests are under `src/test/java/com/ayush` and cover cache behavior, TTL cleanup, statistics, and concurrent public operations.

Run an individual suite with:

```bash
mvn -Dtest=LRUCacheConcurrencyTest test
mvn -Dtest=LRUCacheStatisticsTest test
```

## Example output

The demo prints output similar to the following:

```text
Adding three entries to a cache with capacity 3.
Accessing A makes it the most recently used entry.
get(A) -> Apple
Adding D evicts the least recently used entry, B.
get(B) -> null
Removing D to make room for the TTL demonstration.
Adding a temporary entry with a 100 ms TTL.
get(temporary) after expiration -> null

Cache statistics:
Hits: 1
Misses: 2
Evictions: 1

Resetting statistics.
Hits: 0
Misses: 0
Evictions: 0
```

## Current limitations and future improvements

- Cache operations use an exclusive write lock. This preserves map and LRU consistency, but serializes operations, including reads. A future design could evaluate a different data structure or locking strategy if parallel read throughput is needed.
- `containsKey()` preserves access order while inspecting an entry, which requires iterating through the map when the key exists.
- Expired entries are cleaned lazily, so they can occupy capacity until an operation triggers cleanup.
- `Duration.toNanos()` cannot represent arbitrarily large durations and can throw `ArithmeticException` for values outside its range.
- The cache is in-memory only; it does not provide persistence, distributed coordination, or background expiration.
- TTL tests use elapsed-time waits. An injectable time source could make those tests deterministic in a future iteration.
