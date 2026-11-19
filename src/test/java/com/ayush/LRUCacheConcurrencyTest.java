package com.ayush;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies cache behavior when its public operations are called concurrently.
 */
class LRUCacheConcurrencyTest {

    private static final int THREAD_COUNT = 6;
    private static final int OPERATIONS_PER_THREAD = 300;

    @Test
    void concurrentInsertionsRespectCapacity() throws Exception {
        LRUCache<Integer, Integer> cache = new LRUCache<>(100);
        AtomicInteger maximumObservedSize = new AtomicInteger();

        runConcurrently(THREAD_COUNT, worker -> {
            int firstKey = worker * OPERATIONS_PER_THREAD;
            for (int index = 0; index < OPERATIONS_PER_THREAD; index++) {
                cache.put(firstKey + index, index);
                maximumObservedSize.accumulateAndGet(cache.size(), Math::max);
            }
        });

        assertTrue(maximumObservedSize.get() <= 100,
                "Cache size should never exceed its configured capacity");
        assertEquals(100, cache.size());
    }

    @Test
    void concurrentReadsWritesAndRemovalsCompleteWithoutExceptions() throws Exception {
        LRUCache<Integer, Integer> cache = new LRUCache<>(100);
        for (int key = 0; key < 100; key++) {
            cache.put(key, key);
        }

        runConcurrently(THREAD_COUNT, worker -> {
            for (int index = 0; index < OPERATIONS_PER_THREAD; index++) {
                int key = (worker * OPERATIONS_PER_THREAD + index) % 100;
                cache.put(key, index);
                cache.get(key);
                cache.remove((key + 1) % 100);
            }
        });

        assertTrue(cache.size() <= 100);
    }

    @Test
    void concurrentInsertionsAtSmallCapacityNeverExceedLimit() throws Exception {
        int capacity = 5;
        LRUCache<Integer, Integer> cache = new LRUCache<>(capacity);
        AtomicInteger maximumObservedSize = new AtomicInteger();

        runConcurrently(THREAD_COUNT, worker -> {
            int firstKey = worker * OPERATIONS_PER_THREAD;
            for (int index = 0; index < OPERATIONS_PER_THREAD; index++) {
                cache.put(firstKey + index, index);
                maximumObservedSize.accumulateAndGet(cache.size(), Math::max);
            }
        });

        assertTrue(maximumObservedSize.get() <= capacity,
                "Cache size should never exceed its configured capacity");
        assertEquals(capacity, cache.size());
    }

    @Test
    void concurrentClearAndInsertionsCompleteWithoutExceptions() throws Exception {
        LRUCache<Integer, Integer> cache = new LRUCache<>(100);

        runConcurrently(THREAD_COUNT, worker -> {
            int firstKey = worker * OPERATIONS_PER_THREAD;
            for (int index = 0; index < OPERATIONS_PER_THREAD; index++) {
                if (worker % 2 == 0) {
                    cache.clear();
                } else {
                    cache.put(firstKey + index, index);
                }
            }
        });

        assertTrue(cache.size() <= 100);
    }

    /**
     * Releases workers together and propagates any worker failure to the test.
     *
     * @param workerCount number of worker threads to start
     * @param task operation performed by each worker
     * @throws Exception if a worker fails, coordination times out, or the test thread is interrupted
     */
    private static void runConcurrently(int workerCount, WorkerTask task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(workerCount);

        try {
            for (int worker = 0; worker < workerCount; worker++) {
                int workerId = worker;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for concurrent start");
                    }
                    task.run(workerId);
                    return null;
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS),
                    "All workers should be ready before concurrent execution starts");
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Executor did not terminate");
                }
            }
        }
    }

    @FunctionalInterface
    private interface WorkerTask {
        void run(int workerId) throws Exception;
    }
}
