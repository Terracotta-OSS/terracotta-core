/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.logging;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.CircularLossyQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.Notification;

/**
 * @author Ludovic Orban
 */
public class TCLoggingHistoryProvider {
  private final CircularLossyQueue<Notification> notificationsHistory = new CircularLossyQueue<Notification>(
      TCPropertiesImpl
          .getProperties()
          .getInt(
              TCPropertiesConsts.L2_LOGS_STORE,
              1500));

  public void push(Notification notif) {
    notificationsHistory.push(notif);
  }

  public List<Notification> getLogNotifications() {
    Notification[] notifications = new Notification[this.notificationsHistory.depth()];
    this.notificationsHistory.toArray(notifications);
    return Arrays.asList(notifications);
  }

  /**
   * Return all messages that have come since the provided timestamp
   * 
   * @param timestamp indicates time of last retrieved message
   */
  public List<Notification> getLogNotificationsSince(long timestamp) {
    List<Notification> result = new ArrayList<Notification>();

    Notification[] notifications = new Notification[this.notificationsHistory.depth()];
    this.notificationsHistory.toArray(notifications);
    for (Notification notification : notifications) {
      if (notification.getTimeStamp() > timestamp) {
        result.add(notification);
      }
    }

    return result;
  }
}
