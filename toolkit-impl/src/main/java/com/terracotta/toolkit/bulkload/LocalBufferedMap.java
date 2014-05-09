/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.bulkload;

import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Abhishek Sanoujam
 */
public class LocalBufferedMap<K, V> {
  private static final TCLogger          LOGGER                     = TCLogging.getLogger(LocalBufferedMap.class);
  private static final int               MAX_SIZEOF_DEPTH           = 1000;

  private static final int               LOCAL_MAP_INITIAL_CAPACITY = 128;
  private static final float             LOCAL_MAP_LOAD_FACTOR      = 0.75f;
  private static final int               LOCAL_MAP_INITIAL_SEGMENTS = 128;

  private final Timer                    timer;

  private final BufferBackend<K, V>      backend;
  private final int                      putsBatchByteSize;
  private final long                     batchTimeMillis;
  private final long                     throttlePutsByteSize;

  private volatile Map<K, BufferedOperation<V>>  flushBuffer;

  private ConcurrentMap<K, BufferedOperation<V>> collectBuffer;
  private ScheduledFuture<?>             flusher;
  private final Runnable                 flushRunnable;

  private final AtomicLong               pendingOpsByteSize         = new AtomicLong();
  private final SizeOfEngine             sizeOfEngine;
  private final ReadWriteLock            bufferSwitchLock           = new ReentrantReadWriteLock();
  private final Condition                bufferFullCondition        = bufferSwitchLock.writeLock().newCondition();

  public static int                      NO_VERSION                 = -1;
  public static int                      NO_CREATETIME              = -1;
  public static int                      NO_TTI                     = -1;
  public static int                      NO_TTL                     = -1;

  public LocalBufferedMap(final String name, BufferBackend<K, V> backend,
                          BulkLoadConstants bulkloadConstants, final TaskRunner taskRunner) {
    this.backend = backend;
    this.collectBuffer = newMap();
    this.flushBuffer = Collections.emptyMap();
    // TODO: Make this thing a same thread timer so we don't need synchronization for the flush task and we won't
    // block multiple threads.
    timer = taskRunner.newTimer("BulkLoad Flush Thread [" + name + "]");
    flushRunnable = new Runnable() {
      // Synchonized to prevent multiple flushes from happening at the same time
      @Override
      public synchronized void run() {
        doPeriodicFlush();
      }
    };
    sizeOfEngine = new DefaultSizeOfEngine(MAX_SIZEOF_DEPTH, true);
    putsBatchByteSize = bulkloadConstants.getBatchedPutsBatchBytes();
    batchTimeMillis = bulkloadConstants.getBatchedPutsBatchTimeMillis();
    throttlePutsByteSize = bulkloadConstants.getBatchedPutsThrottlePutsAtByteSize();
  }

  private ConcurrentMap<K, BufferedOperation<V>> newMap() {
    return new ConcurrentHashMap<K, BufferedOperation<V>>(LOCAL_MAP_INITIAL_CAPACITY, LOCAL_MAP_LOAD_FACTOR,
                                              LOCAL_MAP_INITIAL_SEGMENTS);
  }

  private void readUnlock() {
    bufferSwitchLock.readLock().unlock();
  }

  private void readLock() {
    bufferSwitchLock.readLock().lock();
  }

  private void writeUnlock() {
    bufferSwitchLock.writeLock().unlock();
  }

  private void writeLock() {
    bufferSwitchLock.writeLock().lock();
  }

  public V get(Object key) {
    readLock();
    try {
      // get from collectingBuffer or flushBuffer
      BufferedOperation<V> v = collectBuffer.get(key);
      if (v != null) { return v.getValue(); }
      v = flushBuffer.get(key);
      return v == null ? null : v.getValue();
    } finally {
      readUnlock();
    }
  }

  public V remove(K key, final long version) {
    BufferedOperation<V> remove = backend.createBufferedOperation(BufferedOperation.Type.REMOVE, key, null, version,
        NO_CREATETIME, NO_TTI, NO_TTL);
    readLock();
    try {
      checkBuffering();
      BufferedOperation<V> old = collectBuffer.put(key, remove);
      if (old == null) {
        pendingOpsByteSize.addAndGet(sizeOfEngine.sizeOf(key, null, null).getCalculated());
        return null;
      } else {
        return old.getValue();
      }
    } finally {
      readUnlock();
    }
  }

  public boolean containsKey(Object key) {
    readLock();
    try {
      BufferedOperation<V> v = collectBuffer.get(key);
      if (v != null) { return v.getValue() != null; }
      v = flushBuffer.get(key);
      return v != null && v.getValue() != null;
    } finally {
      readUnlock();
    }
  }

  public int getSize() {
    int size = 0;
    readLock();
    try {
      Map<K, BufferedOperation<V>> localCollectingMap = collectBuffer;
      Map<K, BufferedOperation<V>> localFlushMap = flushBuffer;
      for (Entry<K, BufferedOperation<V>> e : localCollectingMap.entrySet()) {
        if (e.getValue().getValue() != null) {
          size++;
        }
      }
      for (Entry<K, BufferedOperation<V>> e : localFlushMap.entrySet()) {
        if (e.getValue().getValue() != null) {
          size++;
        }
      }
    } finally {
      readUnlock();
    }
    return size;
  }

  public void clear() {
    writeLock();
    try {
      collectBuffer = newMap();
      flushBuffer = Collections.emptyMap();
      // mark the backend to be cleared (or not)
      pendingOpsByteSize.set(0);
    } finally {
      writeUnlock();
    }
  }

  public Set<K> getKeys() {
    readLock();
    try {
      Set<K> keySet = new HashSet<K>(collectBuffer.keySet());
      keySet.addAll(flushBuffer.keySet());
      return keySet;
    } finally {
      readUnlock();
    }
  }

  public Set<Map.Entry<K, V>> entrySet() {
    Set<Entry<K, V>> rv = new HashSet<Map.Entry<K, V>>();
    readLock();
    try {
      addEntriesToSet(rv, collectBuffer);
      addEntriesToSet(rv, flushBuffer);
    } finally {
      readUnlock();
    }
    return rv;
  }

  private void addEntriesToSet(Set<Entry<K, V>> rv, Map<K, BufferedOperation<V>> map) {
    for (Entry<K, BufferedOperation<V>> entry : map.entrySet()) {
      final K key = entry.getKey();
      BufferedOperation<V> wrappedValue = entry.getValue();
      final V value = wrappedValue.getValue();
      if (wrappedValue.getType() != BufferedOperation.Type.REMOVE) {
        rv.add(new Map.Entry<K, V>() {

          @Override
          public K getKey() {
            return key;
          }

          @Override
          public V getValue() {
            return value;
          }

          @Override
          public V setValue(V param) {
            throw new UnsupportedOperationException();
          }

        });
      }
    }
  }

  public V put(K key, V value, final long version, int createTimeInSecs, int customMaxTTISeconds,
               int customMaxTTLSeconds) {
    BufferedOperation<V> wrappedValue = backend.createBufferedOperation(BufferedOperation.Type.PUT, key, value,
        version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    BufferedOperation<V> rv = null;
    throttleIfNecessary();
    readLock();
    try {
      checkBuffering();
      rv = collectBuffer.put(key, wrappedValue);
      if (rv == null) {
        pendingOpsByteSize.addAndGet(sizeOfEngine.sizeOf(key, value, null).getCalculated());
      }
    } finally {
      readUnlock();
    }
    return rv == null ? null : rv.getValue();
  }

  public V putIfAbsent(K key, V value, final long version, int createTimeInSecs, int customMaxTTISeconds,
                       int customMaxTTLSeconds) {
    BufferedOperation<V> wrappedValue = backend.createBufferedOperation(BufferedOperation.Type.PUT_IF_ABSENT, key,
        value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    BufferedOperation<V> rv = null;
    throttleIfNecessary();
    readLock();
    try {
      checkBuffering();
      rv = collectBuffer.putIfAbsent(key, wrappedValue);
      if (rv == null) {
        pendingOpsByteSize.addAndGet(sizeOfEngine.sizeOf(key, value, null).getCalculated());
      }
    } finally {
      readUnlock();
    }
    return rv == null ? null : rv.getValue();
  }

  private void throttleIfNecessary() {
    if (pendingOpsByteSize.get() <= throttlePutsByteSize) {
      // check is a bit racy, but it's "close enough". We just want to avoid the writeLock in most cases.
      return;
    }
    writeLock();
    try {
      while (pendingOpsByteSize.get() > throttlePutsByteSize) {
        try {
          bufferFullCondition.await();
        } catch (InterruptedException e) {
          throw new ToolkitAbortableOperationException(e);
        }
      }
    } finally {
      writeUnlock();
    }
  }

  // unpause the flush thread
  public void startBuffering() {
    writeLock();
    try {
      checkState(flusher == null, "Already buffering.");
      flusher = timer.scheduleWithFixedDelay(flushRunnable, batchTimeMillis, batchTimeMillis, TimeUnit.MILLISECONDS);
    } finally {
      writeUnlock();
    }
  }

  // flushes pending buffers and pauses the flushing thread
  public void flushAndStopBuffering() {
    writeLock();
    try {
      checkBuffering();
      flusher.cancel(false);
      flusher = null;
    } finally {
      writeUnlock();
    }
    flush();
  }

  private void doPeriodicFlush() {
    do {
      // mark flush in progress, done under write-lock
      switchBuffers();
      try {
        drainBufferToServer(flushBuffer);
      } catch (RejoinException e) {
        LOGGER.warn("error during doPeriodicFlush", e);
      } catch (TCNotRunningException e) {
      } finally {
        flushBuffer = Collections.emptyMap();
      }
    } while (pendingOpsByteSize.get() >= putsBatchByteSize);
  }

  // This method is always called under write lock.
  private void switchBuffers() {
    writeLock();
    try {
      checkState(flushBuffer.isEmpty(), "Flush buffer is non-empty!");
      if (collectBuffer.isEmpty()) {
        // short circuit when there's nothing to flush
        return;
      }
      flushBuffer = collectBuffer;
      collectBuffer = newMap();
      pendingOpsByteSize.set(0);
      bufferFullCondition.signalAll();
    } finally {
      writeUnlock();
    }
  }

  private void drainBufferToServer(final Map<K, BufferedOperation<V>> buffer) {
    if (buffer.isEmpty()) { return; }
    backend.drain(buffer);
  }

  public boolean isKeyBeingRemoved(Object obj) {
    readLock();
    try {
      BufferedOperation<V> v = collectBuffer.get(obj);
      return v != null && v.getType() == BufferedOperation.Type.REMOVE;
    } finally {
      readUnlock();
    }
  }

  public void flush() {
    try {
      timer.schedule(flushRunnable, 0, TimeUnit.MILLISECONDS).get();
    } catch (Exception e) {
      LOGGER.warn("error during flushAndStopBuffering ", e);
    }
  }

  private void checkBuffering() {
    checkState(flusher != null, "Not buffering");
  }
}
