/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.abortable;


public interface AbortableOperationManager {
  /**
   * Begin an Abortable Operation
   */
  void begin();

  /**
   * Begin an Abortable Operation. Should be called by the same thread which called begin.
   */
  void finish();

  /**
   * Abort the given thread
   */
  void abort(Thread thread);

  /**
   * Check if the current thread is aborted
   */
  boolean isAborted();
}
