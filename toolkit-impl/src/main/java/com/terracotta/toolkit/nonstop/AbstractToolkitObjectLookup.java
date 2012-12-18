/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.object.ToolkitObject;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractToolkitObjectLookup<T extends ToolkitObject> implements ToolkitObjectLookup<T> {
  private volatile T                      object;
  private final Lock                      lock = new ReentrantLock();
  private final AbortableOperationManager abortableOperationManager;

  public AbstractToolkitObjectLookup(AbortableOperationManager abortableOperationManager) {
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public final T getInitializedObject() {
    while (object == null) {
      try {
        lock.lockInterruptibly();
        try {
          if (object == null) {
            object = lookupObject();
          }
        } finally {
          lock.unlock();
        }
      } catch (InterruptedException e) {
        handleInterruptedException(e);
      }
    }

    return object;
  }

  @Override
  public final T getInitializedObjectOrNull() {
    return object;
  }

  private void handleInterruptedException(InterruptedException e) {
    if (abortableOperationManager.isAborted()) { throw new ToolkitAbortableOperationException(); }
  }

  protected abstract T lookupObject();

}
