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
import com.tc.util.concurrent.QueueFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.tc.async.impl.AbstractStageQueueImpl.DirectExecuteContext;
import static com.tc.async.impl.AbstractStageQueueImpl.SourceQueue;
import static com.tc.async.impl.AbstractStageQueueImpl.StageQueueStatsCollector;
import static com.tc.async.impl.AbstractStageQueueImpl.StageQueueStatsCollectorImpl;

/**
 * This StageQueueImpl represents the sink and gives a handle to the source. We are internally just using a queue
 * since our queues are locally processed. This class can be replaced with a distributed queue to enable processing
 * across process boundaries.
 */
public class SingletonStageQueueImpl<EC> implements StageQueue<EC> {

  private final String stageName;
  private final TCLogger logger;
  private final SourceQueueImpl<ContextWrapper<EC>> sourceQueue;
  private volatile boolean closed = false;

  /**
   * The Constructor.
   *
   * @param queueFactory : Factory used to create the queues
   * @param loggerProvider : logger
   * @param stageName : The stage name
   * @param queueSize : Max queue Size allowed
   */
  @SuppressWarnings("unchecked")
  SingletonStageQueueImpl(QueueFactory<ContextWrapper<EC>> queueFactory,
                          TCLoggerProvider loggerProvider,
                          String stageName,
                          int queueSize) {

    this.logger = loggerProvider.getLogger(Sink.class.getName() + ": " + stageName);
    this.stageName = stageName;
    this.sourceQueue = createWorkerQueue(queueFactory, queueSize, stageName);
  }

  private SourceQueueImpl<ContextWrapper<EC>> createWorkerQueue(QueueFactory<ContextWrapper<EC>> queueFactory,
                                                                int queueSize,
                                                                String stage) {
    StageQueueStatsCollector statsCollector = new NullStageQueueStatsCollector(stage);
    BlockingQueue<ContextWrapper<EC>> q = null;

    Assert.eval(queueSize > 0);

    q = queueFactory.createInstance(queueSize);
    return new SourceQueueImpl<ContextWrapper<EC>>(q, statsCollector);
  }

  @Override
  public Source<ContextWrapper<EC>> getSource(int index) {
    return (index != 0) ? null : this.sourceQueue;
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
    deliverToQueue("Single", wrapper);
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
    MultiThreadedEventContext cxt = (MultiThreadedEventContext) context;
    ContextWrapper<EC> wrapper = (cxt.flush()) ? new FlushingHandledContext(context) : new HandledContext<EC>(context);
    deliverToQueue("Multi", wrapper);
  }

  @Override
  public void addSpecialized(SpecializedEventContext specialized) {
    if (closed) {
      throw new IllegalStateException("closed");
    }
    ContextWrapper<EC> wrapper = new DirectExecuteContext<EC>(specialized);
    deliverToQueue("Specialized", wrapper);
  }

  private void deliverToQueue(String type, ContextWrapper<EC> wrapper) {
    boolean interrupted = Thread.interrupted();
    try {
      for (; ; ) {
        try {
          this.sourceQueue.put(wrapper);
          break;
        } catch (InterruptedException e) {
          this.logger.debug("StageQueue Add: [" + type + "] " + e);
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  // Used for testing
  @Override
  public int size() {
    return sourceQueue.size();
  }

  @Override
  public String toString() {
    return "StageQueue(" + this.stageName + ")";
  }

  @Override
  public void clear() {
    int clearCount = sourceQueue.clear();
    this.logger.info("Cleared " + clearCount);
  }

  /*********************************************************************************************************************
   * Monitorable Interface
   * @param enable
   */

  @Override
  public void enableStatsCollection(boolean enable) {
    StageQueueStatsCollector collector = null;

    String name = this.stageName + "[" + sourceQueue.getSourceName() + "]";
    if (collector == null || !collector.getName().equals(name)) {
      collector = (enable) ? new StageQueueStatsCollectorImpl(name) : new NullStageQueueStatsCollector(name);
    }
    sourceQueue.setStatsCollector(collector);
  }

  @Override
  public Stats getStats(long frequency) {
    return this.sourceQueue.getStatsCollector();
  }

  @Override
  public Stats getStatsAndReset(long frequency) {
    return getStats(frequency);
  }

  @Override
  public boolean isStatsCollectionEnabled() {
    // Since all source queues have the same collector, the first reference is used.
    return this.sourceQueue.getStatsCollector() instanceof StageQueueStatsCollectorImpl;
  }

  @Override
  public void resetStats() {
    // Since all source queues have the same collector, the first reference is used.
    this.sourceQueue.getStatsCollector().reset();
  }

  private final class SourceQueueImpl<W> implements SourceQueue<W> {

    private final BlockingQueue<W> queue;
    private volatile StageQueueStatsCollector statsCollector;

    public SourceQueueImpl(BlockingQueue<W> queue, StageQueueStatsCollector statsCollector) {
      this.queue = queue;
      this.statsCollector = statsCollector;
    }

    @Override
    public String toString() {
      return "SourceQueueImpl{Singleton size=" + queue.size() + '}';
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
      return "Singleton";
    }

  }

  private class FlushingHandledContext<T extends EC> implements ContextWrapper<EC> {
    private final EC context;
    private int executionCount = 0;

    public FlushingHandledContext(EC context) {
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
}
