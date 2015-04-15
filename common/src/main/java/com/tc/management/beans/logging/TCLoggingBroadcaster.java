/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.management.beans.logging;

import com.tc.management.AbstractTerracottaMBean;

import java.util.List;
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

  private final AtomicLong                     sequenceNumber           = new AtomicLong(0L);
  private final TCLoggingHistoryProvider       tcLoggingHistoryProvider = new TCLoggingHistoryProvider();

  @Override
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

  public void broadcastLogEvent(final String event, final String[] throwableStringRep) {
    Notification notif = new Notification(LOGGING_EVENT_TYPE, this, sequenceNumber.incrementAndGet(),
                                                System.currentTimeMillis(), event);
    notif.setUserData(throwableStringRep);
    sendNotification(notif);

    notif = new Notification(notif.getType(), getClass().getName(), notif.getSequenceNumber(), notif.getTimeStamp(),
                             notif.getMessage());
    notif.setUserData(throwableStringRep);
    tcLoggingHistoryProvider.push(notif);
  }

  @Override
  public List<Notification> getLogNotifications() {
    return tcLoggingHistoryProvider.getLogNotifications();
  }

  @Override
  public List<Notification> getLogNotificationsSince(long timestamp) {
    return tcLoggingHistoryProvider.getLogNotificationsSince(timestamp);
  }
}
