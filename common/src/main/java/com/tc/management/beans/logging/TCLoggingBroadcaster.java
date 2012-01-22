/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.logging;

import com.tc.management.AbstractTerracottaMBean;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public final class TCLoggingBroadcaster extends AbstractTerracottaMBean implements TCLoggingBroadcasterMBean {

  private static final String                  LOGGING_EVENT_TYPE = "tc.logging.event";
  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
  static {
    final String[] notifTypes = new String[] { LOGGING_EVENT_TYPE };
    final String name = Notification.class.getName();
    final String description = "Each notification sent contains a Terracotta logging event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final AtomicLong                     sequenceNumber     = new AtomicLong(0L);

  public void reset() {
    // nothing to reset
  }

  public TCLoggingBroadcaster() throws NotCompliantMBeanException {
    super(TCLoggingBroadcasterMBean.class, true);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

  public void broadcastLogEvent(final String event) {
    final Notification notif = new Notification(LOGGING_EVENT_TYPE, this, sequenceNumber.incrementAndGet(),
                                                System.currentTimeMillis(), event);
    sendNotification(notif);
  }

}
