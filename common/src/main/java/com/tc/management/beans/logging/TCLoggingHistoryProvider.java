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
