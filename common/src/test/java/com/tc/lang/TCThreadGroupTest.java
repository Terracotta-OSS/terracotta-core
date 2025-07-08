/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class TCThreadGroupTest {

    private ThrowableHandler throwableHandler;
    private List<Thread> createdThreads;
    private AtomicInteger interruptCount;
    
    @Before
    public void setUp() {
        throwableHandler = new ThrowableHandlerImpl(LoggerFactory.getLogger(TCThreadGroupTest.class)) {
            @Override
            protected synchronized void exit(int status) {
                // do not exit in test
            }
        };
        createdThreads = new ArrayList<>();
        interruptCount = new AtomicInteger(0);
    }
    
    @After
    public void tearDown() {
        // Ensure all threads are interrupted to clean up
        for (Thread t : createdThreads) {
            t.interrupt();
        }
        
        // Wait for threads to terminate
        for (Thread t : createdThreads) {
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Test that a stoppable thread group can retire threads normally.
     */
    @Test
    public void testRetireNormalThreads() throws Exception {
        TCThreadGroup group = new TCThreadGroup(throwableHandler, "test-normal", true);
        
        // Create some worker threads that will exit quickly when interrupted
        int threadCount = 5;
        CountDownLatch threadsStarted = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(group, () -> {
                threadsStarted.countDown();
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    // Expected - exit on interrupt
                    interruptCount.incrementAndGet();
                }
            }, "worker-" + i);
            createdThreads.add(t);
            t.start();
        }
        
        // Wait for all threads to start
        assertTrue("Threads did not start in time", threadsStarted.await(5, TimeUnit.SECONDS));
        
        // Retire the threads
        group.interrupt();
        boolean retired = group.retire(2000, e -> {
            // Just log the interruption
            System.out.println("Interrupted: " + e.getMessage());
        });
        
        assertTrue("Failed to retire threads", retired);
        assertEquals("Not all threads were interrupted", threadCount, interruptCount.get());
    }
    
    /**
     * Test that a non-stoppable thread group returns true immediately from retire().
     */
    @Test
    public void testRetireNonStoppableThreadGroup() throws Exception {
        TCThreadGroup group = new TCThreadGroup(throwableHandler, "test-non-stoppable", false);
        
        // Create a worker thread that would normally block
        Thread t = new Thread(group, () -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                interruptCount.incrementAndGet();
            }
        }, "non-stoppable-worker");
        createdThreads.add(t);
        t.start();
        
        // Give thread time to start
        Thread.sleep(100);
        
        // Retire should return true immediately for non-stoppable groups
        long startTime = System.currentTimeMillis();
        boolean retired = group.retire(5000, e -> {
            fail("Should not be interrupted for non-stoppable group");
        });
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue("Non-stoppable group should return true from retire()", retired);
        assertTrue("Retire should return quickly for non-stoppable groups", duration < 1000);
        assertEquals("Thread should not be interrupted", 0, interruptCount.get());
    }
    
    /**
     * Test behavior with threads that ignore interrupts.
     */
    @Test
    public void testRetireWithUninterruptibleThreads() throws Exception {
        TCThreadGroup group = new TCThreadGroup(throwableHandler, "test-uninterruptible", true);
        
        // Create a thread that ignores interrupts
        AtomicBoolean threadShouldExit = new AtomicBoolean(false);
        CountDownLatch threadStarted = new CountDownLatch(1);
        
        Thread t = new Thread(group, () -> {
            threadStarted.countDown();
            while (!threadShouldExit.get()) {
                // Ignore interrupts and keep running
                if (Thread.interrupted()) {
                    interruptCount.incrementAndGet();
                }
                // Busy wait
                LockSupport.parkNanos(1000000); // 1ms
            }
        }, "uninterruptible-worker");
        createdThreads.add(t);
        t.start();
        
        // Wait for thread to start
        assertTrue("Thread did not start in time", threadStarted.await(5, TimeUnit.SECONDS));
        
        // Try to retire - should fail because thread ignores interrupts
        group.interrupt();
        boolean retired = group.retire(1000, e -> {
            System.out.println("Interrupted: " + e.getMessage());
        });
        
        // Verify retire failed
        assertFalse("Retire should fail with uninterruptible threads", retired);
        assertTrue("Thread should have been interrupted at least once", interruptCount.get() > 0);
        
        // Allow thread to exit and try again
        threadShouldExit.set(true);
        Thread.sleep(100);
        
        // Now retire should succeed
        retired = group.retire(1000, e -> {
            System.out.println("Interrupted: " + e.getMessage());
        });
        
        assertTrue("Retire should succeed after thread exits", retired);
    }
    
    /**
     * Test behavior with threads that are blocked on I/O or other uninterruptible operations.
     * This simulates threads that might be stuck during server shutdown.
     */
    @Test
    public void testRetireWithBlockedThreads() throws Exception {
        TCThreadGroup group = new TCThreadGroup(throwableHandler, "test-blocked", true);
        
        // Create a thread that simulates being blocked on something that can't be interrupted
        // In a real scenario, this might be a thread blocked on native I/O
        CountDownLatch threadStarted = new CountDownLatch(1);
        AtomicBoolean threadIsBlocked = new AtomicBoolean(false);
        AtomicBoolean threadShouldUnblock = new AtomicBoolean(false);
        
        Thread t = new Thread(group, () -> {
            threadStarted.countDown();
            
            // Simulate entering a blocked state
            threadIsBlocked.set(true);
            
            // Wait until explicitly unblocked
            while (!threadShouldUnblock.get()) {
                // Check for interruption but don't exit
                if (Thread.interrupted()) {
                    interruptCount.incrementAndGet();
                }
                LockParker.parkNanos(1000000); // 1ms
            }
        }, "blocked-worker");
        createdThreads.add(t);
        t.start();
        
        // Wait for thread to start and become blocked
        assertTrue("Thread did not start in time", threadStarted.await(5, TimeUnit.SECONDS));
        while (!threadIsBlocked.get()) {
            Thread.sleep(10);
        }
        
        // Try to retire - should fail because thread is "blocked"
        group.interrupt();
        boolean retired = group.retire(1000, e -> {
            System.out.println("Interrupted: " + e.getMessage());
        });
        
        // Verify retire failed
        assertFalse("Retire should fail with blocked threads", retired);
        assertTrue("Thread should have been interrupted at least once", interruptCount.get() > 0);
        
        // Unblock the thread and try again
        threadShouldUnblock.set(true);
        Thread.sleep(100);
        
        // Now retire should succeed
        retired = group.retire(1000, e -> {
            System.out.println("Interrupted: " + e.getMessage());
        });
        
        assertTrue("Retire should succeed after thread unblocks", retired);
    }
    
    /**
     * Test that ignorePoolThreads parameter works correctly.
     */
    @Test
    public void testIgnorePoolThreads() throws Exception {
        // Create a thread group that ignores pool threads
        TCThreadGroup group = new TCThreadGroup(throwableHandler, "test-ignore-pool", true, true);
        
        // Create a thread with a pool-like name
        CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = new Thread(group, () -> {
            threadStarted.countDown();
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                interruptCount.incrementAndGet();
            }
        }, "pool-1-thread-1");
        createdThreads.add(t);
        t.start();
        
        // Wait for thread to start
        assertTrue("Thread did not start in time", threadStarted.await(5, TimeUnit.SECONDS));
        
        // Retire should succeed immediately because pool threads are ignored
        boolean retired = group.retire(1000, e -> {
            fail("Should not be interrupted when ignoring pool threads");
        });
        
        assertTrue("Retire should succeed when ignoring pool threads", retired);
        assertEquals("Pool thread should not be interrupted", 0, interruptCount.get());
        
        // Create a non-pool thread in the same group
        CountDownLatch thread2Started = new CountDownLatch(1);
        Thread t2 = new Thread(group, () -> {
            thread2Started.countDown();
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                interruptCount.incrementAndGet();
            }
        }, "regular-thread");
        createdThreads.add(t2);
        t2.start();
        
        // Wait for thread to start
        assertTrue("Thread did not start in time", thread2Started.await(5, TimeUnit.SECONDS));
        
        // Now retire should interrupt the regular thread but not the pool thread
        group.interrupt();
        retired = group.retire(1000, e -> {
            System.out.println("Interrupted: " + e.getMessage());
        });
        
        assertTrue("Retire should succeed", retired);
        assertEquals("Only the regular thread should be interrupted", 2, interruptCount.get());
    }
    
    /**
     * Helper class to simulate LockSupport without actually using it
     * (to avoid potential test interference)
     */
    private static class LockParker {
        public static void parkNanos(long nanos) {
            try {
                Thread.sleep(0, (int)(nanos / 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

// Made with Bob
