/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

/**
 * Event is fired when there is an attempt to access a shared object outside the scope of
 * a shared lock.
 */
public class UnlockedSharedObjectEvent extends AbstractLockEvent {

  private static final long serialVersionUID = 1223477247234324L;

  public UnlockedSharedObjectEvent(UnlockedSharedObjectEventContext context) {
    super(context);
  }

  /**
   * @return Get context specific to this event
   */
  public UnlockedSharedObjectEventContext getUnlockedSharedObjectEventContext() {
    return (UnlockedSharedObjectEventContext) getApplicationEventContext();
  }

  public String getMessage() {
    return "Attempt to access a shared object outside the scope of a shared lock.  "
           + "All access to shared objects must be within the scope of one or more shared locks defined in your Terracotta configuration.";
  }
}
