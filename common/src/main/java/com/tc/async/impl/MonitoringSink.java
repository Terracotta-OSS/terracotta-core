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

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.stats.Stats;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This StageQueueImpl represents the sink and gives a handle to the source. We are internally just using a queue
 * since our queues are locally processed. This class can be replaced with a distributed queue to enable processing
 * across process boundaries.
 */
public class MonitoringSink<EC> implements Sink<EC> {
  private final String name;
  private final Sink<EC> delegate;
  private final EventHandler<EC> handler;
  private final Map<Object, PipelineMonitor> record = Collections.synchronizedMap(new IdentityHashMap<>());
  private static final ThreadLocal<PipelineMonitor> current = new ThreadLocal<>();


  public MonitoringSink(String name, StageQueue<EC> delegate, EventHandler<EC> handler, boolean direct) {
    this.name = name;
    this.handler = wrapHandler(handler);
    this.delegate = direct ? delegate : new DirectSink<>(this.handler, delegate::isEmpty, delegate);
  }
  
  public static PipelineMonitor finish() {
    return current.get().close();
  }
  
  public static void reset() {
    current.remove();
  }
  
  public EventHandler<EC> getHandler() {
    return handler;
  }
  
  private EventHandler<EC> wrapHandler(EventHandler<EC> handle) {
    return new EventHandler<EC>() {
      @Override
      public void handleEvent(EC context) throws EventHandlerException {
        PipelineMonitor monitor = record.remove(context);
        current.set(monitor.action(name, PipelineMonitor.Type.RUN, context));
        handle.handleEvent(context);
        current.remove();
        monitor.action(name, PipelineMonitor.Type.END, context);
      }

      @Override
      public void handleEvents(Collection<EC> context) throws EventHandlerException {
        for (EC e : context) {
          this.handleEvent(e);
        }
      }

      @Override
      public void destroy() {
        handle.destroy();
      }

      @Override
      public void initializeContext(ConfigurationContext context) {
        handle.initializeContext(context);
      }
    };
  }
  
  private void record(EC context, Consumer<EC> next) {
    PipelineMonitor running = current.get();
    if (running == null) {
      running = new PipelineMonitor();
    }
    running = running.action(name, PipelineMonitor.Type.ENQUEUE, context);
    if (record.put(context, running) != null) {
      throw new AssertionError();
    }
    next.accept(context);
  }

  @Override
  public void addSingleThreaded(EC context) {
    record(context, delegate::addSingleThreaded);
  }

  @Override
  public void addMultiThreaded(EC context) {
    record(context, delegate::addMultiThreaded);
  }

  @Override
  public boolean isEmpty() {
    return this.delegate.isEmpty();
  }

  @Override
  public int size() {
    return this.delegate.size();
  }

  @Override
  public void clear() {
    this.delegate.clear();
  }

  @Override
  public void close() {
    this.delegate.close();
  }

  @Override
  public void enableStatsCollection(boolean enable) {
    this.delegate.enableStatsCollection(enable);
  }

  @Override
  public boolean isStatsCollectionEnabled() {
    return this.delegate.isStatsCollectionEnabled();
  }

  @Override
  public Stats getStats(long frequency) {
    return this.delegate.getStats(frequency);
  }

  @Override
  public Stats getStatsAndReset(long frequency) {
    return this.delegate.getStatsAndReset(frequency);
  }

  @Override
  public void resetStats() {
    this.delegate.resetStats();
  }


  
  
}
