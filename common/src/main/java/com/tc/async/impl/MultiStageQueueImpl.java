/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.async.impl;

import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.async.api.SpecializedEventContext;
import com.tc.async.impl.AbstractStageQueueImpl.HandledContext;
import com.tc.async.impl.AbstractStageQueueImpl.NullStageQueueStatsCollector;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;
import com.tc.util.Assert;
import com.tc.util.UpdatableFixedHeap;
import com.tc.util.concurrent.QueueFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tc.async.impl.AbstractStageQueueImpl.DirectExecuteContext;
import static com.tc.async.impl.AbstractStageQueueImpl.SourceQueue;
import static com.tc.async.impl.AbstractStageQueueImpl.StageQueueStatsCollector;
import static com.tc.async.impl.AbstractStageQueueImpl.StageQueueStatsCollectorImpl;

/**
 * This StageQueueImpl represents the sink and gives a handle to the source. We are internally just using a queue
 * since our queues are locally processed. This class can be replaced with a distributed queue to enable processing
 * across process boundaries.
 */
public class MultiStageQueueImpl<EC> implements StageQueue<EC> {

  static final String FINDSTRATEGY_PROPNAME = "tc.stagequeueimpl.findstrategy";


  private static final ShortestFindStrategy SHORTEST_FIND_STRATEGY;

  static {
    ShortestFindStrategy strat = ShortestFindStrategy.PARTITION;
    try {
      strat = chooseStrategy(ShortestFindStrategy.PARTITION);
    } catch (Throwable t) {
    }
    SHORTEST_FIND_STRATEGY = strat;
  }

  static enum ShortestFindStrategy {
    BRUTE,
    PARTITION
  }

  private final boolean moduloAnd;
  private final int moduleMask;
  private final int PARTITION_SHIFT;
  private final String stageName;
  private final ShortestFindStrategy myShortestFindStrategy;
  private final TCLogger logger;
  private final MultiSourceQueueImpl<ContextWrapper<EC>>[] sourceQueues;
  private volatile boolean closed = false;
  private volatile int fcheck = 0;  // used to start the shortest queue search
  private AtomicInteger partitionHand =new AtomicInteger(0);

  /**
   * The Constructor.
   *
   * @param queueCount : Number of queues working on this stage
   * @param queueFactory : Factory used to create the queues
   * @param loggerProvider : logger
   * @param stageName : The stage name
   * @param queueSize : Max queue Size allowed
   */
  @SuppressWarnings("unchecked")
  MultiStageQueueImpl(int queueCount,
                      QueueFactory<ContextWrapper<EC>> queueFactory,
                      TCLoggerProvider loggerProvider,
                      String stageName,
                      int queueSize) {
    Assert.eval(queueCount > 0);

    myShortestFindStrategy = SHORTEST_FIND_STRATEGY;

    if (queueCount >= 8) {
      PARTITION_SHIFT = 2;
    } else {
      PARTITION_SHIFT = 1;
    }

    this.logger = loggerProvider.getLogger(Sink.class.getName() + ": " + stageName);
    this.stageName = stageName;
    this.sourceQueues = new MultiSourceQueueImpl[queueCount];
    createWorkerQueues(queueCount, queueFactory, queueSize, stageName);

    int sz=sourceQueues.length;
    if (Integer.bitCount(sz) == 1) {
      this.moduloAnd = true;
      this.moduleMask = sz - 1;
    } else {
      this.moduloAnd = false;
      this.moduleMask = 0;
    }
  }

  private static ShortestFindStrategy chooseStrategy(ShortestFindStrategy defaultVal) {
    String stratName = System.getProperty(FINDSTRATEGY_PROPNAME, defaultVal.name());
    for (ShortestFindStrategy s : ShortestFindStrategy.values()) {
      if (s.name().toUpperCase().equals(stratName.toUpperCase())) {
        return s;
      }
    }
    System.err.println("Unrecognized '" + FINDSTRATEGY_PROPNAME + "' value: " + stratName + "; using: " + defaultVal);
    return defaultVal;
  }

  private int moduloQueueCount(int i) {
    if (moduloAnd) {
      return i & moduleMask;
    } else {
      return i % this.sourceQueues.length;
    }
  }

  private void createWorkerQueues(int queueCount, QueueFactory<ContextWrapper<EC>> queueFactory, int queueSize, String stage) {
    StageQueueStatsCollector statsCollector = new NullStageQueueStatsCollector(stage);
    BlockingQueue<ContextWrapper<EC>> q = null;

    if (queueSize != Integer.MAX_VALUE) {
      queueSize = (int) Math.ceil(((double) queueSize) / queueCount);
    }
    Assert.eval(queueSize > 0);

    for (int i = 0; i < queueCount; i++) {
      q = queueFactory.createInstance(queueSize);
      this.sourceQueues[i] = new MultiSourceQueueImpl<ContextWrapper<EC>>(q, i, statsCollector);
    }
  }

  @Override
  public Source<ContextWrapper<EC>> getSource(int index) {
    return (index < 0 || index >= this.sourceQueues.length) ? null : this.sourceQueues[index];
  }

  @Override
  public void setClosed(boolean closed) {
    this.closed = closed;
  }

  @Override
  public void addSingleThreaded(EC context) {
    Assert.assertNotNull(context);
    Assert.assertFalse(context instanceof MultiThreadedEventContext);
    if (closed) {
      throw new IllegalStateException("closed");
    }
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Added:" + context + " to:" + this.stageName);
    }

    boolean interrupted = Thread.interrupted();
    ContextWrapper<EC> wrapper = new HandledContext<EC>(context);
    try {
      while (true) {
        try {
          this.sourceQueues[0].put(wrapper);
          break;
        } catch (InterruptedException e) {
          this.logger.debug("StageQueue Add: " + e);
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void addMultiThreaded(EC context) {
    Assert.assertNotNull(context);
    Assert.assertTrue(context instanceof MultiThreadedEventContext);
    if (closed) {
      throw new IllegalStateException("closed");
    }
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Added:" + context + " to:" + this.stageName);
    }

    // NOTE:  We don't currently consult the predicate for multi-threaded events (the only implementation always returns true, in any case).
    boolean interrupted = Thread.interrupted();
    MultiThreadedEventContext cxt = (MultiThreadedEventContext) context;
    int index = getSourceQueueFor(cxt);
    ContextWrapper<EC> wrapper = (cxt.flush()) ? new FlushingHandledContext(context, index) : new HandledContext<EC>(
      context);
    try {
      while (true) {
        try {
          this.sourceQueues[index].put(wrapper);
          break;
        } catch (InterruptedException e) {
          this.logger.debug("StageQueue Add: " + e);
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void addSpecialized(SpecializedEventContext specialized) {
    if (closed) {
      throw new IllegalStateException("closed");
    }
    ContextWrapper<EC> wrapper = new DirectExecuteContext<EC>(specialized);
    boolean interrupted = Thread.interrupted();
    int index = getSourceQueueFor(specialized);
    try {
      while (true) {
        try {
          this.sourceQueues[index].put(wrapper);
          break;
        } catch (InterruptedException e) {
          this.logger.debug("StageQueue Add: " + e);
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  // TODO:  Way too busy. REALLY need a better way
  private int findShortestQueueIndex() {
    switch (SHORTEST_FIND_STRATEGY) {
      case PARTITION: {
        int offset = moduloQueueCount(partitionHand.getAndIncrement() << PARTITION_SHIFT);
        int min = Integer.MAX_VALUE;
        int can = -1;
        for (int i = 0; i < (1 << PARTITION_SHIFT); i++) {
          int checkMin = this.sourceQueues[offset].size();
          if (checkMin < min) {
            can = offset;
            min = checkMin;
          }
          offset = moduloQueueCount(offset + 1);
        }
        return can;
      }
      case BRUTE: {
        final int pointer = fcheck;
        int min = Integer.MAX_VALUE;
        int can = -1;
        // special case where context can go to any queue, pick the shortest
        for (int x = 0; x < this.sourceQueues.length; x++) {
          int index = moduloQueueCount(pointer + x);
          MultiSourceQueueImpl<ContextWrapper<EC>> impl = this.sourceQueues[index];
          if (impl.isEmpty()) {
            return index;
          } else {
            int checkMin = impl.size(); // concurrent access so just use current value.  this method is best efforts
            if (Math.min(min, checkMin) != min) {
              can = index;
              min = checkMin;
            }
          }
        }
        Assert.assertTrue(can >= 0 && can < this.sourceQueues.length);
        return can;
      }
    }
    throw new IllegalStateException();
  }

  private int getSourceQueueFor(MultiThreadedEventContext context) {
    Object schedulingKey = context.getSchedulingKey();
    if (null == schedulingKey) {
      return findShortestQueueIndex();
    } else {
      int index = hashCodeToArrayIndex(schedulingKey.hashCode(), this.sourceQueues.length);
      return index;
    }
  }

  private int hashCodeToArrayIndex(int hashcode, int arrayLength) {
    return Math.abs(hashcode % arrayLength);
  }

  // Used for testing
  @Override
  public int size() {
    int totalQueueSize = 0;
    for (MultiSourceQueueImpl<ContextWrapper<EC>> sourceQueue : this.sourceQueues) {
      totalQueueSize += sourceQueue.size();
    }
    return totalQueueSize;
  }

  @Override
  public String toString() {
    return "StageQueue(" + this.stageName + ")";
  }

  @Override
  public void clear() {
    int clearCount = 0;
    for (MultiSourceQueueImpl<ContextWrapper<EC>> sourceQueue : this.sourceQueues) {
      clearCount += sourceQueue.clear();
    }
    this.logger.info("Cleared " + clearCount);
  }

  /*********************************************************************************************************************
   * Monitorable Interface
   * @param enable
   */

  @Override
  public void enableStatsCollection(boolean enable) {
    StageQueueStatsCollector collector = null;
    for (MultiSourceQueueImpl<ContextWrapper<EC>> src : this.sourceQueues) {
      String name = this.stageName + "[" + src.getSourceName() + "]";
      if (collector == null || !collector.getName().equals(name)) {
        collector = (enable) ? new StageQueueStatsCollectorImpl(name) : new NullStageQueueStatsCollector(name);
      }
      src.setStatsCollector(collector);
    }
  }

  @Override
  public Stats getStats(long frequency) {
    // Since all source queues have the same collector, the first reference is passed.
    if (this.sourceQueues.length == 1 ) {
      return this.sourceQueues[0].getStatsCollector();
    } else {
      return new Stats() {

        @Override
        public String getDetails() {
          StringBuilder build = new StringBuilder();
          StageQueueStatsCollector stats = null;
          for (MultiSourceQueueImpl<ContextWrapper<EC>> impl : sourceQueues) {
            StageQueueStatsCollector current = impl.getStatsCollector();
            if (stats != current) {
              if (stats != null) build.append('\n');
              build.append(current.getDetails());
            }
            stats = current;
          }
          return build.toString();
        }

        @Override
        public void logDetails(TCLogger statsLogger) {
          statsLogger.info(getDetails());
        }
      };
    }
  }

  @Override
  public Stats getStatsAndReset(long frequency) {
    return getStats(frequency);
  }

  @Override
  public boolean isStatsCollectionEnabled() {
    // Since all source queues have the same collector, the first reference is used.
    return this.sourceQueues[0].getStatsCollector() instanceof StageQueueStatsCollectorImpl;
  }

  @Override
  public void resetStats() {
    // Since all source queues have the same collector, the first reference is used.
    this.sourceQueues[0].getStatsCollector().reset();
  }

  private final class MultiSourceQueueImpl<W> implements UpdatableFixedHeap.Ordered, SourceQueue<W> {

    private final BlockingQueue<W> queue;
    private final int                      sourceIndex;
    private volatile StageQueueStatsCollector statsCollector;
    private UpdatableFixedHeap.UpdatableWrapper<MultiSourceQueueImpl<ContextWrapper<EC>>> heapWrapper;

    public MultiSourceQueueImpl(BlockingQueue<W> queue, int sourceIndex, StageQueueStatsCollector statsCollector) {
      this.queue = queue;
      this.sourceIndex = sourceIndex;
      this.statsCollector = statsCollector;
    }

    @Override
    public String toString() {
      return "SourceQueueImpl{" + sourceIndex + "size=" + queue.size() + '}';
    }

    @Override
    public StageQueueStatsCollector getStatsCollector() {
      return this.statsCollector;
    }

    @Override
    public void setStatsCollector(StageQueueStatsCollector collector) {
      this.statsCollector = collector;
    }

    // XXX: poor man's clear.
    @Override
    public int clear() {
      int cleared = 0;
      try {
        while (poll(0) != null) {
          cleared++;
        }
        return cleared;
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    @Override
    public boolean isEmpty() {
      return this.queue.isEmpty();
    }

    @Override
    public W poll(long timeout) throws InterruptedException {
      W rv = this.queue.poll(timeout, TimeUnit.MILLISECONDS);
      if (rv != null) {
        this.statsCollector.contextRemoved();
        if (queue.isEmpty()) {
          // set the empty index for shortest queue in hopes of catching it on the first try
          fcheck = this.sourceIndex;
        }
      } else {
        fcheck = this.sourceIndex;
      }
      return rv;
    }

    @Override
    public void put(W context) throws InterruptedException {
      this.queue.put(context);
      this.statsCollector.contextAdded();
    }

    @Override
    public int size() {
      return this.queue.size();
    }

    @Override
    public String getSourceName() {
      return Integer.toString(this.sourceIndex);
    }

    public void setHeapWrapper(UpdatableFixedHeap.UpdatableWrapper<MultiSourceQueueImpl<ContextWrapper<EC>>> heapWrapper) {
      this.heapWrapper = heapWrapper;
    }

    @Override
    public int getHeapOrderingAmount() {
      return size();
    }
  }

  private class FlushingHandledContext<T extends EC> implements ContextWrapper<EC> {
    private final EC context;
    private final int offset;
    private int executionCount = 0;
    public FlushingHandledContext(EC context, int offset) {
      this.context = context;
      this.offset = offset;
    }

    @Override
    public void runWithHandler(EventHandler<EC> handler) throws EventHandlerException {
      if (++executionCount == sourceQueues.length) {
//  been through all the queues.  execute now.
        handler.handleEvent(this.context);
      } else {
//  move to next queue
        boolean interrupted = false;
        try {
          while (true) {
            try {
              sourceQueues[moduloQueueCount(executionCount + offset)].put(this);
              break;
            } catch (InterruptedException e) {
              logger.debug("FlushingHandledContext move to next queue: " + e + " : " + ((executionCount + offset) % sourceQueues.length));
              interrupted = true;
            }
          }
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (context.getClass().isInstance(obj)) {
        return context.equals(obj);
      }
      return super.equals(obj);
    }
  }
}
