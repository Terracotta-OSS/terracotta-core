/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.nonstop.NonStopToolkitInstantiationException;
import org.terracotta.toolkit.object.ToolkitObject;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

public abstract class AbstractToolkitObjectLookupAsync<T extends ToolkitObject> implements ToolkitObjectLookup<T> {
  private volatile T                      initializedObject;
  private volatile Exception              exceptionDuringInitialization;
  private final AbortableOperationManager abortableOperationManager;

  public AbstractToolkitObjectLookupAsync(AbortableOperationManager abortableOperationManager) {
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public final T getInitializedObject() {
    boolean interrupted = false;
    while (initializationPending()) {
      try {
        synchronized (this) {
          if (initializationPending()) {
            wait();
          }
        }
      } catch (InterruptedException e) {
        handleInterruptedException(e);
        interrupted = true;
      }
    }

    // If the thread was interrupted(but not by NonStopManager) set the interrupted status
    if (interrupted) {
      Thread.currentThread().interrupt();
    }

    if (exceptionDuringInitialization != null) {
      throw new NonStopToolkitInstantiationException(exceptionDuringInitialization);
    }

    return initializedObject;
  }

  @Override
  public final T getInitializedObjectOrNull() {
    return initializedObject;
  }

  public boolean initialize() throws Exception {
    try {
      this.initializedObject = lookupObject();
    } catch (Exception e) {
      this.exceptionDuringInitialization = e;
      throw e;
    } finally {
      synchronized (this) {
        notifyAll();
      }
    }

    return true;
  }

  private void handleInterruptedException(InterruptedException e) {
    if (abortableOperationManager.isAborted()) {
      throw new ToolkitAbortableOperationException();
    }
  }

  private boolean initializationPending() {
    return initializedObject == null && exceptionDuringInitialization == null;
  }

  protected abstract T lookupObject();

}
