package com.tc.async.impl;

import org.slf4j.Logger;

import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.Assert;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author cschanck
 **/
public abstract class AbstractStageQueueImpl<EC> implements StageQueue<EC> {

  private volatile boolean closed = false;  // open at create
  private final AtomicInteger inflight = new AtomicInteger();
  final Logger logger;
  final String stageName;
  
  public AbstractStageQueueImpl(TCLoggerProvider loggerProvider, String stageName) {
    this.logger = loggerProvider.getLogger(Sink.class.getName() + ": " + stageName);
    this.stageName = stageName;
  }
  
  abstract SourceQueue[] getSources();
  
  void addInflight() {
    inflight.incrementAndGet();
  }
  
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
        q.put(new CloseContext());
      } catch (InterruptedException ie) {
        logger.debug("closing stage", ie);
        Thread.currentThread().interrupt();
      }
    }
  }
  
  interface SourceQueue<W> extends Source<W> {
    int clear();

    @Override
    boolean isEmpty();

    @Override
    W poll(long timeout) throws InterruptedException;

    void put(W context) throws InterruptedException;

    int size();

    @Override
    String getSourceName();
  }

  class HandledContext<C> implements ContextWrapper<C> {
    private final C context;

    public HandledContext(C context) {
      this.context = context;
    }

    @Override
    public void runWithHandler(EventHandler<C> handler) throws EventHandlerException {
      try {
        handler.handleEvent(this.context);
      } finally {
        Assert.assertTrue(inflight.decrementAndGet() >= 0);
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
  
  
  static class CloseContext<C> implements ContextWrapper<C> {

    public CloseContext() {
    }

    @Override
    public void runWithHandler(EventHandler<C> handler) throws EventHandlerException {
    }
  }
}
