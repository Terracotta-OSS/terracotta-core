/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.bulkload;

import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;

import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.exception.TCNotRunningException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.terracotta.toolkit.collections.map.AggregateServerMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Abhishek Sanoujam
 */
public class LocalBufferedMap<K, V> {
  private static final TCLogger          LOGGER                     = TCLogging.getLogger(LocalBufferedMap.class);
  private static final int               MAX_SIZEOF_DEPTH           = 1000;

  private static final String            CONCURRENT_TXN_LOCK_ID     = "local-buffer-static-concurrent-txn-lock-id";

  private static final Map               EMPTY_MAP                  = Collections.EMPTY_MAP;

  private static final int               LOCAL_MAP_INITIAL_CAPACITY = 128;
  private static final float             LOCAL_MAP_LOAD_FACTOR      = 0.75f;
  private static final int               LOCAL_MAP_INITIAL_SEGMENTS = 128;

  private final FlushToServerThread      flushToServerThread;
  private final AggregateServerMap<K, V> backend;
  private final int                      putsBatchByteSize;
  private final long                     batchTimeMillis;
  private final long                     throttlePutsByteSize;

  private volatile Map<K, Value<V>>      collectBuffer;
  private volatile Map<K, Value<V>>      flushBuffer;
  private volatile boolean               clearMap                   = false;
  private volatile AtomicLong            pendingOpsSize             = new AtomicLong();
  private final SizeOfEngine             sizeOfEngine;
  private final ReadWriteLock            bufferSwitchLock           = new ReentrantReadWriteLock();
  private final ReentrantLock            bulkLoadOnOffLock          = new ReentrantLock();

  private final Lock                     concurrentTransactionLock;
  public static int                      NO_VERSION                 = -1;
  public static int                      NO_CREATETIME              = -1;
  public static int                      NO_TTI                     = -1;
  public static int                      NO_TTL                     = -1;

  public LocalBufferedMap(String name, AggregateServerMap<K, V> backend, ToolkitInternal toolkit,
                          BulkLoadConstants bulkloadConstants) {
    this.backend = backend;
    this.collectBuffer = newMap();
    this.flushBuffer = EMPTY_MAP;
    this.concurrentTransactionLock = toolkit.getLock(CONCURRENT_TXN_LOCK_ID, ToolkitLockTypeInternal.CONCURRENT);
    this.flushToServerThread = new FlushToServerThread("BulkLoad Flush Thread [" + name + "]", this);
    flushToServerThread.setDaemon(true);
    sizeOfEngine = new DefaultSizeOfEngine(MAX_SIZEOF_DEPTH, true);
    putsBatchByteSize = bulkloadConstants.getBatchedPutsBatchBytes();
    batchTimeMillis = bulkloadConstants.getBatchedPutsBatchTimeMillis();
    throttlePutsByteSize = bulkloadConstants.getBatchedPutsThrottlePutsAtByteSize();
  }

  private Map<K, Value<V>> newMap() {
    return new ConcurrentHashMap<K, Value<V>>(LOCAL_MAP_INITIAL_CAPACITY, LOCAL_MAP_LOAD_FACTOR,
                                              LOCAL_MAP_INITIAL_SEGMENTS);
  }

  private void bufferSwitchReadUnlock() {
    bufferSwitchLock.readLock().unlock();
  }

  private void bufferSwitchReadLock() {
    bufferSwitchLock.readLock().lock();
  }

  private void bufferSwitchWriteUnlock() {
    bufferSwitchLock.writeLock().unlock();
  }

  private void bufferSwitchWriteLock() {
    bufferSwitchLock.writeLock().lock();
  }

  public V get(Object key) {
    bufferSwitchReadLock();
    try {
      // get from collectingBuffer or flushBuffer
      Value<V> v = collectBuffer.get(key);
      if (v != null && v.isRemove()) { return null; }
      if (v != null) { return v.getValue(); }
      v = flushBuffer.get(key);
      if (v != null && v.isRemove()) { return null; }
      return v == null ? null : v.getValue();
    } finally {
      bufferSwitchReadUnlock();
    }
  }

  public V remove(K key, final long version) {
    RemoveValue remove = new RemoveValue(version);
    bufferSwitchReadLock();
    try {
      Value<V> old = collectBuffer.put(key, remove);
      if (old == null) {
        pendingOpsSize.addAndGet(sizeOfEngine.sizeOf(key, remove, null).getCalculated());
        return null;
      } else {
        return old.isRemove() ? null : old.getValue();
      }
    } finally {
      bufferSwitchReadUnlock();
    }
  }

  public boolean containsKey(Object key) {
    bufferSwitchReadLock();
    try {
      Value<V> v = collectBuffer.get(key);
      if (v != null) { return !v.isRemove(); }
      v = flushBuffer.get(key);
      if (v == null || v.isRemove()) {
        return false;
      } else {
        return true;
      }
    } finally {
      bufferSwitchReadUnlock();
    }
  }

  public int getSize() {
    int size = 0;
    bufferSwitchReadLock();
    try {
      Map<K, Value<V>> localCollectingMap = collectBuffer;
      Map<K, Value<V>> localFlushMap = flushBuffer;
      for (Entry<K, Value<V>> e : localCollectingMap.entrySet()) {
        if (e.getValue() != null && !e.getValue().isRemove()) {
          size++;
        }
      }
      for (Entry<K, Value<V>> e : localFlushMap.entrySet()) {
        if (e.getValue() != null && !e.getValue().isRemove()) {
          size++;
        }
      }
    } finally {
      bufferSwitchReadUnlock();
    }
    return size;
  }

  public void clear() {
    bufferSwitchReadLock();
    try {
      collectBuffer.clear();
      flushBuffer.clear();
      // mark the backend to be cleared
      this.clearMap = true;
      pendingOpsSize.set(0);
    } finally {
      bufferSwitchReadUnlock();
    }
  }

  public Set<K> getKeys() {
    bufferSwitchReadLock();
    try {
      Set<K> keySet = new HashSet<K>(collectBuffer.keySet());
      keySet.addAll(flushBuffer.keySet());
      return keySet;
    } finally {
      bufferSwitchReadUnlock();
    }
  }

  public Set<Map.Entry<K, V>> entrySet() {
    Set<Entry<K, V>> rv = new HashSet<Map.Entry<K, V>>();
    bufferSwitchReadLock();
    try {
      addEntriesToSet(rv, collectBuffer);
      addEntriesToSet(rv, flushBuffer);
    } finally {
      bufferSwitchReadUnlock();
    }
    return rv;
  }

  private void addEntriesToSet(Set<Entry<K, V>> rv, Map<K, Value<V>> map) {
    for (Entry<K, Value<V>> entry : map.entrySet()) {
      final K key = entry.getKey();
      Value<V> wrappedValue = entry.getValue();
      final V value = wrappedValue.getValue();
      if (!wrappedValue.isRemove()) {
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
    Value<V> wrappedValue = new Value(value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    Value<V> rv = null;
    bufferSwitchReadLock();
    try {
      rv = collectBuffer.put(key, wrappedValue);
    } finally {
      bufferSwitchReadUnlock();
    }
    if (rv == null) {
      throttleIfNecessary(pendingOpsSize.addAndGet(sizeOfEngine.sizeOf(key, wrappedValue, null).getCalculated()));
    }
    return rv == null ? null : rv.isRemove() ? null : rv.getValue();
  }

  private void startThreadIfNecessary() {
    flushToServerThread.start();
  }

  private void throttleIfNecessary(long currentPendingSize) {
    while (currentPendingSize > throttlePutsByteSize) {
      sleepMillis(100);
      currentPendingSize = pendingOpsSize.get();
    }
  }

  private void sleepMillis(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      // TODO: honour nonstop
    }
  }

  // unpause the flush thread
  public void startBuffering() {
    bulkLoadOnOffLock.lock();
    try {
      startThreadIfNecessary();
      if (flushToServerThread.isFinished()) {
        // sane formatter
        throw new AssertionError("Start Buffering called when flush thread has already finished");
      }
      flushToServerThread.unpause();
    } finally {
      bulkLoadOnOffLock.unlock();
    }

  }

  // flushes pending buffers and pauses the flushing thread
  public void flushAndStopBuffering() {
    bulkLoadOnOffLock.lock();
    try {
      // first flush contents of flushBuffer if flush already in progress.
      // flush thread cannot start flush once another (app) thread has called
      // this method as this is under same write-lock
      flushToServerThread.waitUntilFlushCompleteAndPause();
      // as no more puts can happen, directly drain the collectingMap to server
      switchBuffers(newMap());
      try {
        drainBufferToServer(flushBuffer);
      } catch (RejoinException e) {
        LOGGER.warn("error during flushAndStopBuffering " + e);
      } catch (TCNotRunningException e) {
        LOGGER.info("Ignoring TCNotRunningException while flushAndStopBuffering " + e);
      } finally {
        flushBuffer = EMPTY_MAP;
      }

    } finally {
      bulkLoadOnOffLock.unlock();
    }

  }

  /**
   * Only called by flush thread regularly
   * 
   * @param thread
   */
  private void doPeriodicFlush(FlushToServerThread thread) {
    Map<K, Value<V>> localMap = newMap();
    // mark flush in progress, done under write-lock
    if (!thread.markFlushInProgress()) return;
    switchBuffers(localMap);
    try {
      drainBufferToServer(flushBuffer);
    } catch (RejoinException e) {
      LOGGER.warn("error during doPeriodicFlush " + e);
    } catch (TCNotRunningException e) {
      LOGGER.info("Ignoring TCNotRunningException while doPeriodicFlush " + e);
    } finally {
      flushBuffer = EMPTY_MAP;
      thread.markFlushComplete();
    }
  }

  // This method is always called under write lock.
  private void switchBuffers(Map<K, Value<V>> newBuffer) {
    bufferSwitchWriteLock();
    try {
      flushBuffer = collectBuffer;
      collectBuffer = newBuffer;
      pendingOpsSize.set(0);
    } finally {
      bufferSwitchWriteUnlock();
    }
  }

  private void drainBufferToServer(final Map<K, Value<V>> buffer) {
    clearIfNecessary();
    if (buffer.isEmpty()) { return; }
    backend.drainBufferToServer(buffer);

  }

  private void clearIfNecessary() {
    if (clearMap) {
      final Lock lock = concurrentTransactionLock;
      lock.lock();
      try {
        backend.clear();
      } finally {
        lock.unlock();
        // reset
        clearMap = false;
      }
    }
  }

  private static class FlushToServerThread extends Thread {

    enum State {
      NOT_STARTED, PAUSED, SLEEP, FLUSH, FINISHED
    }

    private final LocalBufferedMap localBufferedMap;
    private State                  state = State.NOT_STARTED;

    public FlushToServerThread(String name, LocalBufferedMap localBufferedMap) {
      super(name);
      this.localBufferedMap = localBufferedMap;
    }

    public void unpause() {
      moveTo(State.PAUSED, State.SLEEP);
    }

    @Override
    public void run() {
      while (!isFinished()) {
        waitUntilNotPaused();
        if (this.localBufferedMap.pendingOpsSize.get() < localBufferedMap.putsBatchByteSize) {
          // do not go to sleep if we've got enough work to do
          sleepFor(localBufferedMap.batchTimeMillis);
        }
        this.localBufferedMap.doPeriodicFlush(this);
      }
    }

    private void waitUntilNotPaused() {
      waitUntilStateChangesFrom(State.PAUSED);
    }

    private synchronized boolean isFinished() {
      return (state == State.FINISHED);
    }

    public boolean markFlushInProgress() {
      return moveTo(State.SLEEP, State.FLUSH);
    }

    public boolean markFlushComplete() {
      return moveTo(State.FLUSH, State.SLEEP);
    }

    public synchronized void waitUntilFlushCompleteAndPause() {
      waitUntilStateChangesFrom(State.FLUSH);
      moveTo(State.SLEEP, State.PAUSED);
    }

    @Override
    public synchronized void start() {
      if (moveTo(State.NOT_STARTED, State.PAUSED)) {
        super.start();
      }
    }

    private void sleepFor(long millis) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private synchronized void waitUntilStateChangesFrom(State current) {
      while (state == current) {
        try {
          wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    private synchronized boolean moveTo(State oldState, State newState) {
      if (state == oldState) {
        state = newState;
        notifyAll();
        return true;
      }
      return false;
    }
  }

  public static class Value<T> {

    private final T    value;
    private final int  createTimeInSecs;
    private final int  customMaxTTISeconds;
    private final int  customMaxTTLSeconds;
    private final long version;

    Value(T value, long version, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
      this.value = value;
      this.createTimeInSecs = createTimeInSecs;
      this.customMaxTTISeconds = customMaxTTISeconds;
      this.customMaxTTLSeconds = customMaxTTLSeconds;
      this.version = version;
    }

    public T getValue() {
      return value;
    }

    public boolean isRemove() {
      return false;
    }

    public boolean isVersioned() {
      return this.version != NO_VERSION;
    }

    public int getCreateTimeInSecs() {
      return createTimeInSecs;
    }

    public int getCustomMaxTTISeconds() {
      return customMaxTTISeconds;
    }

    public int getCustomMaxTTLSeconds() {
      return customMaxTTLSeconds;
    }

    public long getVersion() {
      return version;
    }
  }

  static class RemoveValue<T> extends Value<T> {
    public RemoveValue(final long version) {
      super(null, version, NO_CREATETIME, NO_TTI, NO_TTL);
    }

    @Override
    public boolean isRemove() {
      return true;
    }
  }

  public boolean isKeyBeingRemoved(Object obj) {
    bufferSwitchReadLock();
    try {
      Value<V> v = collectBuffer.get(obj);
      if (v != null && v.isRemove()) { return true; }
      return false;
    } finally {
      bufferSwitchReadUnlock();
    }
  }

}
