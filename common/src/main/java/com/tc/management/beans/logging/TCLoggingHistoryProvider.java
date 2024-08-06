/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
