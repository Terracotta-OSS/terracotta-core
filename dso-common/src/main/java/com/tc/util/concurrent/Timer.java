/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util.concurrent;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Eugene Shelestovich
 */
public interface Timer {

  /**
   * Creates and executes a one-shot action that becomes enabled
   * after the given delay.
   *
   * @param command the task to execute
   * @param delay   the time from now to delay execution
   * @param unit    the time unit of the delay parameter
   * @return a ScheduledFuture representing pending completion of
   *         the task and whose <tt>get()</tt> method will return
   *         <tt>null</tt> upon completion
   * @throws RejectedExecutionException if the task cannot be
   *                                    scheduled for execution
   * @throws NullPointerException       if command is null
   */
  ScheduledFuture<?> schedule(Runnable command,
                              long delay, TimeUnit unit);

  /**
   * Creates and executes a periodic action that becomes enabled first
   * after the given initial delay, and subsequently with the given
   * period; that is executions will commence after
   * <tt>initialDelay</tt> then <tt>initialDelay+period</tt>, then
   * <tt>initialDelay + 2 * period</tt>, and so on.
   * If any execution of the task
   * encounters an exception, subsequent executions are suppressed.
   * Otherwise, the task will only terminate via cancellation or
   * termination of the executor.  If any execution of this task
   * takes longer than its period, then subsequent executions
   * may start late, but will not concurrently execute.
   *
   * @param command      the task to execute
   * @param initialDelay the time to delay first execution
   * @param period       the period between successive executions
   * @param unit         the time unit of the initialDelay and period parameters
   * @return a ScheduledFuture representing pending completion of
   *         the task, and whose <tt>get()</tt> method will throw an
   *         exception upon cancellation
   * @throws RejectedExecutionException if the task cannot be
   *                                    scheduled for execution
   * @throws NullPointerException       if command is null
   * @throws IllegalArgumentException   if period less than or equal to zero
   */
  ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                         long initialDelay,
                                         long period,
                                         TimeUnit unit);

  /**
   * Creates and executes a periodic action that becomes enabled first
   * after the given initial delay, and subsequently with the
   * given delay between the termination of one execution and the
   * commencement of the next.  If any execution of the task
   * encounters an exception, subsequent executions are suppressed.
   * Otherwise, the task will only terminate via cancellation or
   * termination of the executor.
   *
   * @param command      the task to execute
   * @param initialDelay the time to delay first execution
   * @param delay        the delay between the termination of one
   *                     execution and the commencement of the next
   * @param unit         the time unit of the initialDelay and delay parameters
   * @return a ScheduledFuture representing pending completion of
   *         the task, and whose <tt>get()</tt> method will throw an
   *         exception upon cancellation
   * @throws RejectedExecutionException if the task cannot be
   *                                    scheduled for execution
   * @throws NullPointerException       if command is null
   * @throws IllegalArgumentException   if delay less than or equal to zero
   */
  ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                            long initialDelay,
                                            long delay,
                                            TimeUnit unit);

  /**
   * Executes command with zero required delay. This has effect
   * equivalent to <tt>schedule(command, 0, anyUnit)</tt>.
   *
   * @param command the task to execute
   * @throws RejectedExecutionException at discretion of
   *                                    <tt>RejectedExecutionHandler</tt>, if task cannot be accepted
   *                                    for execution because the executor has been shut down.
   * @throws NullPointerException       if command is null
   */
  void execute(Runnable command);

  /**
   * Terminates this timer, discarding any currently scheduled tasks.
   * Does not interfere with a currently executing task (if it exists).
   * Once a timer has been terminated, no more tasks may be scheduled on it.
   * <p>This method may be called repeatedly; the second and subsequent
   * calls have no effect.
   */
  void cancel();
}
