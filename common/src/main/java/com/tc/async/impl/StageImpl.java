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
import com.tc.async.api.Source;
import com.tc.async.api.Stage;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggerProvider;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

/**
 * The SEDA Stage
 */
public class StageImpl<EC> implements Stage<EC> {
  private static final long    pollTime = 3000; // This is the poor man's solution for
                                                // stage
  private final String         name;
  private final EventHandler<EC> handler;
  private final StageQueue<EC> stageQueue;
  private final WorkerThread<EC>[] threads;
  private final ThreadGroup    group;
  private final TCLogger       logger;
  private final int            sleepMs;
  private final boolean        pausable;

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
  public StageImpl(TCLoggerProvider loggerProvider, String name, EventHandler<EC> handler, int queueCount,
                   ThreadGroup group, QueueFactory<ContextWrapper<EC>> queueFactory, int queueSize) {
    this.logger = loggerProvider.getLogger(Stage.class.getName() + ": " + name);
    this.name = name;
    this.handler = handler;
    this.threads = new WorkerThread[queueCount];
    this.stageQueue = StageQueue.FACTORY.factory(queueCount, queueFactory, loggerProvider, name, queueSize);
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

  @Override
  public void destroy() {
    synchronized (this) {
      if (shutdown) {
        return;
      }
      shutdown = true;
    }
    stageQueue.setClosed(true);
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
    stageQueue.setClosed(false);
    handler.initializeContext(context);
    startThreads();
  }

  @Override
  public Sink<EC> getSink() {
    return stageQueue;
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
 
  private synchronized void startThreads() {
    for (int i = 0; i < threads.length; i++) {
      String threadName = "WorkerThread(" + name + ", " + i;
      if (threads.length > 1) {
        threadName = threadName + ", " + this.stageQueue.getSource(i).getSourceName() + ")";
      } else {
        threadName = threadName + ")";
      }
      threads[i] = new WorkerThread<EC>(threadName, this.stageQueue.getSource(i), handler, group, logger, sleepMs, pausable, name);
      threads[i].start();
    }
  }

  private synchronized void stopThreads() {
    for (WorkerThread<EC> thread : threads) {
      thread.interrupt();
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

  private class WorkerThread<EC> extends Thread {
    private final Source<ContextWrapper<EC>>       source;
    private final EventHandler<EC> handler;
    private final TCLogger     tcLogger;
    private final int          sleepMs;
    private final boolean      pausable;
    private final String       stageName;

    public WorkerThread(String name, Source<ContextWrapper<EC>> source, EventHandler<EC> handler, ThreadGroup group, TCLogger logger, int sleepMs, boolean pausable, String stageName) {
      super(group, name);
      tcLogger = logger;
      setDaemon(true);
      this.source = source;
      this.handler = handler;
      this.sleepMs = sleepMs;
      this.pausable = pausable;
      this.stageName = stageName;
    }

    private void handleStageDebugPauses() {
      if (sleepMs > 0) {
        ThreadUtil.reallySleep(sleepMs);
      }
      while (paused || (pausable && "paused".equalsIgnoreCase(System.getProperty(stageName)))) {
        if (!paused) {
          tcLogger.info("Stage paused, sleeping for 1s");
        }
        ThreadUtil.reallySleep(1000);
      }
    }

    @Override
    public void run() {
      while (!shutdown || !source.isEmpty()) {
        ContextWrapper<EC> ctxt = null;
        try {
          ctxt = source.poll(pollTime);
          if (ctxt != null) {
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
            tcLogger.info("Ignoring " + TCNotRunningException.class.getSimpleName() + " while handling context: "
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
  }

  private static boolean isTCNotRunningException(Throwable e) {
    Throwable rootCause = null;
    while (e != null) {
      rootCause = e;
      e = e.getCause();
    }
    return rootCause instanceof TCNotRunningException;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print("Queue depth: " + getSink().size() + " " + this.name);
    return out;
  }

}
