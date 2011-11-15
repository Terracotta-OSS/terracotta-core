/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.logging;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.object.logging.RuntimeLogger;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public class RuntimeOutputOptions extends AbstractTerracottaMBean implements RuntimeOutputOptionsMBean {
  private final RuntimeLogger                  runtimeLogger;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

  static {
    final String[] notifTypes = new String[] { AUTOLOCK_DETAILS_EVENT_TYPE, CALLER_EVENT_TYPE, FULL_STACK_EVENT_TYPE };
    final String name = Notification.class.getName();
    final String description = "Runtime Output event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final AtomicLong                     sequenceNumber = new AtomicLong(0L);

  public RuntimeOutputOptions(RuntimeLogger runtimeLogger) throws NotCompliantMBeanException {
    super(RuntimeOutputOptionsMBean.class, true);
    this.runtimeLogger = runtimeLogger;
  }

  public void setAutoLockDetails(boolean autolockDetails) {
    runtimeLogger.setAutoLockDetails(autolockDetails);
    sendNotification(new Notification(AUTOLOCK_DETAILS_EVENT_TYPE, this, sequenceNumber.incrementAndGet(),
                                      System.currentTimeMillis(), Boolean.toString(autolockDetails)));
  }

  public boolean getAutoLockDetails() {
    return runtimeLogger.getAutoLockDetails();
  }

  public void setCaller(boolean caller) {
    runtimeLogger.setCaller(caller);
    sendNotification(new Notification(CALLER_EVENT_TYPE, this, sequenceNumber.incrementAndGet(),
                                      System.currentTimeMillis(), Boolean.toString(caller)));
  }

  public boolean getCaller() {
    return runtimeLogger.getCaller();
  }

  public void setFullStack(boolean fullStack) {
    runtimeLogger.setFullStack(fullStack);
    sendNotification(new Notification(FULL_STACK_EVENT_TYPE, this, sequenceNumber.incrementAndGet(),
                                      System.currentTimeMillis(), Boolean.toString(fullStack)));
  }

  public boolean getFullStack() {
    return runtimeLogger.getFullStack();
  }

  public void reset() {
    /**/
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }
}
