/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.logging;

import com.tc.management.TerracottaMBean;

import javax.management.NotificationEmitter;

public interface RuntimeOutputOptionsMBean extends TerracottaMBean, NotificationEmitter {
  public static final String AUTOLOCK_DETAILS_EVENT_TYPE = "tc.logging.runtime-output.AutoLockDetails";
  public static final String CALLER_EVENT_TYPE           = "tc.logging.runtime-output.Caller";
  public static final String FULL_STACK_EVENT_TYPE       = "tc.logging.runtime-output.FullStack";

  void setAutoLockDetails(boolean autolockDetails);

  boolean getAutoLockDetails();

  void setCaller(boolean caller);

  boolean getCaller();

  void setFullStack(boolean fullStack);

  boolean getFullStack();
}
