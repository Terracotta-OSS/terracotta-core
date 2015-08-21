/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.operatorevent;

import com.tc.management.beans.TerracottaOperatorEventsMBean;

import javax.management.Notification;
import javax.management.NotificationFilter;

class OperatorEventsFilter implements NotificationFilter {
  @Override
  public boolean isNotificationEnabled(Notification notification) {
    return notification.getType().equals(TerracottaOperatorEventsMBean.TERRACOTTA_OPERATOR_EVENT);
  }
}
