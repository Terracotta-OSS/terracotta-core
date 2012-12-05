/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;

public class AbortedOperationUtil {

  public static void throwExceptionIfAborted(AbortableOperationManager abortableOperationManager)
      throws AbortedOperationException {
    if (abortableOperationManager.isAborted()) {
      Thread.currentThread().interrupt();
      throw new AbortedOperationException();
    }
  }

}
