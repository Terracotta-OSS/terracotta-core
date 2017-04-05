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
package com.tc.services;


/**
 * The high-level interface extracted from SingleThreadedTimer so it can be more easily mocked in tests.
 */
public interface ISimpleTimer {
  /**
   * Start the timer.  No messages will be processed until this is done.
   */
  public void start();

  /**
   * Stops the timer.  Note that the implementation may not be usable after this call.
   * 
   * @throws InterruptedException Interruption experienced while waiting for background thread.
   */
  public void stop() throws InterruptedException;

  /**
   * Asks the timer implementation for the current time, in millis, from its time source (note that this is generally
   * equivalent to System.currentTimeMillis() but exists for testability).
   * 
   * @return The system's current time, in milliseconds.
   */
  public long currentTimeMillis();

  /**
   * Enqueues a runnable to be run after the receiver's clock reaches startTimeMillis.
   * 
   * @param toRun The runnable to run (in the background thread).
   * @param startTimeMillis The time when the runnable is first allowed to run.
   * @return A unique ID which can be used for cancellation, later.
   */
  public long addDelayed(Runnable toRun, long startTimeMillis);

  /**
   * Enqueues a runnable to be run after the receiver's clock reaches startTimeMillis, and every repeatPeriodMillis
   * thereafter.
   * 
   * @param toRun The runnable to run (in the background thread).
   * @param startTimeMillis The time when the runnable is first allowed to run.
   * @param repeatPeriodMillis The period between invocations of toRun.
   * @return A unique ID which can be used for cancellation, later.
   */
  public long addPeriodic(Runnable toRun, long startTimeMillis, long repeatPeriodMillis);

  /**
   * Cancels a delayed or period invocation previously scheduled with addDelayed or addPeriodic.
   * 
   * @param id The ID of the invocation to cancel.
   * @return True if the invocation was found and cancelled.
   */
  public boolean cancel(long id);
}
