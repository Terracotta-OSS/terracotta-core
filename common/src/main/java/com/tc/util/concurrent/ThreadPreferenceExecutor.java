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
package com.tc.util.concurrent;

import org.slf4j.Logger;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This executor is like the standard j.u.c.ThreadPoolExecutor (although nowhere near as fancy). The main difference is
 * that new threads are always preferred (as opposed to queue'ing commands) and the core size is 0.
 */
public class ThreadPreferenceExecutor implements Executor {

  private final int                     maxThreads;
  private final long                    idleTime;
  private final TimeUnit                unit;
  private final ThreadFactory           threadFactory;
  private final BlockingQueue<Runnable> queue;
  private final String                  name;
  private final Logger logger;

  private int                           numberOfActiveThreads = 0;
  private int                           newThreadCreateCount  = 0;

  public ThreadPreferenceExecutor(String name, int maxThreads, long idleTime, TimeUnit unit, Logger logger) {
    this(name, maxThreads, idleTime, unit, defaultThreadFactory(name), logger);
  }

  private static ThreadFactory defaultThreadFactory(String name) {
    return new DefaultThreadFactory(name);
  }

  public ThreadPreferenceExecutor(String name, int maxThreads, long idleTime, TimeUnit unit,
                                  ThreadFactory threadFactory, Logger logger) {
    this.name = name;
    this.maxThreads = maxThreads;
    this.idleTime = idleTime;
    this.unit = unit;
    this.threadFactory = threadFactory;
    this.queue = new SynchronousQueue<Runnable>();
    this.logger = logger;
  }

  @Override
  public void execute(Runnable command) {
    if (queue.offer(command)) { return; }

    final boolean accepted = createNewThreadIfPossible(command);
    if (!accepted) { throw new RejectedExecutionException("Max thread limit (" + maxThreads + ") reached for ["
                                                          + getName() + "]"); }
  }

  private String getName() {
    return name;
  }

  private synchronized boolean createNewThreadIfPossible(Runnable command) {
    if (numberOfActiveThreads == maxThreads) { return false; }
    numberOfActiveThreads++;
    newThreadCreateCount++;
    Thread newThread = threadFactory.newThread(new WorkerTask(command));
    newThread.start();
    if (newThreadCreateCount % 5 == 0) {
      newThreadCreateCount = 0;
      if (numberOfActiveThreads > (maxThreads - 10)) {
        logger.info(getName() + " thread count : " + numberOfActiveThreads);
      }
    }
    return true;
  }

  private synchronized void workerDone() {
    numberOfActiveThreads--;
  }

  public synchronized int getActiveThreadCount() {
    return numberOfActiveThreads;
  }

  private class WorkerTask implements Runnable {

    private Runnable firstCommand;

    WorkerTask(Runnable firstCommand) {
      this.firstCommand = firstCommand;
    }

    @Override
    public void run() {
      try {
        while (true) {
          Runnable task = getTask();
          if (task == null) { return; }
          task.run();
        }
      } finally {
        workerDone();
      }
    }

    private Runnable getTask() {
      final Runnable task;

      if (firstCommand != null) {
        task = firstCommand;
        firstCommand = null;
      } else {
        try {
          task = queue.poll(idleTime, unit);
        } catch (InterruptedException e) {
          return null;
        }
      }

      return task;
    }
  }

  private static class DefaultThreadFactory implements ThreadFactory {

    private final AtomicInteger sequence = new AtomicInteger();
    private final String        executorName;

    DefaultThreadFactory(String executorName) {
      this.executorName = executorName;
    }

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, executorName + "-" + sequence.getAndIncrement());
      t.setDaemon(true);
      t.setPriority(Thread.NORM_PRIORITY + 1);
      return t;
    }
  }

}
