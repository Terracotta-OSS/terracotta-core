/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.abortable.AbortedOperationException;

public interface OnCommitCallable {
  public void call() throws AbortedOperationException;
}
