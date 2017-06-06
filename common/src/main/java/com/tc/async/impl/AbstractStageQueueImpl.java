package com.tc.async.impl;

import org.slf4j.Logger;

import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Source;
import com.tc.async.api.SpecializedEventContext;
import com.tc.async.api.StageQueueStats;
import com.tc.exception.TCRuntimeException;
import com.tc.util.UpdatableFixedHeap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author cschanck
 **/
public class AbstractStageQueueImpl<EC> {


  public AbstractStageQueueImpl() {
  }

  interface SourceQueue<W> extends Source<W> {
    AbstractStageQueueImpl.StageQueueStatsCollector getStatsCollector();

    void setStatsCollector(AbstractStageQueueImpl.StageQueueStatsCollector collector);

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

  static abstract class StageQueueStatsCollector implements StageQueueStats {

    public void logDetails(Logger statsLogger) {
      statsLogger.info(getDetails());
    }

    public abstract void contextAdded();

    public abstract void reset();

    public abstract void contextRemoved();

    protected String makeWidth(String name, int width) {
      final int len = name.length();
      if (len == width) {
        return name;
      }
      if (len > width) {
        return name.substring(0, width);
      }

      StringBuffer buf = new StringBuffer(name);
      for (int i = len; i < width; i++) {
        buf.append(' ');
      }
      return buf.toString();
    }
  }

  static class NullStageQueueStatsCollector extends StageQueueStatsCollector {

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

  static class StageQueueStatsCollectorImpl extends StageQueueStatsCollector {

    private final AtomicInteger count = new AtomicInteger(0);
    private final String name;
    private final String trimmedName;

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

  static class DirectExecuteContext<EC> implements ContextWrapper<EC> {
    private final SpecializedEventContext context;

    public DirectExecuteContext(SpecializedEventContext context) {
      this.context = context;
    }

    @Override
    public void runWithHandler(EventHandler<EC> handler) throws EventHandlerException {
      this.context.execute();
    }
  }

  static class HandledContext<C> implements ContextWrapper<C> {
    private final C context;

    public HandledContext(C context) {
      this.context = context;
    }

    @Override
    public void runWithHandler(EventHandler<C> handler) throws EventHandlerException {
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
