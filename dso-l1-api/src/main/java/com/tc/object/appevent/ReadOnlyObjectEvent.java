/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.appevent;

/**
 * This event is fired when a transaction with read-only access is attempting
 * to modify a shared object.
 */
public class ReadOnlyObjectEvent extends AbstractLockEvent {

  private static final long serialVersionUID = 1223477247234324L;

  public ReadOnlyObjectEvent(ReadOnlyObjectEventContext context) {
    super(context);
  }

  /**
   * @return Get event context specific to this event
   */
  public ReadOnlyObjectEventContext getReadOnlyObjectEventContext() {
    return (ReadOnlyObjectEventContext) getAbstractLockEventContext();
  }

  public String getMessage() {
    return "Current transaction with read-only access attempted to modify a shared object.  "
           + "\nPlease alter the locks section of your Terracotta configuration so that the methods involved in this transaction have read/write access.";
  }
}
