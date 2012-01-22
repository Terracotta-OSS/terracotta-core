/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.object.tx.UnlockedSharedObjectException;

/**
 * Event context for {@link UnlockedSharedObjectEvent}
 */
public class UnlockedSharedObjectEventContext extends AbstractLockEventContext {

  private static final long serialVersionUID = 4788562594133534828L;

  public UnlockedSharedObjectEventContext(Object pojo, String threadName, String clientId,
                                          UnlockedSharedObjectException exception) {
    this(pojo, null, null, threadName, clientId, exception);
  }

  public UnlockedSharedObjectEventContext(Object pojo, String className, String fieldName, String threadName,
                                          String clientId, UnlockedSharedObjectException exception) {
    super(pojo, className, fieldName, threadName, clientId, exception);
  }

  public UnlockedSharedObjectException getUnlockedSharedObjectException() {
    return (UnlockedSharedObjectException) getException();
  }
}
