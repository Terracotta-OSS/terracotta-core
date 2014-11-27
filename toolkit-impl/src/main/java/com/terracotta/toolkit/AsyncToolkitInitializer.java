/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.nonstop.NonStopToolkitInstantiationException;

import com.tc.abortable.AbortableOperationManager;
import com.tc.util.Util;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class AsyncToolkitInitializer implements ToolkitInitializer {

  private final FutureTask<ToolkitInternal> toolkitDelegateFutureTask;
  private final AbortableOperationManager   abortableOperationManager;

  public AsyncToolkitInitializer(FutureTask<ToolkitInternal> toolkitDelegateFutureTask,
                                 AbortableOperationManager abortableOperationManager) {
    this.toolkitDelegateFutureTask = toolkitDelegateFutureTask;
    this.abortableOperationManager = abortableOperationManager;
  }

  /**
   * waits until the toolkit is initialized and returns the toolkit.
   */
  @Override
  public ToolkitInternal getToolkit() {
    return getToolkit(true);
  }
  
  private ToolkitInternal getToolkit(boolean abortable) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return toolkitDelegateFutureTask.get();
        } catch (InterruptedException e) {
          if (abortable) {
            handleInterruptedException(e);
          }
          interrupted = true;
        } catch (ExecutionException e) {
          throw new NonStopToolkitInstantiationException(e.getCause());
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(interrupted);
    }
  }

  /**
   * returns toolkit If initialized or Null
   */
  @Override
  public ToolkitInternal getToolkitOrNull() {
    if (toolkitDelegateFutureTask.isDone()) {
      return getToolkit(false);
    } else {
      return null;
    }

  }

  private void handleInterruptedException(InterruptedException e) {
    if (abortableOperationManager.isAborted()) { throw new ToolkitAbortableOperationException(); }
  }
}
