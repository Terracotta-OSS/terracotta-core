/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util.concurrent;

/**
 * @see Timer
 * @see Runners
 */
public interface TaskRunner {

  /**
   * Constructs an unnamed timer to schedule tasks. Unlike {@link java.util.Timer},
   * all timers share a common thread pool, maintained by this {@link TaskRunner}.
   * <p/>Attempting to call this method after shutting down the task runner throws {@link IllegalStateException}.
   *
   * @return a new unnamed {@link Timer} instance
   */
  Timer newTimer();

  /**
   * Constructs a named timer to schedule tasks. Unlike {@link java.util.Timer},
   * all timers share a common thread pool, maintained by this {@link TaskRunner}.
   * <p/>Attempting to call this method after shutting down the task runner throws {@link IllegalStateException}.
   *
   * @return a new named {@link Timer} instance
   */
  Timer newTimer(String name);

  /**
   * Shuts down the timer and all its scheduled tasks. Currently running tasks are not affected.
   *
   * @param timer the timer to cancel
   */
  void cancelTimer(final Timer timer);

  /**
   * Attempts to stop all actively executing tasks and halts the
   * processing of waiting tasks.
   * <p/>
   * <p>There are no guarantees beyond best-effort attempts to stop
   * processing actively executing tasks.  For example, typical
   * implementations will cancel via {@link Thread#interrupt}, so any
   * task that fails to respond to interrupts may never terminate.
   */
  void shutdown();

}
