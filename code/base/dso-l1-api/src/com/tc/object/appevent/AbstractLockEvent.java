/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

/**
 * Abstract class for lock events
 */
public class AbstractLockEvent extends AbstractApplicationEvent {

  private static final long serialVersionUID = 1223477247234324L;

  /**
   * Construct new event with a lock event context
   * @param context Context
   */
  public AbstractLockEvent(AbstractLockEventContext context) {
    super(context);
  }

  /**
   * Get context typed more specifically to this event
   * @return The lock context
   */
  public AbstractLockEventContext getAbstractLockEventContext() {
    return (AbstractLockEventContext) getApplicationEventContext();
  }

  public String getMessage() {
    return "Current transaction with read-only access attempted to modify a shared object.  "
           + "\nPlease alter the locks section of your Terracotta configuration so that the methods involved in this transaction have read/write access.";
  }
}
