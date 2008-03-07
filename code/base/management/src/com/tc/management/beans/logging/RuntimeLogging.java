/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.logging;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.object.logging.RuntimeLogger;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public class RuntimeLogging extends AbstractTerracottaMBean implements RuntimeLoggingMBean {
  private final RuntimeLogger                  runtimeLogger;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

  static {
    final String[] notifTypes = new String[] { DISTRIBUTED_METHOD_DEBUG_EVENT_TYPE, FIELD_CHANGE_DEBUG_EVENT_TYPE,
      LOCK_DEBUG_EVENT_TYPE, NON_PORTABLE_DUMP_EVENT_TYPE, WAIT_NOTIFY_DEBUG_EVENT_TYPE };
    final String name = Notification.class.getName();
    final String description = "Runtime logging event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final SynchronizedLong               sequenceNumber = new SynchronizedLong(0L);

  public RuntimeLogging(RuntimeLogger runtimeLogger) throws NotCompliantMBeanException {
    super(RuntimeLoggingMBean.class, true);
    this.runtimeLogger = runtimeLogger;
  }

  public void setDistributedMethodDebug(boolean distributedMethodDebug) {
    runtimeLogger.setDistributedMethodDebug(distributedMethodDebug);
    sendNotification(new Notification(DISTRIBUTED_METHOD_DEBUG_EVENT_TYPE, this, sequenceNumber.increment(), System
        .currentTimeMillis(), Boolean.toString(distributedMethodDebug)));
  }

  public boolean getDistributedMethodDebug() {
    return runtimeLogger.getDistributedMethodDebug();
  }

  public void setFieldChangeDebug(boolean fieldChangeDebug) {
    runtimeLogger.setFieldChangeDebug(fieldChangeDebug);
    sendNotification(new Notification(FIELD_CHANGE_DEBUG_EVENT_TYPE, this, sequenceNumber.increment(), System
        .currentTimeMillis(), Boolean.toString(fieldChangeDebug)));
  }

  public boolean getFieldChangeDebug() {
    return runtimeLogger.getFieldChangeDebug();
  }

  public void setLockDebug(boolean lockDebug) {
    runtimeLogger.setLockDebug(lockDebug);
    sendNotification(new Notification(LOCK_DEBUG_EVENT_TYPE, this, sequenceNumber.increment(), System
        .currentTimeMillis(), Boolean.toString(lockDebug)));
  }

  public boolean getLockDebug() {
    return runtimeLogger.getLockDebug();
  }

  public void setNewObjectDebug(boolean newObjectDebug) {
    runtimeLogger.setNewManagedObjectDebug(newObjectDebug);
    sendNotification(new Notification(NEW_OBJECT_DEBUG_EVENT_TYPE, this, sequenceNumber.increment(), System
        .currentTimeMillis(), Boolean.toString(newObjectDebug)));
  }

  public boolean getNewObjectDebug() {
    return runtimeLogger.getNewManagedObjectDebug();
  }

  public void setNonPortableDump(boolean nonPortableDump) {
    runtimeLogger.setNonPortableDump(nonPortableDump);
    sendNotification(new Notification(NON_PORTABLE_DUMP_EVENT_TYPE, this, sequenceNumber.increment(), System
        .currentTimeMillis(), Boolean.toString(nonPortableDump)));
  }

  public boolean getNonPortableDump() {
    return runtimeLogger.getNonPortableDump();
  }

  public void setWaitNotifyDebug(boolean waitNotifyDebug) {
    runtimeLogger.setWaitNotifyDebug(waitNotifyDebug);
    sendNotification(new Notification(WAIT_NOTIFY_DEBUG_EVENT_TYPE, this, sequenceNumber.increment(), System
        .currentTimeMillis(), Boolean.toString(waitNotifyDebug)));
  }

  public boolean getWaitNotifyDebug() {
    return runtimeLogger.getWaitNotifyDebug();
  }

  public void reset() {
    /**/
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

}
