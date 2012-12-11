/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.logging;

import com.tc.management.TerracottaMBean;

import java.util.List;

import javax.management.Notification;

public interface TCLoggingBroadcasterMBean extends TerracottaMBean {

  List<Notification> getLogNotifications();

  List<Notification> getLogNotificationsSince(long timestamp);

}
