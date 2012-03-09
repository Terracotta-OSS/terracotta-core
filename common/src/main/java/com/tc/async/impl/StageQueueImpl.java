/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.async.api.StageQueueStats;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;
import com.tc.util.Assert;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.TCQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This StageQueueImpl represents the sink and gives a handle to the source. We are internally justun using a queue
 * since our queues are locally processed. This class can be replaced with a distributed queue to enable processing
 * across process boundaries.
 */
public class StageQueueImpl implements Sink {

  private final String            stageName;
  private final TCLogger          logger;
  private volatile AddPredicate   predicate = DefaultAddPredicate.getInstance();
  private final SourceQueueImpl[] sourceQueues;

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
  public StageQueueImpl(int threadCount, int threadsToQueueRatio, QueueFactory queueFactory,
                        TCLoggerProvider loggerProvider, String stageName, int queueSize) {
    Assert.eval(threadCount > 0);
    this.logger = loggerProvider.getLogger(Sink.class.getName() + ": " + stageName);
    this.stageName = stageName;
    this.sourceQueues = new SourceQueueImpl[threadCount];
    createWorkerQueues(threadCount, threadsToQueueRatio, queueFactory, queueSize, loggerProvider, stageName);
  }

  private void createWorkerQueues(int threads, int threadsToQueueRatio, QueueFactory queueFactory, int queueSize,
                                  TCLoggerProvider loggerProvider, String stage) {
    StageQueueStatsCollector statsCollector = new NullStageQueueStatsCollector(stage);
    TCQueue q = null;
    int queueCount = -1;

    if (queueSize != Integer.MAX_VALUE) {
      int totalQueueToBeConstructed = (int) Math.ceil(((double) threads) / threadsToQueueRatio);
      queueSize = (int) Math.ceil(((double) queueSize) / totalQueueToBeConstructed);
    }
    Assert.eval(queueSize > 0);

    for (int i = 0; i < threads; i++) {
      if (threadsToQueueRatio > 0) {
        if (i % threadsToQueueRatio == 0) {
          // creating new worker queue
          q = queueFactory.createInstance(queueSize);
          queueCount++;
        } else {
          // use same queue for this worker too
        }
      } else if (q == null) {
        // all workers share the same queue, create queue only once
        q = queueFactory.createInstance(queueSize);
        queueCount++;
      }
      this.sourceQueues[i] = new SourceQueueImpl(q, String.valueOf(queueCount), statsCollector);
    }
  }

  public Source getSource(int index) {
    return this.sourceQueues[index];
  }

  /**
   * The context will be added if the sink was found to be empty(at somepoint during the call). If the queue was not
   * empty (at somepoint during the call) the context might not be added. This method should only be used where the
   * stage threads are to be signaled on data availablity and the threads take care of getting data from elsewhere
   */
  public boolean addLossy(EventContext context) {
    SourceQueueImpl sourceQueue;
    if (context instanceof MultiThreadedEventContext) {
      sourceQueue = getSourceQueueFor((MultiThreadedEventContext) context);
    } else {
      sourceQueue = this.sourceQueues[0];
    }

    if (sourceQueue.isEmpty()) {
      add(context);
      return true;
    } else {
      return false;
    }
  }

  public void addMany(Collection contexts) {
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Added many:" + contexts + " to:" + this.stageName);
    }
    for (Iterator i = contexts.iterator(); i.hasNext();) {
      add((EventContext) i.next());
    }
  }

  public void add(EventContext context) {
    Assert.assertNotNull(context);
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Added:" + context + " to:" + this.stageName);
    }
    if (!this.predicate.accept(context)) {
      if (this.logger.isDebugEnabled()) {
        this.logger.debug("Predicate caused skip add for:" + context + " to:" + this.stageName);
      }
      return;
    }

    boolean interrupted = false;
    try {
      while (true) {
        try {
          if (context instanceof MultiThreadedEventContext) {
            SourceQueueImpl sourceQueue = getSourceQueueFor((MultiThreadedEventContext) context);
            sourceQueue.put(context);
          } else {
            this.sourceQueues[0].put(context);
          }
          break;
        } catch (InterruptedException e) {
          this.logger.error("StageQueue Add: " + e);
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private SourceQueueImpl getSourceQueueFor(MultiThreadedEventContext context) {
    Object o = context.getKey();
    int index = hashCodeToArrayIndex(o.hashCode(), this.sourceQueues.length);
    return this.sourceQueues[index];
  }

  private int hashCodeToArrayIndex(int hashcode, int arrayLength) {
    return Math.abs(hashcode % arrayLength);
  }

  // Used for testing
  public int size() {
    int totalQueueSize = 0;
    for (SourceQueueImpl sourceQueue : this.sourceQueues) {
      totalQueueSize += sourceQueue.size();
    }
    return totalQueueSize;
  }

  public void setAddPredicate(AddPredicate predicate) {
    Assert.eval(predicate != null);
    this.predicate = predicate;
  }

  public AddPredicate getPredicate() {
    return this.predicate;
  }

  @Override
  public String toString() {
    return "StageQueue(" + this.stageName + ")";
  }

  public void clear() {
    int clearCount = 0;
    for (SourceQueueImpl sourceQueue : this.sourceQueues) {
      clearCount += sourceQueue.clear();
    }
    this.logger.info("Cleared " + clearCount);
  }

  /*********************************************************************************************************************
   * Monitorable Interface
   */

  public void enableStatsCollection(boolean enable) {
    StageQueueStatsCollector statsCollector;
    if (enable) {
      statsCollector = new StageQueueStatsCollectorImpl(this.stageName);
    } else {
      statsCollector = new NullStageQueueStatsCollector(this.stageName);
    }
    for (SourceQueueImpl sourceQueue : this.sourceQueues) {
      sourceQueue.setStatesCollector(statsCollector);
    }
  }

  public Stats getStats(long frequency) {
    // Since all source queues have the same collector, the first reference is passed.
    return this.sourceQueues[0].getStatsCollector();
  }

  public Stats getStatsAndReset(long frequency) {
    return getStats(frequency);
  }

  public boolean isStatsCollectionEnabled() {
    // Since all source queues have the same collector, the first reference is used.
    return this.sourceQueues[0].getStatsCollector() instanceof StageQueueStatsCollectorImpl;
  }

  public void resetStats() {
    // Since all source queues have the same collector, the first reference is used.
    this.sourceQueues[0].getStatsCollector().reset();
  }

  private static final class SourceQueueImpl implements Source {

    private final TCQueue                     queue;
    private final String                      sourceName;
    private volatile StageQueueStatsCollector statsCollector;

    public SourceQueueImpl(TCQueue queue, String sourceName, StageQueueStatsCollector statsCollector) {
      this.queue = queue;
      this.sourceName = sourceName;
      this.statsCollector = statsCollector;
    }

    public StageQueueStatsCollector getStatsCollector() {
      return this.statsCollector;
    }

    public void setStatesCollector(StageQueueStatsCollector collector) {
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

    public boolean isEmpty() {
      return this.queue.isEmpty();
    }

    public EventContext poll(long timeout) throws InterruptedException {
      EventContext rv = (EventContext) this.queue.poll(timeout);
      if (rv != null) {
        this.statsCollector.contextRemoved();
      }
      return rv;
    }

    public void put(Object obj) throws InterruptedException {
      this.queue.put(obj);
      this.statsCollector.contextAdded();
    }

    public int size() {
      return this.queue.size();
    }

    public String getSourceName() {
      return this.sourceName;
    }
  }

  private static abstract class StageQueueStatsCollector implements StageQueueStats {

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

    public String getName() {
      return this.trimmedName;
    }

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

    public String getName() {
      return this.trimmedName;
    }

    public int getDepth() {
      return this.count.get();
    }
  }
}
