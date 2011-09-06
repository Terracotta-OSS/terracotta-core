/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.logging;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.object.logging.InstrumentationLogger;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public class InstrumentationLogging extends AbstractTerracottaMBean implements InstrumentationLoggingMBean {
  private final InstrumentationLogger          instrumentationLogger;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

  static {
    final String[] notifTypes = new String[] { CLASS_EVENT_TYPE, DISTRIBUTED_METHODS_EVENT_TYPE, LOCKS_EVENT_TYPE,
      ROOTS_EVENT_TYPE, TRANSIENT_ROOT_EVENT_TYPE };
    final String name = Notification.class.getName();
    final String description = "Instrumentation logging event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final SynchronizedLong               sequenceNumber = new SynchronizedLong(0L);

  public InstrumentationLogging(InstrumentationLogger instrumentationLogger) throws NotCompliantMBeanException {
    super(InstrumentationLoggingMBean.class, true);
    this.instrumentationLogger = instrumentationLogger;
  }

  public void setClass1(boolean classInclusion) {
    instrumentationLogger.setClassInclusion(classInclusion);
    sendNotification(new Notification(CLASS_EVENT_TYPE, this, sequenceNumber.increment(), System.currentTimeMillis(),
                                      Boolean.toString(classInclusion)));
  }

  public boolean getClass1() {
    return instrumentationLogger.getClassInclusion();
  }

  public void setDistributedMethods(boolean distMethodClassInsertion) {
    instrumentationLogger.setDistMethodCallInsertion(distMethodClassInsertion);
    sendNotification(new Notification(DISTRIBUTED_METHODS_EVENT_TYPE, this, sequenceNumber.increment(), System
        .currentTimeMillis(), Boolean.toString(distMethodClassInsertion)));
  }

  public boolean getDistributedMethods() {
    return instrumentationLogger.getDistMethodCallInsertion();
  }

  /**
   * TODO: this isn't used anywhere.
   */
  public void setHierarchy(boolean hierarchy) {
    /**/
  }

  /**
   * TODO: this isn't used anywhere.
   */
  public boolean getHierarchy() {
    return false;
  }

  public void setLocks(boolean lockInsertion) {
    instrumentationLogger.setLockInsertion(lockInsertion);
    sendNotification(new Notification(LOCKS_EVENT_TYPE, this, sequenceNumber.increment(), System.currentTimeMillis(),
                                      Boolean.toString(lockInsertion)));
  }

  public boolean getLocks() {
    return instrumentationLogger.getLockInsertion();
  }

  public void setRoots(boolean rootInsertion) {
    instrumentationLogger.setRootInsertion(rootInsertion);
    sendNotification(new Notification(ROOTS_EVENT_TYPE, this, sequenceNumber.increment(), System.currentTimeMillis(),
                                      Boolean.toString(rootInsertion)));
  }

  public boolean getRoots() {
    return instrumentationLogger.getRootInsertion();
  }

  public void setTransientRoot(boolean transientRootWarning) {
    instrumentationLogger.setTransientRootWarning(transientRootWarning);
    sendNotification(new Notification(TRANSIENT_ROOT_EVENT_TYPE, this, sequenceNumber.increment(), System
        .currentTimeMillis(), Boolean.toString(transientRootWarning)));
  }

  public boolean getTransientRoot() {
    return instrumentationLogger.getTransientRootWarning();
  }

  public void reset() {
    /**/
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

}
