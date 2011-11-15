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

  private final AtomicLong                     sequenceNumber = new AtomicLong(0L);

  public RuntimeLogging(RuntimeLogger runtimeLogger) throws NotCompliantMBeanException {
    super(RuntimeLoggingMBean.class, true);
    this.runtimeLogger = runtimeLogger;
  }

  public void setDistributedMethodDebug(boolean distributedMethodDebug) {
    runtimeLogger.setDistributedMethodDebug(distributedMethodDebug);
    sendNotification(DISTRIBUTED_METHOD_DEBUG_EVENT_TYPE, distributedMethodDebug);
  }

  public boolean getDistributedMethodDebug() {
    return runtimeLogger.getDistributedMethodDebug();
  }

  public void setFieldChangeDebug(boolean fieldChangeDebug) {
    runtimeLogger.setFieldChangeDebug(fieldChangeDebug);
    sendNotification(FIELD_CHANGE_DEBUG_EVENT_TYPE, fieldChangeDebug);
  }

  public boolean getFieldChangeDebug() {
    return runtimeLogger.getFieldChangeDebug();
  }

  public void setLockDebug(boolean lockDebug) {
    runtimeLogger.setLockDebug(lockDebug);
    sendNotification(LOCK_DEBUG_EVENT_TYPE, lockDebug);
  }

  public boolean getLockDebug() {
    return runtimeLogger.getLockDebug();
  }

  public void setNewObjectDebug(boolean newObjectDebug) {
    runtimeLogger.setNewManagedObjectDebug(newObjectDebug);
    sendNotification(NEW_OBJECT_DEBUG_EVENT_TYPE, newObjectDebug);
  }

  public boolean getNewObjectDebug() {
    return runtimeLogger.getNewManagedObjectDebug();
  }

  public boolean getNamedLoaderDebug() {
    return runtimeLogger.getNamedLoaderDebug();
  }

  public void setNamedLoaderDebug(boolean namedLoaderDebug) {
    runtimeLogger.setNamedLoaderDebug(namedLoaderDebug);
    sendNotification(NAMED_LOADER_DEBUG_EVENT_TYPE, namedLoaderDebug);
  }

  public void setNonPortableDump(boolean nonPortableDump) {
    runtimeLogger.setNonPortableDump(nonPortableDump);
    sendNotification(NON_PORTABLE_DUMP_EVENT_TYPE, nonPortableDump);
  }

  public boolean getNonPortableDump() {
    return runtimeLogger.getNonPortableDump();
  }

  public void setWaitNotifyDebug(boolean waitNotifyDebug) {
    runtimeLogger.setWaitNotifyDebug(waitNotifyDebug);
    sendNotification(WAIT_NOTIFY_DEBUG_EVENT_TYPE, waitNotifyDebug);
  }

  public boolean getWaitNotifyDebug() {
    return runtimeLogger.getWaitNotifyDebug();
  }

  public void setFlushDebug(boolean flushDebug) {
    runtimeLogger.setFlushDebug(flushDebug);
    sendNotification(FLUSH_DEBUG_EVENT_TYPE, flushDebug);
  }

  public boolean getFlushDebug() {
    return runtimeLogger.getFlushDebug();
  }

  public void setFaultDebug(boolean faultDebug) {
    runtimeLogger.setFaultDebug(faultDebug);
    sendNotification(FAULT_DEBUG_EVENT_TYPE, faultDebug);
  }

  public boolean getFaultDebug() {
    return runtimeLogger.getFaultDebug();
  }

  private void sendNotification(String eventType, boolean eventValue) {
    sendNotification(new Notification(eventType, this, sequenceNumber.incrementAndGet(), System.currentTimeMillis(),
                                      Boolean.toString(eventValue)));
  }

  public void reset() {
    /**/
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }
}
