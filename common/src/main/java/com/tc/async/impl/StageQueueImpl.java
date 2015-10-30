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
import com.tc.async.api.StageQueueStats;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;
import com.tc.util.Assert;
import com.tc.util.concurrent.QueueFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This StageQueueImpl represents the sink and gives a handle to the source. We are internally justun using a queue
 * since our queues are locally processed. This class can be replaced with a distributed queue to enable processing
 * across process boundaries.
 */
public class StageQueueImpl<EC> implements Sink<EC> {

  private final String            stageName;
  private final TCLogger          logger;
  private final SourceQueueImpl<ContextWrapper<EC>>[] sourceQueues;
  private volatile boolean closed = false;
  /**
   * The Constructor.
   * 
   * @param threadCount : Number of threads working on this stage
   * @param threadsToQueueRatio : The ratio determines the number of queues internally used and the number of threads
   *        per each queue. Ideally you would want this to be same as threadCount, in which case there is only 1 queue
   *        used internally and all thread are working on the same queue (which doesn't guarantee order in processing)
   *        or set it to 1 where each thread gets its own queue but the (multithreaded) event contexts are distributed
   *        based on the key they return.
   * @param queueFactory : Factory used to create the queues
   * @param loggerProvider : logger
   * @param stageName : The stage name
   * @param queueSize : Max queue Size allowed
   */
  @SuppressWarnings("unchecked")
  public StageQueueImpl(int queueCount, QueueFactory<ContextWrapper<EC>> queueFactory,
                        TCLoggerProvider loggerProvider, String stageName, int queueSize) {
    Assert.eval(queueCount > 0);
    this.logger = loggerProvider.getLogger(Sink.class.getName() + ": " + stageName);
    this.stageName = stageName;
    this.sourceQueues = new SourceQueueImpl[queueCount];
    createWorkerQueues(queueCount, queueFactory, queueSize, stageName);
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
      this.sourceQueues[i] = new SourceQueueImpl<ContextWrapper<EC>>(q, String.valueOf(queueCount), statsCollector);
    }
  }

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
    MultiThreadedEventContext cxt = (MultiThreadedEventContext)context;
    int index = getSourceQueueFor(cxt);
    ContextWrapper<EC> wrapper = (cxt.flush()) ? new FlushingHandledContext(context, index) : new HandledContext<EC>(context);
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
  
  private volatile int fcheck = 0;
//  TODO:  Way too busy. need a better way
  private int findShortestQueueIndex() {
      int stop = (fcheck++) % this.sourceQueues.length;
      int pointer = stop+1;
      int min = Integer.MAX_VALUE;
      int can = -1;
// special case where context can go to any queue, pick the shortest
      while (pointer != stop) {
        if (++pointer >= this.sourceQueues.length) {
          pointer = 0;
        }
        SourceQueueImpl<ContextWrapper<EC>> impl = this.sourceQueues[pointer];
        if (impl.isEmpty()) {
          return pointer;
        } else {
          if (Math.min(min, impl.size()) != min) {
            can = pointer;
            min = impl.size();
          }
        }
      }
      return can;
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
    for (SourceQueueImpl<ContextWrapper<EC>> sourceQueue : this.sourceQueues) {
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
    for (SourceQueueImpl<ContextWrapper<EC>> sourceQueue : this.sourceQueues) {
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
    for (SourceQueueImpl<ContextWrapper<EC>> src : this.sourceQueues) {
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
          for (SourceQueueImpl<ContextWrapper<EC>> impl : sourceQueues) {
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

  private static final class SourceQueueImpl<W> implements Source<W> {

    private final BlockingQueue<W> queue;
    private final String                      sourceName;
    private volatile StageQueueStatsCollector statsCollector;

    public SourceQueueImpl(BlockingQueue<W> queue, String sourceName, StageQueueStatsCollector statsCollector) {
      this.queue = queue;
      this.sourceName = sourceName;
      this.statsCollector = statsCollector;
    }

    public StageQueueStatsCollector getStatsCollector() {
      return this.statsCollector;
    }

    public void setStatsCollector(StageQueueStatsCollector collector) {
      this.statsCollector = collector;
    }

    // XXX: poor man's clear.
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
      }
      return rv;
    }

    public void put(W context) throws InterruptedException {
      this.queue.put(context);
      this.statsCollector.contextAdded();
    }

    public int size() {
      return this.queue.size();
    }

    @Override
    public String getSourceName() {
      return this.sourceName;
    }
  }

  private static abstract class StageQueueStatsCollector implements StageQueueStats {

    @Override
    public void logDetails(TCLogger statsLogger) {
      statsLogger.info(getDetails());
    }

    public abstract void contextAdded();

    public abstract void reset();

    public abstract void contextRemoved();

    protected String makeWidth(String name, int width) {
      final int len = name.length();
      if (len == width) { return name; }
      if (len > width) { return name.substring(0, width); }

      StringBuffer buf = new StringBuffer(name);
      for (int i = len; i < width; i++) {
        buf.append(' ');
      }
      return buf.toString();
    }
  }

  private static class NullStageQueueStatsCollector extends StageQueueStatsCollector {

    private final String name;
    private final String trimmedName;

    public NullStageQueueStatsCollector(String stage) {
      this.trimmedName = stage.trim();
      this.name = makeWidth(stage, 40);
    }

    @Override
    public String getDetails() {
      return this.name + " : Not Monitored";
    }

    @Override
    public void contextAdded() {
      // NO-OP
    }

    @Override
    public void contextRemoved() {
      // NO-OP
    }

    @Override
    public void reset() {
      // NO-OP
    }

    @Override
    public String getName() {
      return this.trimmedName;
    }

    @Override
    public int getDepth() {
      return -1;
    }
  }

  private static class StageQueueStatsCollectorImpl extends StageQueueStatsCollector {

    private final AtomicInteger count = new AtomicInteger(0);
    private final String        name;
    private final String        trimmedName;

    public StageQueueStatsCollectorImpl(String stage) {
      this.trimmedName = stage.trim();
      this.name = makeWidth(stage, 40);
    }

    @Override
    public String getDetails() {
      return this.name + " : " + this.count;
    }

    @Override
    public void contextAdded() {
      this.count.incrementAndGet();
    }

    @Override
    public void contextRemoved() {
      this.count.decrementAndGet();
    }

    @Override
    public void reset() {
      this.count.set(0);
    }

    @Override
    public String getName() {
      return this.trimmedName;
    }

    @Override
    public int getDepth() {
      return this.count.get();
    }
  }
  
  private static class DirectExecuteContext<EC> implements ContextWrapper<EC> {
    private final SpecializedEventContext context;
    public DirectExecuteContext(SpecializedEventContext context) {
      this.context = context;
    }
    @Override
    public void runWithHandler(EventHandler<EC> handler) throws EventHandlerException {
      this.context.execute();
    }
  }
  
  private static class HandledContext<EC> implements ContextWrapper<EC> {
    private final EC context;
    public HandledContext(EC context) {
      this.context = context;
    }
    @Override
    public void runWithHandler(EventHandler<EC> handler) throws EventHandlerException {
      handler.handleEvent(this.context);
    }

    @Override
    public boolean equals(Object obj) {
      if (context.getClass().isInstance(obj)) {
        return context.equals(obj);
      }
      return super.equals(obj);
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
              sourceQueues[(executionCount + offset) % sourceQueues.length].put(this);
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
