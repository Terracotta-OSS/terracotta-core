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

import org.slf4j.Logger;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.async.api.Stage;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLoggerProvider;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;
import java.util.Arrays;

/**
 * The SEDA Stage
 */
public class StageImpl<EC> implements Stage<EC> {
  private static final long    pollTime = 3000; // This is the poor man's solution for
                                                // stage
  private final String         name;
  private final EventHandler<EC> handler;
  private final StageQueue<EC> stageQueue;
  private final Sink<EC> sink;
  private final WorkerThread[] threads;
  private final ThreadGroup    group;
  private final Logger logger;
  private final int            sleepMs;
  private final boolean        pausable;
  private static final boolean     MONITOR       = TCPropertiesImpl.getProperties()
                                                     .getBoolean(TCPropertiesConsts.TC_STAGE_MONITOR_ENABLED);
  private volatile boolean     paused;
  private volatile boolean     shutdown = true;

  /**
   * The Constructor.
   * 
   * @param loggerProvider : logger
   * @param name : The stage name
   * @param handler : Event handler for this stage
   * @param queueCount : Number of threads and queues working on this stage with 1 thread bound to 1 queue
   * @param group : The thread group to be used
   * @param queueFactory : Factory used to create the queues
   * @param queueSize : Max queue Size allowed
   */
  @SuppressWarnings("unchecked")
  public StageImpl(TCLoggerProvider loggerProvider, String name, Class<EC> type, EventHandler<EC> handler, int queueCount,
                   ThreadGroup group, QueueFactory queueFactory, int queueSize, boolean canBeDirect) {
    this.logger = loggerProvider.getLogger(Stage.class.getName() + ": " + name);
    this.name = name;
    this.threads = new WorkerThread[queueCount];
    this.stageQueue = StageQueue.FACTORY.factory(queueCount, queueFactory, type, loggerProvider, name, queueSize);
    this.sink = createSink(handler, canBeDirect);
    this.handler = (this.sink instanceof MonitoringSink) ? ((MonitoringSink)this.sink).getHandler() : handler;
    this.group = group;
    this.sleepMs = TCPropertiesImpl.getProperties().getInt("seda." + name + ".sleepMs", 0);
    if (this.sleepMs > 0) {
      logger.warn("Sleep of " + this.sleepMs + "ms enabled for stage " + name);
    }
    this.pausable = TCPropertiesImpl.getProperties().getBoolean("seda." + name + ".pausable", false);
    if (this.pausable) {
      logger.warn("Stage pausing is enabled for stage " + name);
    }
  }
  
  private Sink<EC> createSink(EventHandler<EC> handler, boolean direct) {
    return MONITOR ? new MonitoringSink<>(name, stageQueue, handler, direct) : 
        direct ? new DirectSink<>(handler, stageQueue::isEmpty, stageQueue) : stageQueue;
  }

  @Override
  public void destroy() {
    synchronized (this) {
      if (shutdown) {
        return;
      }
      shutdown = true;
    }
    stageQueue.close();
    stopThreads();
    handler.destroy();
  }

  @Override
  public void start(ConfigurationContext context) {
    synchronized (this) {
      if (!shutdown) {
        return;
      }
      shutdown = false;
    }
    handler.initializeContext(context);
    startThreads();
  }

  @Override
  public Sink<EC> getSink() {
    return sink;
  }

  @Override
  public int pause() {
    paused = true;
    return stageQueue.size();
  }

  @Override
  public void unpause() {
    paused = false;
  }

  @Override
  public void clear() {
    boolean interrupted = Thread.interrupted();
    this.stageQueue.clear();
    for (WorkerThread wt : threads) {
      try {
        if (wt != null) {
          wt.waitForIdle();
        }
      } catch (InterruptedException ie) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }
 
  private synchronized void startThreads() {
    for (int i = 0; i < threads.length; i++) {
      String threadName = "WorkerThread(" + name + ", " + i;
      if (threads.length > 1) {
        threadName = threadName + ", " + this.stageQueue.getSource(i).getSourceName() + ")";
      } else {
        threadName = threadName + ")";
      }
      threads[i] = new WorkerThread<>(threadName, this.stageQueue.getSource(i), handler);
      threads[i].start();
    }
  }

  private synchronized void stopThreads() {
    for (WorkerThread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException ie) {
        throw new RuntimeException(ie);
      }
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "StageImpl(" + name + ")";
  }
// for testing
  void waitForIdle() {
    Arrays.stream(threads).forEach(t->t.waitForIdleUninterruptibly());
  }

  private class WorkerThread<EC> extends Thread {
    private final Source<ContextWrapper<EC>>       source;
    private final EventHandler<EC> handler;
    private volatile boolean idle = false;
    private final Object idleLock = new Object();
    private boolean waitingForIdle = false;

    public WorkerThread(String name, Source<ContextWrapper<EC>> source, EventHandler<EC> handler) {
      super(group, name);
      setDaemon(true);
      this.source = source;
      this.handler = handler;
    }

    private void handleStageDebugPauses() {
      if (sleepMs > 0) {
        ThreadUtil.reallySleep(sleepMs);
      }
      while (paused || (pausable && "paused".equalsIgnoreCase(System.getProperty(name)))) {
        if (!paused) {
          logger.info("Stage paused, sleeping for 1s");
        }
        ThreadUtil.reallySleep(1000);
      }
    }
    
    public boolean isIdle() {
      return this.idle;
    }

    @Override
    public void run() {
      while (!shutdown || !source.isEmpty()) {
        ContextWrapper<EC> ctxt = null;
        try {
          this.setToIdle();
          ctxt = source.poll(pollTime);
          if (ctxt != null) {
            this.idle = false;
            handleStageDebugPauses();
            ctxt.runWithHandler(handler);
          }
        } catch (InterruptedException ie) {
          if (shutdown) { continue; }
          throw new TCRuntimeException(ie);
        } catch (EventHandlerException ie) {
          if (shutdown) { continue; }
          throw new TCRuntimeException(ie);
        } catch (Exception e) {
          if (isTCNotRunningException(e)) {
            if (shutdown) { continue; }
            logger.info("Ignoring " + TCNotRunningException.class.getSimpleName() + " while handling context: "
                          + ctxt);
          } else {
            throw new TCRuntimeException("Uncaught exception in stage", e);
          }
        } finally {
          // Aggressively null out the reference before going around the loop again. If you don't do this, the reference
          // to the context will exist until another context comes in. This can potentially keep many objects in memory
          // longer than necessary
          ctxt = null;
        }
      }
    }
    
    private void setToIdle() {
      if (this.idle != true && source.isEmpty()) {
        this.idle = true;
        synchronized (idleLock) {
          if (waitingForIdle) {
            idleLock.notifyAll();
          }
        }
      }
    }
    
    private void waitForIdleUninterruptibly() {
      boolean interrupted = false;
      boolean localIdle = false;
      while (!localIdle) {
        try {
          waitForIdle();
          localIdle = true;
        } catch (InterruptedException ie) {
          interrupted = true;
        }
      }
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    
    private void waitForIdle() throws InterruptedException {
      while (!this.idle) {
        synchronized (idleLock) {
          waitingForIdle = true;
          idleLock.wait();
          waitingForIdle = false;
        }
      }
    }
  }

  private static boolean isTCNotRunningException(Throwable e) {
    Throwable rootCause = null;
    while (e != null) {
      rootCause = e;
      e = e.getCause();
    }
    return rootCause instanceof TCNotRunningException;
  }
}
