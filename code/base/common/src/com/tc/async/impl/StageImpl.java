/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.async.api.SpecializedEventContext;
import com.tc.async.api.Stage;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggerProvider;
import com.tc.util.concurrent.QueueFactory;

/**
 * @author steve
 */
public class StageImpl implements Stage {
  private static final long    pollTime = 3000; // This is the poor man's solution for stage
  private final String         name;
  private final EventHandler   handler;
  private final StageQueueImpl sink;
  private final WorkerThread[] threads;
  private final ThreadGroup    group;
  private final TCLogger       logger;

  public StageImpl(TCLoggerProvider loggerProvider, String name, EventHandler handler, int threadCount, int queueRatio,
                   ThreadGroup group, QueueFactory queueFactory, int queueSize) {
    this.logger = loggerProvider.getLogger(Stage.class.getName() + ": " + name);
    this.name = name;
    this.handler = handler;
    this.threads = new WorkerThread[threadCount];
    if (queueRatio > threadCount) {
      logger.warn("Worker Queue Ratio " + queueRatio + " > Worker Threads " + threadCount);
    }
    this.sink = new StageQueueImpl(threadCount, queueRatio, queueFactory, loggerProvider, name, queueSize);
    this.group = group;
  }

  public void destroy() {
    stopThreads();
  }

  public void start(ConfigurationContext context) {
    handler.initializeContext(context);
    startThreads();
  }

  public Sink getSink() {
    return sink;
  }

  public String getName() {
    return name;
  }

  private synchronized void startThreads() {
    for (int i = 0; i < threads.length; i++) {
      String threadName = "WorkerThread(" + name + ", " + i;
      if (threads.length > 1) {
        threadName = threadName + ", " + this.sink.getSource(i).getSourceName() + ")";
      } else {
        threadName = threadName + ")";
      }
      threads[i] = new WorkerThread(threadName, this.sink.getSource(i), handler, group);
      threads[i].start();
    }
  }

  private void stopThreads() {
    for (int i = 0; i < threads.length; i++) {
      threads[i].shutdown();
    }
  }

  public String toString() {
    return "StageImpl(" + name + ")";
  }

  private static class WorkerThread extends Thread {
    private final Source       source;
    private final EventHandler handler;
    private volatile boolean   shutdownRequested = false;

    public WorkerThread(String name, Source source, EventHandler handler, ThreadGroup group) {
      super(group, name);
      setDaemon(true);
      this.source = source;
      this.handler = handler;
    }

    public void shutdown() {
      this.shutdownRequested = true;
    }

    private boolean shutdownRequested() {
      return this.shutdownRequested;
    }

    public void run() {
      while (!shutdownRequested()) {
        EventContext ctxt;
        try {
          ctxt = source.poll(pollTime);
          if (ctxt != null) {
            if (ctxt instanceof SpecializedEventContext) {
              ((SpecializedEventContext) ctxt).execute();
            } else {
              handler.handleEvent(ctxt);
            }
          }
        } catch (InterruptedException ie) {
          if (shutdownRequested()) { return; }
          throw new TCRuntimeException(ie);
        } catch (EventHandlerException ie) {
          if (shutdownRequested()) return;
          throw new TCRuntimeException(ie);
        } finally {
          // Aggressively null out the reference before going around the loop again. If you don't do this, the reference
          // to the context will exist until another context comes in. This can potentially keep many objects in memory
          // longer than necessary
          ctxt = null;
        }
      }
    }
  }

}