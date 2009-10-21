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
 * The beginnings of an implementation of our SEDA like framework. This Is part of an impl for the queue
 * 
 * @author steve
 */
public class StageQueueImpl implements Sink, Source {

  private final String                      stageName;
  private final TCLogger                    logger;
  private AddPredicate                      predicate = DefaultAddPredicate.getInstance();
  private volatile StageQueueStatsCollector statsCollector;
  private final Source[]                    workerSources;
  private final TCQueue[]                   workerQueues;
  private String                            sourceName;

  public StageQueueImpl(int threads, int threadsToQueueRatio, QueueFactory queueFactory,
                        TCLoggerProvider loggerProvider, String stageName, int queueSize) {
    this(threads, queueFactory.createInstance(queueSize), loggerProvider, stageName);
    createWorkerQueues(threads, threadsToQueueRatio, queueFactory, this.workerQueues[0], queueSize, loggerProvider,
                       stageName);
  }

  // for worker queues
  private StageQueueImpl(TCLoggerProvider loggerProvider, String stageName, TCQueue queue, String sourceName) {
    this(1, queue, loggerProvider, stageName);
    this.sourceName = sourceName;
  }

  private StageQueueImpl(int threads, TCQueue queue, TCLoggerProvider loggerProvider, String stageName) {
    Assert.eval(threads > 0);
    this.logger = loggerProvider.getLogger(Sink.class.getName() + ": " + stageName);
    this.stageName = stageName;
    this.statsCollector = new NullStageQueueStatsCollector(stageName);
    this.workerSources = new Source[threads];
    this.workerQueues = new TCQueue[threads];
    this.workerQueues[0] = queue;
    this.workerSources[0] = this;
  }

  private void createWorkerQueues(int threads, int threadsToQueueRatio, QueueFactory queueFactory, TCQueue queue,
                                  int queueSize, TCLoggerProvider loggerProvider, String stage) {
    TCQueue q = queue;
    int queueCount = 0;
    for (int i = 1; i < threads; i++) {
      if (threadsToQueueRatio > 0) {
        if (i % threadsToQueueRatio == 0) {
          // creating new worker queue
          q = queueFactory.createInstance(queueSize);
          queueCount++;
        } else {
          // use same queue for this worker too
        }
      } else {
        // all workers share the same queue which is this.queue
      }
      Source source = new StageQueueImpl(loggerProvider, stage + "_worker_" + queueCount, q, "" + queueCount);
      workerSources[i] = source;
      workerQueues[i] = q;
    }
  }

  public Source getSource(int index) {
    return workerSources[index];
  }

  public String getSourceName() {
    return this.sourceName;
  }

  /**
   * The context will be added if the sink was found to be empty(at somepoint during the call). If the queue was not
   * empty (at somepoint during the call) the context might not be added. This method should only be used where the
   * stage threads are to be signaled on data availablity and the threads take care of getting data from elsewhere
   */
  public boolean addLossy(EventContext context) {
    if (isEmpty()) {
      add(context);
      return true;
    } else {
      return false;
    }
  }

  // XXX::Ugly hack since this method doesnt exist on the Channel interface
  private boolean isEmpty() {
    boolean empty = true;
    for (int i = 0; i < workerQueues.length; i++) {
      if (!workerQueues[i].isEmpty()) {
        empty = false;
        break;
      }
    }
    return empty;
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

    statsCollector.contextAdded();
    try {
      if (context instanceof EventMultiThreadedContext) {
        Object o = ((EventMultiThreadedContext) context).getKey();
        TCQueue workerSink = getWorkerSink(o);
        workerSink.put(context);
      } else {
        delegateToAnyWorker(context);
      }
    } catch (InterruptedException e) {
      logger.error(e);
      throw new AssertionError(e);
    }

  }

  private void delegateToAnyWorker(EventContext ctxt) throws InterruptedException {
    workerQueues[0].put(ctxt);
  }

  private TCQueue getWorkerSink(Object o) {
    int index = hashCodeToArrayIndex(o.hashCode(), workerQueues.length);
    return workerQueues[index];
  }

  private int hashCodeToArrayIndex(int hashcode, int arrayLength) {
    return (hashcode % arrayLength);
  }

  public EventContext poll(long period) throws InterruptedException {
    EventContext rv = (EventContext) workerQueues[0].poll(period);
    if (rv != null) statsCollector.contextRemoved();
    return rv;
  }

  private EventContext poll(int workerQueueIndex, long period) throws InterruptedException {
    EventContext rv = (EventContext) workerQueues[workerQueueIndex].poll(period);
    if (rv != null) statsCollector.contextRemoved();
    return rv;
  }

  // Used for testing
  public int size() {
    int totalQueueSize = 0;
    for (int i = 0; i < workerQueues.length; i++) {
      totalQueueSize += workerQueues[i].size();
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
    try {
      // XXX: poor man's clear.
      int clearCount = 0;
      for (int i = 0; i < this.workerQueues.length; i++) {
        while (poll(i, 0) != null) { // calling this.poll() to get counter updated
          /* supress no-body warning */
          clearCount++;
        }
      }
      statsCollector.reset();
      logger.info("Cleared " + clearCount);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  /*********************************************************************************************************************
   * Monitorable Interface
   */

  public void enableStatsCollection(boolean enable) {
    if (enable) {
      statsCollector = new StageQueueStatsCollectorImpl(stageName);
    } else {
      statsCollector = new NullStageQueueStatsCollector(stageName);
    }
  }

  public Stats getStats(long frequency) {
    return statsCollector;
  }

  public Stats getStatsAndReset(long frequency) {
    return getStats(frequency);
  }

  public boolean isStatsCollectionEnabled() {
    return statsCollector instanceof StageQueueStatsCollectorImpl;
  }

  public void resetStats() {
    // NOP
  }

  public static abstract class StageQueueStatsCollector implements StageQueueStats {

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

  public static class NullStageQueueStatsCollector extends StageQueueStatsCollector {

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
      // NOOP
    }

    public void contextRemoved() {
      // NOOP
    }

    public void reset() {
      // NOOP
    }

    public String getName() {
      return trimmedName;
    }

    public int getDepth() {
      return -1;
    }
  }

  public static class StageQueueStatsCollectorImpl extends StageQueueStatsCollector {

    private AtomicInteger count = new AtomicInteger(0);
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
