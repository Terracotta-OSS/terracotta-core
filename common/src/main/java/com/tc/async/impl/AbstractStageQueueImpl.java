package com.tc.async.impl;

import org.slf4j.Logger;

import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.Assert;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author cschanck
 **/
public abstract class AbstractStageQueueImpl<EC> implements StageQueue<EC> {

  private volatile boolean closed = false;  // open at create
  private volatile boolean extraStats = true;  
  private final AtomicInteger inflight = new AtomicInteger();
  private final MonitoringEventCreator<EC> monitoring;
  private final EventCreator<EC> creator;
  final Logger logger;
  final String stageName;
  
  public AbstractStageQueueImpl(TCLoggerProvider loggerProvider, String stageName, EventCreator<EC> creator) {
    this.logger = loggerProvider.getLogger(Sink.class.getName() + ": " + stageName);
    this.stageName = stageName;
    this.creator = creator;
    this.monitoring = new MonitoringEventCreator<>(stageName, creator);
  }
  
  abstract SourceQueue[] getSources();
  
  void addInflight() {
    inflight.incrementAndGet();
  }

  @Override
  public void enableAdditionalStatistics(boolean track) {
    extraStats = track;
  }
  
  final EventCreator<EC> getEventCreator() {
    return (extraStats) ? this.monitoring : creator;
  }
  
  @Override
  public void clear() {
    inflight.set(0);
  }
    
  Logger getLogger() {
    return logger;
  }
  
  boolean isClosed() {
    return closed;
  }

  @Override
  public int size() {
    return inflight.get();
  }
  
  @Override
  public boolean isEmpty() {
    int val = inflight.get();
    Assert.assertTrue(val >= 0);
    return val == 0;
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
  
  @Override
  public Map<String, ?> getState() {
    Map<String, Object> queueState = new LinkedHashMap<>();
    if (extraStats) {
      Map<String, ?> stats = this.monitoring.getState();
      if (!stats.isEmpty()) {
        queueState.put("stats", stats);
      }
    }
    queueState.put("backlog", this.size());
    return queueState;
  }
  
  interface SourceQueue extends Source {
    int clear();

    @Override
    boolean isEmpty();

    @Override
    Event poll(long timeout) throws InterruptedException;

    void put(Event context) throws InterruptedException;

    int size();

    @Override
    String getSourceName();
  }

  class HandledEvent<C> implements Event {
    private final Event event;

    public HandledEvent(Event event) {
      this.event = event;
    }

    @Override
    public void call() throws EventHandlerException {
      try {
        event.call();
      } finally {
        Assert.assertTrue(inflight.decrementAndGet() >= 0);
      }
    }
  }
  
  
  static class CloseEvent<C> implements Event {

    public CloseEvent() {
    }

    @Override
    public void call() throws EventHandlerException {

    }

  }
}
