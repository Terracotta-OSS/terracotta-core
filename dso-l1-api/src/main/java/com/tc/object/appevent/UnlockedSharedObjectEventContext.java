/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
