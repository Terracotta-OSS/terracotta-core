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
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 */
public class MonitoringSink<EC> implements Sink<EC> {
  private final String name;
  private final Sink<StatsWrapper<EC>> delegate;
  private final EventHandler<EC> handler;
  private static final ThreadLocal<PipelineMonitor> CURRENT = new ThreadLocal<>();
  private final LongAdder queueTime = new LongAdder();
  private final LongAdder runTime = new LongAdder();
  private final LongAdder queued = new LongAdder();


  public MonitoringSink(String name, StageQueue<EC> delegate, EventHandler<EC> handler, boolean direct) {
    this.name = name;
    this.handler = wrapHandler(handler);
    Sink base = direct ? delegate : new DirectSink<>(this.handler, delegate::isEmpty, delegate);
    this.delegate = (Sink<StatsWrapper<EC>>)base;
  }
  
  public static PipelineMonitor finish() {
    PipelineMonitor mon = CURRENT.get();
    if (mon != null) {
      mon.close();
      CURRENT.remove();
    }
    return mon;
  }
  
  public static void start() {
    CURRENT.set(new PipelineMonitor());
  }
  
  public EventHandler<EC> getHandler() {
    return handler;
  }
  
  private EventHandler<EC> wrapHandler(EventHandler<EC> handle) {
    return new EventHandler<EC>() {
      @Override
      public void handleEvent(EC context) throws EventHandlerException {
        StatsWrapper<EC> stats = (StatsWrapper<EC>)context;
        PipelineMonitor monitor = stats.monitor();
        if (monitor != null) {
          CURRENT.set(monitor.action(name, PipelineMonitor.Type.RUN, context));
        }
        stats.run();
        handle.handleEvent(stats.getBase());
        stats.end();
        addStats(stats);
        if (monitor != null) {
          CURRENT.remove();
          monitor.action(name, PipelineMonitor.Type.END, context);
        }
      }
      
      private void addStats(StatsWrapper stats) {
        runTime.add(stats.runTime());
        queueTime.add(stats.queueTime());
        queued.increment();
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
  
  private void record(StatsWrapper<EC> context, Consumer<StatsWrapper<EC>> next) {
    PipelineMonitor running = context.monitor();
    if (running != null) {
      running.action(name, PipelineMonitor.Type.ENQUEUE, context);
    }
    next.accept(context);
  }

  @Override
  public void addSingleThreaded(EC context) {
    record(new StatsWrapper<>(context), delegate::addSingleThreaded);
  }

  @Override
  public void addMultiThreaded(EC context) {
    record(new MultiThreadedStatsWrapper<>(context), delegate::addMultiThreaded);
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
  public Map<String, ?> getState() {
    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("queueTime", queueTime);
    stats.put("runTime", runTime);
    stats.put("count", queued);
    long count = queued.sum();
    if (count > 0) {
      stats.put("average queue time", TimeUnit.NANOSECONDS.toNanos(queueTime.sum()/count));
      stats.put("average run time", TimeUnit.NANOSECONDS.toNanos(runTime.sum()/count));
    }
    return stats;
  }
  
  private static class MultiThreadedStatsWrapper<EC> extends StatsWrapper<EC> implements MultiThreadedEventContext {

    public MultiThreadedStatsWrapper(EC base) {
      super(base);
    }

    @Override
    public Object getSchedulingKey() {
      return ((MultiThreadedEventContext)getBase()).getSchedulingKey();
    }

    @Override
    public boolean flush() {
      return ((MultiThreadedEventContext)getBase()).flush();
    }
    
  }

  private static class StatsWrapper<EC> {
    private final EC base;
    private long queue = 0;
    private long run = 0;
    private long end = 0;
    private final PipelineMonitor monitor = CURRENT.get();

    public StatsWrapper(EC base) {
      this.base = base;
      queue();
    }
    
    void run() {
      run = System.nanoTime();
    }
    
    final void queue() {
      queue = System.nanoTime();
    }
    
    void end() {
      end = System.nanoTime();
    }
    
    long queueTime() {
      return run - queue;
    }
    
    long runTime() {
      return end - run;
    }
    
    EC getBase() {
      return base;
    }
    
    PipelineMonitor monitor() {
      return monitor;
    }
    
  }
}
