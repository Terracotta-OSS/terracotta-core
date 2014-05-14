/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
* A drop-in replacement {@code ThreadLocal} implementation that does not leak
 *   when thread-local values reference the {@code ThreadLocal} object.
 * The code is optimised to cope with frequently changing values.  
 * <p>
 * In comparison to plain {@code ThreadLocal}, this implementation:<ul>
 * <li>from the point of view of a single thread,
 *   each thread-local
 *   {code #get} requires access to four objects instead of two
 * <li>is fractionally slower in terms of CPU cycles for {code #get}
 * <li>uses around twice the memory for each thead-local value
 * <li>uses around four times the memory for each {@code ThreadLocal}
 * <li>may release thread-local values for garbage collection more promptly
 * </ul>
 */ 
public class VicariousThreadLocal<T> extends ThreadLocal<T> {
    /**
     * Maps a unique WeakReference onto each Thread.
     */
    private static final ThreadLocal<WeakReference<Thread>> weakThread =
        new ThreadLocal<WeakReference<Thread>>();
        
    /**
     * Returns a unique object representing the current thread.
     * Although we use a weak-reference to the thread,
     *   we could use practically anything
     *   that does not reference our class-loader.
     */
    static WeakReference<Thread> currentThreadRef() {
        WeakReference<Thread> ref = weakThread.get();
        if (ref == null) {
            ref = new WeakReference<Thread>(Thread.currentThread());
            weakThread.set(ref);
        }
        return ref;
    }
    
    /**
     * Object representing an uninitialised value.
     */
    private static final Object UNINITIALISED = new Object();
    
    /**
     * Actual ThreadLocal implementation object.
     */
    private final ThreadLocal<WeakReference<Holder>> local =
        new ThreadLocal<WeakReference<Holder>>();
    
    /**
     * Maintains a strong reference to value for each thread,
     *   so long as the Thread has not been collected.
     * Note, alive Threads strongly references the WeakReference&lt;Thread>
     *   through weakThread.
     */
    private volatile Holder strongRefs;
    
    /**
     * Compare-and-set of {@link #strongRefs}.
     */
    private static final AtomicReferenceFieldUpdater<VicariousThreadLocal,Holder> strongRefsUpdater =
        AtomicReferenceFieldUpdater.newUpdater(VicariousThreadLocal.class, Holder.class, "strongRefs");
    
    /**
     * Queue of Holders belonging to exited threads.
     */
    private final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        
    /**
     * Creates a new {@code VicariousThreadLocal}.
     */
    public VicariousThreadLocal() {
      //
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        final Holder holder;
        WeakReference<Holder> ref = local.get();
        if (ref != null) {
            holder = ref.get();
            Object value = holder.value;
            if (value != UNINITIALISED) {
                return (T)value;
            }
        } else {
            holder = createHolder();
        }
        T value = initialValue();
        holder.value = value;
        return value;
    }
    
    @Override
    public void set(T value) {
        WeakReference<Holder> ref = local.get();
        final Holder holder =
            ref!=null ? ref.get() : createHolder();
        holder.value = value;
    }
    
    /**
     * Creates a new holder object, and registers it appropriately.
     * Also polls for thread-exits.
     */
    private Holder createHolder() {
        poll();
        Holder holder = new Holder(queue);
        WeakReference<Holder> ref = new WeakReference<Holder>(holder);
        
        Holder old;
        do {
            old = strongRefs;
            holder.next = old;
        } while (!strongRefsUpdater.compareAndSet(this, old, holder));
        
        local.set(ref);
        return holder;
    }
    
    @Override
    public void remove() {
        WeakReference<Holder> ref = local.get();
        if (ref != null) {
            ref.get().value = UNINITIALISED;
        }
    }
    
    /**
     * Check if any strong references need should be removed due to thread exit.
     */
    public void poll() {
        synchronized (queue) {
            // Remove queued references.
            // (Is this better inside or out?)
            if (queue.poll() == null) {
                // Nothing to do.
                return;
            }
            while (queue.poll() != null) {
                // Discard.
            }
            
            // Remove any dead holders.
            Holder first = strongRefs;
            if (first == null) {
                // Unlikely...
                return;
            }
            Holder link = first;
            Holder next = link.next;
            while (next != null) {
                if (next.get() == null) {
                    next = next.next;
                    link.next = next;
                } else {
                    link = next;
                    next = next.next;
                }
            }
            
            // Remove dead head, possibly.
            if (first.get() == null) {
                if (!strongRefsUpdater.weakCompareAndSet(
                        this, first, first.next
                )) {
                    // Something else has come along.
                    // Just null out - next search will remove it.
                    first.value = null;
                }
            }
        }
    }
    
    /**
     * Holds strong reference to a thread-local value.
     * The WeakReference is to a thread-local representing the current thread.
     */
    private static class Holder extends WeakReference<Object> {
        /**
         * Construct a new holder for the current thread.
         */
        Holder(ReferenceQueue<Object> queue) {
            super(currentThreadRef(), queue);
        }
        /**
         * Next holder in chain for this thread-local.
         */
        Holder next;
        /**
         * Current thread-local value.
         * {@link #UNINITIALISED} represents an uninitialised value.
         */
        Object value = UNINITIALISED;
    }
}
