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
import org.slf4j.LoggerFactory;

import com.tc.util.Assert;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Throttled executor runs tasks in the context of calling threads. Tasks are run in parallel if they are offered from
 * different threads and there is a max limit on such tasks that can be run simultaneously. Beyond which the offered
 * tasks are scheduled for future run. Once this executor receives a feedback (of some task completion), one scheduled
 * task is executed in the context of the same feedback thread. Though tasks are picked up in order, there is no
 * guarantee that they are started in the same order as exectution can happen in parallel from different threads
 * provided not throttled.
 */

public class ThrottledTaskExecutor {

  private final ConcurrentLinkedQueue<Task> scheduledTasks;
  private final Semaphore                   semaphore;
  private final AtomicLong                  sequenceID;
  private static final Logger logger = LoggerFactory.getLogger(ThrottledTaskExecutor.class);

  public ThrottledTaskExecutor(int maxOutstandingTasks) {
    Assert.eval(maxOutstandingTasks > 0);
    this.semaphore = new Semaphore(maxOutstandingTasks);
    this.scheduledTasks = new ConcurrentLinkedQueue<Task>();
    this.sequenceID = new AtomicLong(0);
  }

  public long offer(Runnable task) {
    Task newTask = new Task(this.sequenceID.incrementAndGet(), task);
    return offer(newTask);
  }

  /**
   * Running/Scheduling logging for a particular task can be out of order as they cab be picked up for execution by
   * different threads.
   */
  private long offer(Task newTask) {
    scheduledTasks.add(newTask);
    if (semaphore.tryAcquire()) {
      if (runTasks()) { return newTask.getId(); }
    }

    if (logger.isDebugEnabled()) debugLog("Scheduling : " + newTask);
    return newTask.scheduled();
  }

  /**
   * should run task only after having got a semaphore license. Either a fresh acquire or on feedback
   */
  private boolean runTasks() {
    Task t;
    if ((t = scheduledTasks.poll()) != null) {
      if (logger.isDebugEnabled()) debugLog("Running : " + t);
      execute(t);
      return true;
    } else {
      semaphore.release();
    }
    return false;
  }

  private long execute(Task task) {
    return task.execute();
  }

  public void receiveFeedback() {
    runTasks();
  }

  synchronized int getScheduledTasksCount() {
    return scheduledTasks.size();
  }

  private void debugLog(String message) {
    if (logger.isDebugEnabled()) {
      logger.debug(message);
    }
  }

  private static class Task {
    private final long     SCHEDULED_TASK_ID = -1;
    private final long     id;
    private final Runnable task;

    public Task(long id, Runnable task) {
      this.id = id;
      this.task = task;
    }

    public long scheduled() {
      return SCHEDULED_TASK_ID;
    }

    public long execute() {
      this.task.run();
      return this.id;
    }

    public long getId() {
      return id;
    }

    @Override
    public String toString() {
      return "Task  - id: " + this.id;
    }
  }

}
