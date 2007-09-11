/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

public class AbstractLockEvent extends AbstractApplicationEvent {

  private static final long serialVersionUID = 1223477247234324L;

  public AbstractLockEvent(AbstractLockEventContext context) {
    super(context);
  }

  public AbstractLockEventContext getAbstractLockEventContext() {
    return (AbstractLockEventContext) getApplicationEventContext();
  }

  public String getMessage() {
    return "Current transaction with read-only access attempted to modify a shared object.  "
           + "\nPlease alter the locks section of your Terracotta configuration so that the methods involved in this transaction have read/write access.";
  }
}
