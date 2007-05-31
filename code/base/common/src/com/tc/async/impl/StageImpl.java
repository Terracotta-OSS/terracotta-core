/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.async.api.Stage;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggerProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * @author steve
 */
public class StageImpl implements Stage {
  private static final long         pollTime    = 3000; // This is the poor man's solution for stage
  // shutdown
  private static final EventContext PAUSE_TOKEN = new EventContext() {
                                                  //
                                                };

  private final String              name;
  private final EventHandler        handler;
  private final Sink                sink;
  private final WorkerThread[]      threads;
  private final ThreadGroup         group;
  private final TCLogger            logger;
  private boolean                   isPaused    = false;

  public StageImpl(TCLoggerProvider loggerProvider, String name, EventHandler handler, Sink sink, int threadCount,
                   ThreadGroup group) {
    this.logger = loggerProvider.getLogger(Stage.class.getName() + ": " + name);
    this.name = name;
    this.handler = handler;
    this.threads = new WorkerThread[threadCount];
    this.sink = sink;
    this.group = group;
  }

  public void destroy() {
    stopThreads();
  }

  public void start(ConfigurationContext context) {
    handler.initialize(context);
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
      threads[i] = new WorkerThread("WorkerThread(" + name + "," + i + ")", (Source) sink, handler, group);
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
    private static final com.tc.util.State RUNNING           = new com.tc.util.State("RUNNING");
    private static final com.tc.util.State PAUSED            = new com.tc.util.State("PAUSED");

    private com.tc.util.State              state;
    private final Source                   source;
    private final EventHandler             handler;
    private boolean                        shutdownRequested = false;

    public WorkerThread(String name, Source source, EventHandler handler, ThreadGroup group) {
      super(group, name);
      setDaemon(true);
      this.source = source;
      this.handler = handler;
    }

    public synchronized void shutdown() {
      this.shutdownRequested = true;
    }

    private synchronized boolean shutdownRequested() {
      return this.shutdownRequested;
    }

    synchronized void pause() {
      while (state != PAUSED) {
        try {
          wait();
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }

    synchronized void unpause() {
      if (state != PAUSED) throw new AssertionError("Attempt to unpause when not paused: " + state);
      state = RUNNING;
      notifyAll();
    }

    public void run() {
      state = RUNNING;
      while (!shutdownRequested()) {
        EventContext ctxt;
        try {
          ctxt = source.poll(pollTime);
          if (ctxt == PAUSE_TOKEN) {
            synchronized (this) {
              state = PAUSED;
              notifyAll();
              while (state == PAUSED) {
                wait();
              }
            }
          } else if (ctxt != null) {
            handler.logOnEnter(ctxt);
            try {
              handler.handleEvent(ctxt);
            } finally {
              handler.logOnExit(ctxt);
            }
          }
        } catch (InterruptedException ie) {
          if (shutdownRequested()) { return; }
          throw new TCRuntimeException(ie);
        } catch (EventHandlerException ie) {
          if (shutdownRequested()) return;
          throw new TCRuntimeException(ie);
        } finally {
          // Agressively null out the reference before going around the loop again. If you don't do this, the reference
          // to the context will exist until another context comes in. This can potentially keep many objects in memory
          // longer than necessary
          ctxt = null;
        }
      }
    }
  }

  public void pause() {
    if (isPaused) throw new AssertionError("Attempt to pause while already paused.");

    log("Pausing...");

    List pauseTokens = new ArrayList(threads.length);
    for (int i = 0; i < threads.length; i++) {
      pauseTokens.add(PAUSE_TOKEN);
    }
    sink.pause(pauseTokens);
    for (int i = 0; i < threads.length; i++) {
      threads[i].pause();
    }
    isPaused = true;
    log("Paused.");
  }

  public void unpause() {
    if (!isPaused) throw new AssertionError("Attempt to unpause while not paused.");
    log("Unpausing...");

    sink.unpause();
    for (int i = 0; i < threads.length; i++) {
      threads[i].unpause();
    }

    isPaused = false;
    log("Unpaused.");
  }

  private void log(Object msg) {
    logger.info("Stage " + name + ": " + msg);
  }
}