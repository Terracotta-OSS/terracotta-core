/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.abortable;

public class NullAbortableOperationManager implements AbortableOperationManager {
  @Override
  public void begin() {
    //
  }

  @Override
  public void finish() {
    //
  }

  @Override
  public void abort(Thread thread) {
    //
  }

  @Override
  public boolean isAborted() {
    return false;
  }
}
