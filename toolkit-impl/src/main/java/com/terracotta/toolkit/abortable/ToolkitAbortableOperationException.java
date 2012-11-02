package com.terracotta.toolkit.abortable;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

public class ToolkitAbortableOperationException extends RuntimeException {
  public ToolkitAbortableOperationException() {
    //
  }

  public ToolkitAbortableOperationException(Exception e) {
    super(e);
  }
}
