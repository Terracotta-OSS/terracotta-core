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


import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Source;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.QueueFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.tc.async.impl.AbstractStageQueueImpl.SourceQueue;

/**
 * This StageQueueImpl represents the sink and gives a handle to the source. We are internally just using a queue
 * since our queues are locally processed. This class can be replaced with a distributed queue to enable processing
 * across process boundaries.
 */
public class SingletonStageQueueImpl<EC> extends AbstractStageQueueImpl<EC> {

  private final SourceQueueImpl sourceQueue;

  /**
   * The Constructor.
   *
   * @param queueFactory : Factory used to create the queues
   * @param loggerProvider : logger
   * @param stageName : The stage name
   * @param queueSize : Max queue Size allowed
   */
  @SuppressWarnings("unchecked")
  SingletonStageQueueImpl(QueueFactory queueFactory, Class<EC> type, EventCreator<EC> creator,
                          TCLoggerProvider loggerProvider,
                          String stageName,
                          int queueSize) {
    super(loggerProvider, stageName, creator);
    this.sourceQueue = createWorkerQueue(queueFactory, type, queueSize, stageName);
  }

  private SourceQueueImpl createWorkerQueue(QueueFactory queueFactory, Class<EC> type, 
                                                                int queueSize,
                                                                String stage) {
    BlockingQueue<Event> q = null;

    Assert.eval(queueSize > 0);

    return new SourceQueueImpl(queueFactory.createInstance(type, queueSize));
  }

  @Override
  public Source getSource(int index) {
    return (index != 0) ? null : this.sourceQueue;
  }

  @Override
  SourceQueue[] getSources() {
    return new SourceQueue[] {this.sourceQueue};
  }

  @Override
  public void addToSink(EC context) {
    Assert.assertNotNull(context);
    if (isClosed()) {
      throw new IllegalStateException("closed");
    }
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Added:" + context + " to:" + this.stageName);
    }
    Event wrapper = createEvent(context);
    if (wrapper != null) {
      deliverToQueue(wrapper);
    }
  }

  private void deliverToQueue(Event wrapper) {
    boolean interrupted = Thread.interrupted();
    try {
      for (; ; ) {
        try {
          this.sourceQueue.put(wrapper);
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
  public String toString() {
    return "StageQueue(" + this.stageName + ")";
  }

  @Override
  public int clear() {
    int clearCount = sourceQueue.clear();
    this.logger.info("Cleared " + clearCount);
    return clearCount;
  }

  private final class SourceQueueImpl implements SourceQueue {

    private final BlockingQueue<Event> queue;

    public SourceQueueImpl(BlockingQueue<Event> queue) {
      this.queue = queue;
    }

    @Override
    public String toString() {
      return "SourceQueueImpl{Singleton size=" + queue.size() + '}';
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
    public Event poll(long timeout) throws InterruptedException {
      Event rv = (timeout == 0) ? this.queue.poll() : this.queue.poll(timeout, TimeUnit.MILLISECONDS);
      return rv;
    }

    @Override
    public void put(Event context) throws InterruptedException {
      this.queue.put(context);
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
}
