/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.object.util.ReadOnlyException;

/**
 * Event context for a {@link ReadOnlyObjectEvent} 
 */
public class ReadOnlyObjectEventContext extends AbstractLockEventContext {

  private static final long serialVersionUID = 4788562594133534828L;

  public ReadOnlyObjectEventContext(Object pojo, String threadName, String clientId, ReadOnlyException exception) {
    this(pojo, null, null, threadName, clientId, exception);
  }

  public ReadOnlyObjectEventContext(Object pojo, String className, String fieldName, String threadName,
                                    String clientId, ReadOnlyException exception) {
    super(pojo, className, fieldName, threadName, clientId, exception);
  }

  /**
   * @return Get read only exception that occurred
   */
  public ReadOnlyException getReadOnlyException() {
    return (ReadOnlyException) getException();
  }
}
