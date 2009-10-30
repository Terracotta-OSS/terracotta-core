/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventMultiThreadedContext;
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
    TCQueue q = queueFactory.createInstance(queueSize);
    int queueCount = 0;
    for (int i = 0; i < threads; i++) {
      if (threadsToQueueRatio > 0) {
        if (i % threadsToQueueRatio == 0) {
          // creating new worker queue
          q = queueFactory.createInstance(queueSize);
          queueCount++;
        } else {
          // use same queue for this worker too
        }
      } else {
        // all workers share the same queue
      }
      sourceQueues[i] = new SourceQueueImpl(q, String.valueOf(queueCount), statsCollector);
    }
  }

  public Source getSource(int index) {
    return sourceQueues[index];
  }

  /**
   * The context will be added if the sink was found to be empty(at somepoint during the call). If the queue was not
   * empty (at somepoint during the call) the context might not be added. This method should only be used where the
   * stage threads are to be signaled on data availablity and the threads take care of getting data from elsewhere
   */
  public boolean addLossy(EventContext context) {
    SourceQueueImpl sourceQueue;
    if (context instanceof EventMultiThreadedContext) {
      sourceQueue = getSourceQueueFor((EventMultiThreadedContext) context);
    } else {
      sourceQueue = sourceQueues[0];
    }
    
    if (sourceQueue.isEmpty()) {
      add(context);
      return true;
    } else {
      return false;
    }
  }

  public void addMany(Collection contexts) {
    if (logger.isDebugEnabled()) logger.debug("Added many:" + contexts + " to:" + stageName);
    for (Iterator i = contexts.iterator(); i.hasNext();) {
      add((EventContext) i.next());
    }
  }

  public void add(EventContext context) {
    Assert.assertNotNull(context);
    if (logger.isDebugEnabled()) logger.debug("Added:" + context + " to:" + stageName);
    if (!predicate.accept(context)) {
      if (logger.isDebugEnabled()) logger.debug("Predicate caused skip add for:" + context + " to:" + stageName);
      return;
    }

    try {
      if (context instanceof EventMultiThreadedContext) {
        SourceQueueImpl sourceQueue = getSourceQueueFor((EventMultiThreadedContext) context);
        sourceQueue.put(context);
      } else {
        sourceQueues[0].put(context);
      }
    } catch (InterruptedException e) {
      logger.error(e);
      throw new AssertionError(e);
    }

  }

  private SourceQueueImpl getSourceQueueFor(EventMultiThreadedContext context) {
    Object o = context.getKey();
    int index = hashCodeToArrayIndex(o.hashCode(), sourceQueues.length);
    return sourceQueues[index];
  }

  private int hashCodeToArrayIndex(int hashcode, int arrayLength) {
    return (hashcode % arrayLength);
  }

  // Used for testing
  public int size() {
    int totalQueueSize = 0;
    for (int i = 0; i < sourceQueues.length; i++) {
      totalQueueSize += sourceQueues[i].size();
    }
    return totalQueueSize;
  }

  public void setAddPredicate(AddPredicate predicate) {
    Assert.eval(predicate != null);
    this.predicate = predicate;
  }

  public AddPredicate getPredicate() {
    return predicate;
  }

  public String toString() {
    return "StageQueue(" + stageName + ")";
  }

  public void clear() {
    int clearCount = 0;
    for (int i = 0; i < this.sourceQueues.length; i++) {
      clearCount += sourceQueues[i].clear();
    }
    logger.info("Cleared " + clearCount);
  }

  /*********************************************************************************************************************
   * Monitorable Interface
   */

  public void enableStatsCollection(boolean enable) {
    StageQueueStatsCollector statsCollector;
    if (enable) {
      statsCollector = new StageQueueStatsCollectorImpl(stageName);
    } else {
      statsCollector = new NullStageQueueStatsCollector(stageName);
    }
    for (int i = 0; i < sourceQueues.length; i++) {
      sourceQueues[i].setStatesCollector(statsCollector);
    }
  }

  public Stats getStats(long frequency) {
    // Since all source queues have the same collector, the first reference is passed.
    return sourceQueues[0].getStatsCollector();
  }

  public Stats getStatsAndReset(long frequency) {
    return getStats(frequency);
  }

  public boolean isStatsCollectionEnabled() {
    // Since all source queues have the same collector, the first reference is used.
    return sourceQueues[0].getStatsCollector() instanceof StageQueueStatsCollectorImpl;
  }

  public void resetStats() {
    // Since all source queues have the same collector, the first reference is used.
    sourceQueues[0].getStatsCollector().reset();
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
      return statsCollector;
    }

    public void setStatesCollector(StageQueueStatsCollector collector) {
      statsCollector = collector;
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
      return queue.isEmpty();
    }

    public EventContext poll(long timeout) throws InterruptedException {
      EventContext rv = (EventContext) queue.poll(timeout);
      if (rv != null) statsCollector.contextRemoved();
      return rv;
    }

    public void put(Object obj) throws InterruptedException {
      queue.put(obj);
      statsCollector.contextAdded();
    }

    public int size() {
      return queue.size();
    }

    public String getSourceName() {
      return sourceName;
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
      return name + " : Not Monitored";
    }

    public void contextAdded() {
      // NO-OP
    }

    public void contextRemoved() {
      // NO-OP
    }

    public void reset() {
      // NO-OP
    }

    public String getName() {
      return trimmedName;
    }

    public int getDepth() {
      return -1;
    }
  }

  private static class StageQueueStatsCollectorImpl extends StageQueueStatsCollector {

    private final AtomicInteger count = new AtomicInteger(0);
    private final String  name;
    private final String  trimmedName;

    public StageQueueStatsCollectorImpl(String stage) {
      this.trimmedName = stage.trim();
      this.name = makeWidth(stage, 40);
    }

    public String getDetails() {
      return name + " : " + count;
    }

    public void contextAdded() {
      count.incrementAndGet();
    }

    public void contextRemoved() {
      count.decrementAndGet();
    }

    public void reset() {
      count.set(0);
    }

    public String getName() {
      return trimmedName;
    }

    public int getDepth() {
      return count.get();
    }
  }
}
