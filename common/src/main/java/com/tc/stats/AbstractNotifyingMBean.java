/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import com.tc.management.AbstractTerracottaMBean;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public abstract class AbstractNotifyingMBean extends AbstractTerracottaMBean {

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
  static {
    final String[] types = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
    final String noticeType = AttributeChangeNotification.class.getName();
    final String description = "An attribute of this MBean has changed";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(types, noticeType, description) };

  }

  private long                                 nextSequenceNumber = 1;

  protected AbstractNotifyingMBean(final Class mBeanInterface) throws NotCompliantMBeanException {
    super(mBeanInterface, true);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

  protected synchronized void sendNotification(final String type, final Object source) {
    sendNotification(new Notification(type, source, nextSequenceNumber++));
  }

  protected synchronized void sendNotification(final String msg, final String attr, final String type,
                                               final Object oldVal, final Object newVal) {
    sendNotification(new AttributeChangeNotification(this, nextSequenceNumber++, System.currentTimeMillis(), msg, attr,
                                                     type, oldVal, newVal));
  }
  
  protected synchronized void sendNotification(final String type, final Object source, String message) {
    sendNotification(new Notification(type, source, nextSequenceNumber++, message));
  }
  
  protected synchronized void sendNotification(final String type, final Object source, final Object userData) {
    Notification notification = new Notification(type, source, nextSequenceNumber++);
    notification.setUserData(userData);
    sendNotification(notification);
  }

}
