/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.async.impl;


import com.tc.async.api.Source;
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
    super(loggerProvider, stageName, creator, queueSize);
    this.sourceQueue = createWorkerQueue(queueFactory, type, queueSize);
  }

  private SourceQueueImpl createWorkerQueue(QueueFactory queueFactory, Class<EC> type, 
                                                                int queueSize) {
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
      return;
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
          updateDepth(this.sourceQueue.put(wrapper));
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

  private final class SourceQueueImpl implements SourceQueue {

    private final BlockingQueue<Event> queue;

    public SourceQueueImpl(BlockingQueue<Event> queue) {
      this.queue = queue;
    }

    @Override
    public String toString() {
      return "SourceQueueImpl{Singleton size=" + queue.size() + '}';
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
    public int put(Event context) throws InterruptedException {
      this.queue.put(context);
      return this.queue.size();
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
