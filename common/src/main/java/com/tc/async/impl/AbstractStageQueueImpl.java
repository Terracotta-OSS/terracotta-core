/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

import org.slf4j.Logger;

import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.Assert;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author cschanck
 **/
public abstract class AbstractStageQueueImpl<EC> implements StageQueue<EC> {

  private volatile boolean closed = false;  // open at create
  private volatile boolean extraStats = true;  
  private volatile int maxDepth = 0;
  private final int queueSize;
  private final MonitoringEventCreator<EC> monitoring;
  private final EventCreator<EC> creator;
  final Logger logger;
  final String stageName;
  
  public AbstractStageQueueImpl(TCLoggerProvider loggerProvider, String stageName, EventCreator<EC> creator, int queueSize) {
    this.logger = loggerProvider.getLogger(Sink.class.getName() + ": " + stageName);
    this.stageName = stageName;
    this.creator = creator;
    this.monitoring = new MonitoringEventCreator<>(stageName, creator);
    this.queueSize = queueSize;
  }
  
  abstract SourceQueue[] getSources();

  @Override
  public void enableAdditionalStatistics(boolean track) {
    extraStats = track;
  }
  
  final Event createEvent(EC context) {
    return (extraStats) ? this.monitoring.createEvent(context) : creator.createEvent(context);
  }
    
  Logger getLogger() {
    return logger;
  }
  
  boolean isClosed() {
    return closed;
  }

  @Override
  public void close() {
    Assert.assertFalse(this.closed);
    this.closed = true;
    for (SourceQueue q : this.getSources()) {
      try {
        q.put(new CloseEvent());
      } catch (InterruptedException ie) {
        logger.debug("closing stage", ie);
        Thread.currentThread().interrupt();
      }
    }
  }
  
  protected void updateDepth(int max) {
    if (max > maxDepth) {
      maxDepth = max;
    }
  }
  
  public Map<String, ?> getState() {
    Map<String, Object> queueState = new LinkedHashMap<>();
    if (extraStats) {
      Map<String, ?> stats = this.monitoring.getState();
      if (!stats.isEmpty()) {
        queueState.put("stats", stats);
        queueState.put("maxQueueDepth", maxDepth);
        queueState.put("queueSize", queueSize);
      }
    }
    return queueState;
  }
  
  interface SourceQueue extends Source {
    /*  returns queue depth at time of put  */
    int put(Event context) throws InterruptedException;
  }

  class HandledEvent<C> implements Event {
    private final Event event;

    public HandledEvent(Event event) {
      this.event = event;
    }

    @Override
    public void call() throws EventHandlerException {
      event.call();
    }
  }
  
  
  static class CloseEvent implements Event {

    public CloseEvent() {
    }

    @Override
    public void call() throws EventHandlerException {

    }

  }
}
